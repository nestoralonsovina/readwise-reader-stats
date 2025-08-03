import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { AuthGuard } from "./auth-guard.tsx";
import { AuthProvider } from "../../contexts/auth-context.tsx";

// Mock useAuth hook
vi.mock("../../contexts/auth-context.tsx", async () => {
  const actual = await vi.importActual("../../contexts/auth-context.tsx");
  return {
    ...actual,
    useAuth: vi.fn(() => ({
      isAuthenticated: false,
      user: null,
      signOut: vi.fn(),
      signIn: vi.fn(),
      signUp: vi.fn(),
      loading: false,
      error: null
    })),
    AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>
  };
});

describe("AuthGuard", () => {
  it("should render children when user is authenticated", async () => {
    const { useAuth } = await import("../../contexts/auth-context.tsx");
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: { id: "123", email: "test@example.com" } as any,
      signOut: vi.fn(),
      signIn: vi.fn(),
      signUp: vi.fn(),
      loading: false,
      error: null
    });

    render(
      <AuthProvider>
        <AuthGuard>
          <div>Protected content</div>
        </AuthGuard>
      </AuthProvider>
    );
    
    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });
  it("should render login prompt when user is not authenticated", async () => {
    const { useAuth } = await import("../../contexts/auth-context.tsx");
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      user: null,
      signOut: vi.fn(),
      signIn: vi.fn(),
      signUp: vi.fn(),
      loading: false,
      error: null
    });

    render(
      <AuthProvider>
        <AuthGuard>
          <div>Protected content</div>
        </AuthGuard>
      </AuthProvider>
    );
    
    expect(screen.getByText("Welcome to Readwise Analytics")).toBeInTheDocument();
    expect(screen.queryByText("Protected content")).not.toBeInTheDocument();
  });
  
  it("should render without crashing", async () => {
    const { useAuth } = await import("../../contexts/auth-context.tsx");
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      user: null,
      signOut: vi.fn(),
      signIn: vi.fn(),
      signUp: vi.fn(),
      loading: false,
      error: null
    });

    render(
      <AuthProvider>
        <AuthGuard>
          <div>Protected content</div>
        </AuthGuard>
      </AuthProvider>
    );
    
    // Should show auth forms when not authenticated
    expect(screen.getByText("Welcome to Readwise Analytics")).toBeInTheDocument();
  });
});