# Authentication with WorkOS

## Summary

Add multi-user authentication using WorkOS Magic Auth (passwordless 6-digit codes) and store per-user Readwise API tokens in WorkOS Vault.

## Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│   Backend    │────▶│   WorkOS     │
│   Angular    │     │ Spring Boot  │     │  AuthKit     │
└──────────────┘     └──────────────┘     └──────────────┘
                            │                    │
                            │              ┌─────▼─────┐
                            │              │  Vault    │
                            │              │ (tokens)  │
                            └──────────────┴───────────┘
```

## User Flow

### First-Time User
1. Enter email on login page
2. Receive 6-digit code via email (Magic Auth)
3. Enter code → authenticated
4. Onboarding: Provide Readwise API token
5. Token encrypted and stored in WorkOS Vault
6. Redirected to dashboard

### Returning User
1. Enter email
2. Enter 6-digit code
3. Dashboard loads with their data

---

## Why Magic Auth (Not Magic Link)

WorkOS deprecated Magic Link because email security scanners visit links and invalidate them. Magic Auth uses a 6-digit code that's immune to this problem.

| Method | How it works | Issue |
|--------|--------------|-------|
| Magic Link | Click link in email | Scanners click links, invalidating them |
| Magic Auth | Enter 6-digit code | Codes can't be "used" by scanners |

---

## Implementation

### Phase 1: WorkOS SDK Setup

**Dependencies (build.gradle.kts):**
```kotlin
implementation("com.workos:workos:4.18.1")
```

**Configuration (application.yml):**
```yaml
workos:
  api-key: ${WORKOS_API_KEY}
  client-id: ${WORKOS_CLIENT_ID}
```

**WorkOS Client Bean:**
```kotlin
@Configuration
class WorkOSConfig {
    @Bean
    fun workOS(@Value("\${workos.api-key}") apiKey: String): WorkOS {
        return WorkOS(apiKey)
    }
}
```

### Phase 2: Magic Auth Endpoints

**AuthController.kt:**
```kotlin
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    // Step 1: Request code
    @PostMapping("/magic-auth/start")
    fun startMagicAuth(@RequestBody request: MagicAuthRequest): MagicAuthResponse {
        return authService.createMagicAuthCode(request.email)
    }

    // Step 2: Verify code
    @PostMapping("/magic-auth/verify")
    fun verifyMagicAuth(@RequestBody request: VerifyMagicAuthRequest): AuthResponse {
        return authService.authenticateWithMagicAuth(
            email = request.email,
            code = request.code
        )
    }

    // Get current user
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticatedUser user: User): UserResponse {
        return UserResponse(
            id = user.id,
            email = user.email,
            hasReadwiseToken = user.hasReadwiseToken
        )
    }

    // Logout
    @PostMapping("/logout")
    fun logout(response: HttpServletResponse) {
        // Clear session cookie
    }
}

