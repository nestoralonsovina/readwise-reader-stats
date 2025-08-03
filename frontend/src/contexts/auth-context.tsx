import { createContext, useContext } from "react";
import type { ReactNode } from "react";
import type { AuthState } from "../types/auth.ts";
import { useSupabaseAuth } from "../hooks/use-supabase-auth.tsx";

const AuthContext = createContext<AuthState | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const supabaseAuth = useSupabaseAuth();

  // Convert Supabase User to our User type
  const user = supabaseAuth.user ? {
    id: supabaseAuth.user.id,
    email: supabaseAuth.user.email || '',
    name: supabaseAuth.user.user_metadata?.name,
    created_at: supabaseAuth.user.created_at,
    email_verified: supabaseAuth.user.email_confirmed_at !== null
  } : null;

  const value: AuthState = {
    isAuthenticated: !!supabaseAuth.user,
    user,
    loading: supabaseAuth.loading,
    error: supabaseAuth.error,
    signIn: supabaseAuth.signIn,
    signUp: supabaseAuth.signUp,
    signOut: supabaseAuth.signOut
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}