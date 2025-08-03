import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { SignUpForm } from "./signup-form.tsx";

// Mock the auth hook
vi.mock("../../hooks/use-supabase-auth.tsx", () => ({
  useSupabaseAuth: vi.fn(() => ({
    signUp: vi.fn(),
    loading: false,
    error: null
  }))
}));

describe("SignUpForm", () => {
  it("should render name, email and password inputs", () => {
    render(<SignUpForm />);
    
    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign up/i })).toBeInTheDocument();
  });
  it("should handle form submission", async () => {
    const mockSignUp = vi.fn();
    const { useSupabaseAuth } = await import("../../hooks/use-supabase-auth.tsx");
    vi.mocked(useSupabaseAuth).mockReturnValue({
      signUp: mockSignUp,
      loading: false,
      error: null,
      signIn: vi.fn(),
      signOut: vi.fn(),
      user: null
    });

    const user = userEvent.setup();
    render(<SignUpForm />);
    
    const nameInput = screen.getByLabelText(/name/i);
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole("button", { name: /sign up/i });
    
    await user.type(nameInput, "John Doe");
    await user.type(emailInput, "test@example.com");
    await user.type(passwordInput, "password123");
    await user.click(submitButton);
    
    expect(mockSignUp).toHaveBeenCalledWith("test@example.com", "password123", "John Doe");
  });
  it("should display loading state", async () => {
    const { useSupabaseAuth } = await import("../../hooks/use-supabase-auth.tsx");
    vi.mocked(useSupabaseAuth).mockReturnValue({
      signUp: vi.fn(),
      loading: true,
      error: null,
      signIn: vi.fn(),
      signOut: vi.fn(),
      user: null
    });

    render(<SignUpForm />);
    
    const submitButton = screen.getByRole("button", { name: /creating account/i });
    expect(submitButton).toBeDisabled();
  });
  it("should display error messages", async () => {
    const { useSupabaseAuth } = await import("../../hooks/use-supabase-auth.tsx");
    vi.mocked(useSupabaseAuth).mockReturnValue({
      signUp: vi.fn(),
      loading: false,
      error: "Email already exists",
      signIn: vi.fn(),
      signOut: vi.fn(),
      user: null
    });

    render(<SignUpForm />);
    
    expect(screen.getByText("Email already exists")).toBeInTheDocument();
  });
  it("should validate email format", async () => {
    const user = userEvent.setup();
    render(<SignUpForm />);
    
    const emailInput = screen.getByLabelText(/email/i);
    const submitButton = screen.getByRole("button", { name: /sign up/i });
    
    // Try to submit with invalid email
    await user.type(emailInput, "invalid-email");
    await user.click(submitButton);
    
    // HTML5 validation should prevent submission
    expect(emailInput).toBeInvalid();
  });
  it("should require all fields", async () => {
    render(<SignUpForm />);
    
    const nameInput = screen.getByLabelText(/name/i);
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    
    // Check that all inputs are required
    expect(nameInput).toBeRequired();
    expect(emailInput).toBeRequired();
    expect(passwordInput).toBeRequired();
  });
  
  it("should render without crashing", () => {
    render(<SignUpForm />);
    expect(screen.getByRole("form")).toBeInTheDocument();
  });
});