data class MagicAuthRequest(val email: String)
data class MagicAuthResponse(val magicAuthId: String)
data class VerifyMagicAuthRequest(val email: String, val code: String)
data class AuthResponse(
    val user: UserResponse,
    val accessToken: String,
    val needsOnboarding: Boolean
)
```

**AuthService.kt:**
```kotlin
@Service
class AuthService(
    private val workOS: WorkOS,
    @Value("\${workos.client-id}") private val clientId: String,
    private val userRepository: UserRepository,
    private val vaultService: VaultService
) {
    fun createMagicAuthCode(email: String): MagicAuthResponse {
        val magicAuth = workOS.userManagement.createMagicAuth(
            CreateMagicAuthOptions(email = email)
        )
        // WorkOS sends the email automatically
        return MagicAuthResponse(magicAuthId = magicAuth.id)
    }

    fun authenticateWithMagicAuth(email: String, code: String): AuthResponse {
        val authResult = workOS.userManagement.authenticateWithMagicAuth(
            AuthenticateWithMagicAuthOptions(
                clientId = clientId,
                code = code,
                email = email
            )
        )

        // Create or update local user
        val user = userRepository.findByWorkosId(authResult.user.id)
            ?: userRepository.save(User(
                workosId = authResult.user.id,
                email = authResult.user.email
            ))

        val hasToken = vaultService.hasReadwiseToken(user.workosId)

        return AuthResponse(
            user = user.toResponse(),
            accessToken = authResult.accessToken,
            needsOnboarding = !hasToken
        )
    }
}
```

### Phase 3: Vault Integration (Readwise Tokens)

**VaultService.kt:**
```kotlin
@Service
class VaultService(
    private val workOS: WorkOS
) {
    companion object {
        private const val READWISE_TOKEN_NAME = "readwise_api_token"
    }

    fun storeReadwiseToken(userId: String, token: String) {
        workOS.vault.createObject(
            CreateObjectOptions(
                name = READWISE_TOKEN_NAME,
                value = token,
                keyContext = KeyContext(userId = userId)
            )
        )
    }

    fun getReadwiseToken(userId: String): String? {
        return try {
            val obj = workOS.vault.getObject(
                GetObjectOptions(
                    name = READWISE_TOKEN_NAME,
                    keyContext = KeyContext(userId = userId)
                )
            )
            obj.value
        } catch (e: NotFoundException) {
            null
        }
    }

    fun hasReadwiseToken(userId: String): Boolean {
        return getReadwiseToken(userId) != null
    }

    fun deleteReadwiseToken(userId: String) {
        workOS.vault.deleteObject(
            DeleteObjectOptions(
                name = READWISE_TOKEN_NAME,
                keyContext = KeyContext(userId = userId)
            )
        )
    }
}
```

**OnboardingController.kt:**
```kotlin
@RestController
@RequestMapping("/api/onboarding")
class OnboardingController(
    private val vaultService: VaultService,
    private val readwiseClient: ReadwiseClient
) {
    @PostMapping("/readwise-token")
    fun setReadwiseToken(
        @AuthenticatedUser user: User,
        @RequestBody request: SetTokenRequest
    ): TokenValidationResponse {
        // Validate token works
        val isValid = readwiseClient.validateToken(request.token)
        if (!isValid) {
            throw InvalidTokenException("Readwise token is invalid")
        }

        // Store encrypted in Vault
        vaultService.storeReadwiseToken(user.workosId, request.token)

        return TokenValidationResponse(valid = true)
    }
}

data class SetTokenRequest(val token: String)
data class TokenValidationResponse(val valid: Boolean)
```

### Phase 4: Update Sync to Use Per-User Tokens

**SyncService.kt (modified):**
```kotlin
@Service
class SyncService(
    private val vaultService: VaultService,
    private val readwiseClientFactory: ReadwiseClientFactory,
    // ... other deps
) {
    fun syncForUser(userId: String) {
        val token = vaultService.getReadwiseToken(userId)
            ?: throw NoReadwiseTokenException("User has not configured Readwise token")

        val client = readwiseClientFactory.createClient(token)
        // ... sync logic using user's token
    }
}
```

### Phase 5: Session Management

**JWT-based sessions:**
```kotlin
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {
    fun createToken(userId: String): String {
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .compact()
    }

    fun validateAndGetUserId(token: String): String? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.toByteArray()))
                .build()
                .parseClaimsJws(token)
                .body
                .subject
        } catch (e: Exception) {
            null
        }
    }
}
```

**Security Filter:**
```kotlin
@Component
class JwtAuthenticationFilter(
    private val tokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val token = extractToken(request)
        if (token != null) {
            val userId = tokenProvider.validateAndGetUserId(token)
            if (userId != null) {
                val user = userRepository.findByWorkosId(userId)
                if (user != null) {
                    val auth = UsernamePasswordAuthenticationToken(user, null, emptyList())
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        }
        chain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        return if (header?.startsWith("Bearer ") == true) {
            header.substring(7)
        } else null
    }
}
```

---

## Database Changes

**User Entity:**
```kotlin
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val workosId: String,

    @Column(nullable = false)
    val email: String,

    val createdAt: Instant = Instant.now()
)
```

**Migration:**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workos_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_users_workos_id ON users(workos_id);
```

---

## Frontend Changes

### Login Component
```typescript
@Component({
  selector: 'app-login',
  template: `
    @if (step() === 'email') {
      <form (submit)="requestCode()">
        <input type="email" [(ngModel)]="email" placeholder="Email" />
        <button type="submit">Send Code</button>
      </form>
    }

    @if (step() === 'code') {
      <form (submit)="verifyCode()">
        <p>Enter the 6-digit code sent to {{ email }}</p>
        <input type="text" [(ngModel)]="code" maxlength="6" />
        <button type="submit">Verify</button>
      </form>
    }
  `
})
export class LoginComponent {
  step = signal<'email' | 'code'>('email');
  email = '';
  code = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  async requestCode() {
    await this.authService.startMagicAuth(this.email);
    this.step.set('code');
  }

  async verifyCode() {
    const result = await this.authService.verifyMagicAuth(this.email, this.code);
    if (result.needsOnboarding) {
      this.router.navigate(['/onboarding']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
```

