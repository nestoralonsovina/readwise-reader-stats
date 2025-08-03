import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { SignOutButton } from "./sign-out-button.tsx";
import { AuthProvider } from "../../contexts/auth-context.tsx";

describe("SignOutButton", () => {
  it.todo("should render sign out button when user is authenticated");
  it.todo("should call signOut function when sign out button is clicked");
  it.todo("should not render when user is not authenticated");
  
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