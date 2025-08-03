import type { ReactNode } from "react";
import { useAuth } from "../../contexts/auth-context.tsx";
import { SignInForm } from "./signin-form.tsx";

interface AuthGuardProps {
  children: ReactNode;
}

export function AuthGuard({ children }: AuthGuardProps) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-xl">Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen p-8">
        <div className="mb-8 text-center">
          <h2 className="text-3xl font-bold mb-2">Welcome to Readwise Analytics</h2>
          <p className="text-lg text-gray-600">Please sign in to access your reading dashboard</p>
        </div>
        <SignInForm />
      </div>
    );
  }

  return <>{children}</>;
}