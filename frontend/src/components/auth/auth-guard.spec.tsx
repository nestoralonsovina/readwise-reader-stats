import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { AuthGuard } from "./auth-guard.tsx";
import { AuthProvider } from "../../contexts/auth-context.tsx";

describe("AuthGuard", () => {
  it.todo("should render children when user is authenticated");
  it.todo("should render login prompt when user is not authenticated");
  
  it("should render without crashing", () => {
    render(
      <AuthProvider>
        <AuthGuard>
          <div>Protected content</div>
        </AuthGuard>
      </AuthProvider>
    );
    
    // Initially shows loading, then shows sign in form
    expect(screen.getByText("Loading...") || screen.getByText("Welcome to Readwise Analytics")).toBeInTheDocument();
  });
});