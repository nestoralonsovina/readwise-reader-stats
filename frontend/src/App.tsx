import './App.css'
import { AuthGuard } from "./components/auth/auth-guard.tsx";
import { SignOutButton } from "./components/auth/sign-out-button.tsx";
import { useAuth } from "./contexts/auth-context.tsx";

function App() {
  const { user, isAuthenticated } = useAuth();

  return (
    <AuthGuard>
      <div className="min-h-screen bg-white p-8">
        <header className="flex justify-between items-center mb-8 border-b-2 border-black pb-4">
          <h1 className="text-3xl font-bold">Readwise Analytics Dashboard</h1>
          <div className="flex items-center gap-4">
            {isAuthenticated && user && (
              <span className="text-lg">
                Welcome, {user.name || user.email}!
              </span>
            )}
            <SignOutButton />
          </div>
        </header>
        
        <main>
          <h2 className="text-2xl font-bold mb-4">Dashboard Content</h2>
          <p className="text-lg">Your reading analytics will appear here.</p>
          {user && (
            <div className="mt-4 p-4 border-2 border-black rounded-base">
              <h3 className="font-bold">User Information:</h3>
              <p>Email: {user.email}</p>
              <p>ID: {user.id}</p>
              {user.created_at && (
                <p>Member since: {new Date(user.created_at).toLocaleDateString()}</p>
              )}
            </div>
          )}
        </main>
      </div>
    </AuthGuard>
  )
}

export default App
