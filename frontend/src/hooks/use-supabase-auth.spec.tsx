import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook } from "@testing-library/react";
import { useSupabaseAuth } from "./use-supabase-auth.tsx";

// Mock Supabase client
vi.mock("../lib/supabase.ts", () => ({
  supabase: {
    auth: {
      signInWithPassword: vi.fn(() => Promise.resolve({ error: null })),
      signUp: vi.fn(() => Promise.resolve({ error: null })),
      signOut: vi.fn(() => Promise.resolve({ error: null })),
      getSession: vi.fn(() => Promise.resolve({ 
        data: { session: null }, 
        error: null 
      })),
      onAuthStateChange: vi.fn(() => ({
        data: { subscription: { unsubscribe: vi.fn() } }
      }))
    }
  }
}));

describe("useSupabaseAuth", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return initial loading state", () => {
    const { result } = renderHook(() => useSupabaseAuth());
    
    expect(result.current.loading).toBe(true);
    expect(result.current.user).toBe(null);
    expect(result.current.error).toBe(null);
  });
  it("should handle successful sign in", async () => {
    const { result } = renderHook(() => useSupabaseAuth());
    
    await result.current.signIn("test@example.com", "password");
    
    const { supabase } = await import("../lib/supabase.ts");
    expect(supabase.auth.signInWithPassword).toHaveBeenCalledWith({
      email: "test@example.com",
      password: "password"
    });
  });
  it("should handle sign in errors", async () => {
    const { supabase } = await import("../lib/supabase.ts");
    vi.mocked(supabase.auth.signInWithPassword).mockResolvedValueOnce({
      error: { message: "Invalid credentials" }
    } as any);

    const { result } = renderHook(() => useSupabaseAuth());
    
    await result.current.signIn("test@example.com", "wrong-password");
    
    // Wait for state update after async operation
    await vi.waitFor(() => {
      expect(result.current.error).toBe("Invalid credentials");
    });
  });
  it("should handle sign out", async () => {
    const { result } = renderHook(() => useSupabaseAuth());
    
    await result.current.signOut();
    
    const { supabase } = await import("../lib/supabase.ts");
    expect(supabase.auth.signOut).toHaveBeenCalled();
  });
  it("should restore session on mount", async () => {
    const mockUser = { id: "123", email: "test@example.com" };
    const { supabase } = await import("../lib/supabase.ts");
    vi.mocked(supabase.auth.getSession).mockResolvedValueOnce({
      data: { session: { user: mockUser } },
      error: null
    } as any);

    const { result } = renderHook(() => useSupabaseAuth());

    await vi.waitFor(() => {
      expect(result.current.user).toEqual(mockUser);
      expect(result.current.loading).toBe(false);
    });
  });
  it("should handle successful sign up with name", async () => {
    const { result } = renderHook(() => useSupabaseAuth());
    
    await result.current.signUp("test@example.com", "password", "John Doe");
    
    const { supabase } = await import("../lib/supabase.ts");
    expect(supabase.auth.signUp).toHaveBeenCalledWith({
      email: "test@example.com",
      password: "password",
      options: {
        data: {
          name: "John Doe"
        }
      }
    });
  });
  it("should handle sign up errors", async () => {
    const { supabase } = await import("../lib/supabase.ts");
    vi.mocked(supabase.auth.signUp).mockResolvedValueOnce({
      error: { message: "Email already exists" }
    } as any);

    const { result } = renderHook(() => useSupabaseAuth());
    
    await result.current.signUp("existing@example.com", "password", "John Doe");
    
    await vi.waitFor(() => {
      expect(result.current.error).toBe("Email already exists");
    });
  });
  it("should store name in user metadata on sign up", async () => {
    const { result } = renderHook(() => useSupabaseAuth());
    
    await result.current.signUp("test@example.com", "password", "Jane Smith");
    
    const { supabase } = await import("../lib/supabase.ts");
    expect(supabase.auth.signUp).toHaveBeenCalledWith({
      email: "test@example.com", 
      password: "password",
      options: {
        data: {
          name: "Jane Smith"
        }
      }
    });
  });
  
  it("should be defined", () => {
    const { result } = renderHook(() => useSupabaseAuth());
    expect(result.current).toBeDefined();
  });
});