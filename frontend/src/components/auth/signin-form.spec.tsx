import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { SignInForm } from "./signin-form.tsx";

// Mock the auth hook
vi.mock("../../hooks/use-supabase-auth.tsx", () => ({
  useSupabaseAuth: vi.fn(() => ({
    signIn: vi.fn(),
    loading: false,
    error: null
  }))
}));

describe("SignInForm", () => {
  it("should render email and password inputs", () => {
    render(<SignInForm />);
    
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();
  });
  it("should handle form submission", async () => {
    const mockSignIn = vi.fn();
    const { useSupabaseAuth } = await import("../../hooks/use-supabase-auth.tsx");
    vi.mocked(useSupabaseAuth).mockReturnValue({
      signIn: mockSignIn,
      loading: false,
      error: null,
      signOut: vi.fn(),
      signUp: vi.fn(),
      user: null
    });

    const user = userEvent.setup();
    render(<SignInForm />);
    
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole("button", { name: /sign in/i });
    
    await user.type(emailInput, "test@example.com");
    await user.type(passwordInput, "password123");
    await user.click(submitButton);
    
    expect(mockSignIn).toHaveBeenCalledWith("test@example.com", "password123");
  });
  it("should display loading state", async () => {
    const { useSupabaseAuth } = await import("../../hooks/use-supabase-auth.tsx");
    vi.mocked(useSupabaseAuth).mockReturnValue({
      signIn: vi.fn(),
      loading: true,
      error: null,
      signOut: vi.fn(),
      signUp: vi.fn(),
      user: null
    });

    render(<SignInForm />);
    
    const submitButton = screen.getByRole("button", { name: /signing in/i });
    expect(submitButton).toBeDisabled();
  });
  it("should display error messages", async () => {
    const { useSupabaseAuth } = await import("../../hooks/use-supabase-auth.tsx");
    vi.mocked(useSupabaseAuth).mockReturnValue({
      signIn: vi.fn(),
      loading: false,
      error: "Invalid credentials",
      signOut: vi.fn(),
      signUp: vi.fn(),
      user: null
    });

    render(<SignInForm />);
    
    expect(screen.getByText("Invalid credentials")).toBeInTheDocument();
  });
  it("should validate email format", async () => {
    const user = userEvent.setup();
    render(<SignInForm />);
    
    const emailInput = screen.getByLabelText(/email/i);
    const submitButton = screen.getByRole("button", { name: /sign in/i });
    
    // Try to submit with invalid email
    await user.type(emailInput, "invalid-email");
    await user.click(submitButton);
    
    // HTML5 validation should prevent submission
    expect(emailInput).toBeInvalid();
  });
  
  it("should render without crashing", () => {
    render(<SignInForm />);
    expect(screen.getByRole("form")).toBeInTheDocument();
  });
});