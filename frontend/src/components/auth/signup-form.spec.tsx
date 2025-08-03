import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
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
  it.todo("should render name, email and password inputs");
  it.todo("should handle form submission");
  it.todo("should display loading state");
  it.todo("should display error messages");
  it.todo("should validate email format");
  it.todo("should require all fields");
  
  it("should render without crashing", () => {
    render(<SignUpForm />);
    expect(screen.getByRole("form")).toBeInTheDocument();
  });
});