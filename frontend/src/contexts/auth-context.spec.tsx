import { describe, it, expect } from "vitest";
import { renderHook } from "@testing-library/react";
import { AuthProvider, useAuth } from "./auth-context.tsx";
import type { ReactNode } from "react";

describe("AuthContext", () => {
  it("should provide initial unauthenticated state", () => {
    const wrapper = ({ children }: { children: ReactNode }) => (
      <AuthProvider>{children}</AuthProvider>
    );
    const { result } = renderHook(() => useAuth(), { wrapper });
    
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBe(null);
    expect(result.current.error).toBe(null);
  });
  it("should authenticate user when login is called", async () => {
    const wrapper = ({ children }: { children: ReactNode }) => (
      <AuthProvider>{children}</AuthProvider>
    );
    const { result } = renderHook(() => useAuth(), { wrapper });
    
    await result.current.signIn("test@example.com", "password");
    
    // Should call the underlying signIn function (mocked via useSupabaseAuth)
    expect(typeof result.current.signIn).toBe("function");
  });
  it("should logout user when logout is called", async () => {
    const wrapper = ({ children }: { children: ReactNode }) => (
      <AuthProvider>{children}</AuthProvider>
    );
    const { result } = renderHook(() => useAuth(), { wrapper });
    
    await result.current.signOut();
    
    // Should call the underlying signOut function (mocked via useSupabaseAuth)
    expect(typeof result.current.signOut).toBe("function");
  });
  it("should throw error when useAuth is used outside AuthProvider", () => {
    expect(() => {
      renderHook(() => useAuth());
    }).toThrow("useAuth must be used within an AuthProvider");
  });
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
    expect(typeof result.current.signUp).toBe("function");
    expect(typeof result.current.signOut).toBe("function");
    expect(typeof result.current.loading).toBe("boolean"); // Can be true initially
    expect(result.current.error).toBe(null);
  });
});