### Onboarding Component
```typescript
@Component({
  selector: 'app-onboarding',
  template: `
    <div class="onboarding">
      <h1>Connect Your Readwise Account</h1>
      <p>Enter your Readwise API token to sync your reading data.</p>
      <a href="https://readwise.io/access_token" target="_blank">
        Get your token from Readwise →
      </a>

      <form (submit)="saveToken()">
        <input type="password" [(ngModel)]="token" placeholder="Readwise API Token" />
        <button type="submit" [disabled]="saving()">
          {{ saving() ? 'Validating...' : 'Connect' }}
        </button>
      </form>

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </div>
  `
})
export class OnboardingComponent {
  token = '';
  saving = signal(false);
  error = signal<string | null>(null);

  constructor(
    private onboardingService: OnboardingService,
    private router: Router
  ) {}

  async saveToken() {
    this.saving.set(true);
    this.error.set(null);

    try {
      await this.onboardingService.setReadwiseToken(this.token);
      this.router.navigate(['/dashboard']);
    } catch (e) {
      this.error.set('Invalid token. Please check and try again.');
    } finally {
      this.saving.set(false);
    }
  }
}
```

---

## API Endpoints Summary

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/magic-auth/start` | POST | No | Request 6-digit code |
| `/api/auth/magic-auth/verify` | POST | No | Verify code, get token |
| `/api/auth/me` | GET | Yes | Get current user |
| `/api/auth/logout` | POST | Yes | Clear session |
| `/api/onboarding/readwise-token` | POST | Yes | Store Readwise token |
| `/api/sync` | POST | Yes | Trigger sync (uses user's token) |

---

## Environment Variables

```bash
# WorkOS
WORKOS_API_KEY=sk_...
WORKOS_CLIENT_ID=client_...

# JWT
JWT_SECRET=<32+ character secret>
JWT_EXPIRATION=86400000  # 24 hours in ms
```

---

## WorkOS Dashboard Setup

1. Create account at [workos.com](https://workos.com)
2. Create new project
3. Enable **User Management** product
4. Enable **Magic Auth** authentication method
5. Enable **Vault** product
6. Copy API Key and Client ID
7. Configure redirect URIs for your domains

---

## Security Considerations

1. **Token encryption:** Readwise tokens encrypted at rest in WorkOS Vault with per-user keys
2. **JWT expiration:** 24-hour tokens, refresh on activity
3. **HTTPS only:** All endpoints require TLS
4. **Rate limiting:** Limit Magic Auth requests per email
5. **Token validation:** Verify Readwise token works before storing

---

## Files to Create

| File | Purpose |
|------|---------|
| `core/domain/User.kt` | User entity |
| `core/infrastructure/UserRepository.kt` | User persistence |
| `auth/application/AuthService.kt` | Authentication logic |
| `auth/application/VaultService.kt` | Token storage |
| `auth/api/AuthController.kt` | Auth endpoints |
| `auth/api/OnboardingController.kt` | Onboarding endpoints |
| `auth/infrastructure/JwtTokenProvider.kt` | JWT handling |
| `auth/infrastructure/JwtAuthenticationFilter.kt` | Security filter |
| `config/WorkOSConfig.kt` | WorkOS client bean |
| `config/SecurityConfig.kt` | Spring Security config |

---

## Implementation Order

1. **WorkOS account setup** — Create project, enable products
2. **User entity + migration** — Database foundation
3. **WorkOS SDK integration** — Client bean, config
4. **Magic Auth flow** — Start/verify endpoints
5. **JWT session management** — Token provider, filter
6. **Vault integration** — Store/retrieve Readwise tokens
7. **Onboarding flow** — Token collection UI
8. **Update sync** — Use per-user tokens
9. **Frontend auth** — Login, onboarding components
10. **Update all entities** — Add userId (from data-scalability-improvements.md)

---

## References

- [WorkOS Magic Auth Docs](https://workos.com/docs/user-management/magic-auth)
- [WorkOS Vault Docs](https://workos.com/docs/vault)
- [WorkOS Kotlin SDK](https://github.com/workos/workos-kotlin)
