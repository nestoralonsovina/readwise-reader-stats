import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { SignOutButton } from "./sign-out-button.tsx";
import { AuthProvider } from "../../contexts/auth-context.tsx";

// Mock useAuth hook
const mockSignOut = vi.fn();
vi.mock("../../contexts/auth-context.tsx", async () => {
  const actual = await vi.importActual("../../contexts/auth-context.tsx");
  return {
    ...actual,
    useAuth: vi.fn(() => ({
      isAuthenticated: false,
      user: null,
      signOut: mockSignOut,
      signIn: vi.fn(),
      signUp: vi.fn(),
      loading: false,
      error: null
    })),
    AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>
  };
});

describe("SignOutButton", () => {
  it("should render sign out button when user is authenticated", async () => {
    const { useAuth } = await import("../../contexts/auth-context.tsx");
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: { id: "123", email: "test@example.com" } as any,
      signOut: mockSignOut,
      signIn: vi.fn(),
      signUp: vi.fn(),
      loading: false,
      error: null
    });

    render(
      <AuthProvider>
        <SignOutButton />
      </AuthProvider>
    );
    
    expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument();
  });
  it("should call signOut function when sign out button is clicked", async () => {
    const { useAuth } = await import("../../contexts/auth-context.tsx");
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: { id: "123", email: "test@example.com" } as any,
      signOut: mockSignOut,
      signIn: vi.fn(),
      signUp: vi.fn(),
      loading: false,
      error: null
    });

    const user = userEvent.setup();
    render(
      <AuthProvider>
        <SignOutButton />
      </AuthProvider>
    );
    
    const signOutButton = screen.getByRole("button", { name: /sign out/i });
    await user.click(signOutButton);
    
    expect(mockSignOut).toHaveBeenCalled();
  });
  it("should not render when user is not authenticated", async () => {
    const { useAuth } = await import("../../contexts/auth-context.tsx");
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      user: null,
      signOut: mockSignOut,
      signIn: vi.fn(),
      signUp: vi.fn(),
      loading: false,
      error: null
    });

    render(
      <AuthProvider>
        <SignOutButton />
      </AuthProvider>
    );
    
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
  
  it("should render with correct initial state", () => {
    render(
      <AuthProvider>
        <SignOutButton />
      </AuthProvider>
    );
    
    // SignOutButton returns null when not authenticated, so no button should be present
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});