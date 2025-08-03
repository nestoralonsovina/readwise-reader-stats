import { Button } from "../ui/button.tsx";
import { useAuth } from "../../contexts/auth-context.tsx";

export function SignOutButton() {
  const { isAuthenticated, signOut, loading } = useAuth();

  if (isAuthenticated) {
    return (
      <Button 
        onClick={signOut} 
        variant="neutral" 
        disabled={loading}
      >
        {loading ? 'Signing out...' : 'Sign Out'}
      </Button>
    );
  }

  return null; // Don't show when not authenticated
}