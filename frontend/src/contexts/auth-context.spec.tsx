import { describe, it, expect } from "vitest";
import { renderHook } from "@testing-library/react";
import { AuthProvider, useAuth } from "./auth-context.tsx";
import type { ReactNode } from "react";

describe("AuthContext", () => {
  it.todo("should provide initial unauthenticated state");
  it.todo("should authenticate user when login is called");
  it.todo("should logout user when logout is called");
  it.todo("should throw error when useAuth is used outside AuthProvider");
});

describe("useAuth hook", () => {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <AuthProvider>{children}</AuthProvider>
  );

  it("should return initial unauthenticated state", () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBe(null);
    expect(typeof result.current.signIn).toBe("function");
    expect(typeof result.current.signOut).toBe("function");
    expect(typeof result.current.loading).toBe("boolean"); // Can be true initially
    expect(result.current.error).toBe(null);
  });
});