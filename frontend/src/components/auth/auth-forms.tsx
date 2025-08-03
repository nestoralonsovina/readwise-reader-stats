import { useState } from 'react'
import { Button } from '../ui/button.tsx'
import { SignInForm } from './signin-form.tsx'
import { SignUpForm } from './signup-form.tsx'

export function AuthForms() {
  const [isSignUp, setIsSignUp] = useState(false)

  return (
    <div className="w-full max-w-md mx-auto">
      <div className="flex justify-center mb-6">
        <div className="flex border-2 border-black rounded-base overflow-hidden">
          <Button
            variant={!isSignUp ? "default" : "neutral"}
            className={`rounded-none border-0 ${!isSignUp ? '' : 'shadow-none translate-x-0 translate-y-0'}`}
            onClick={() => setIsSignUp(false)}
          >
            Sign In
          </Button>
          <Button
            variant={isSignUp ? "default" : "neutral"}
            className={`rounded-none border-0 ${isSignUp ? '' : 'shadow-none translate-x-0 translate-y-0'}`}
            onClick={() => setIsSignUp(true)}
          >
            Sign Up
          </Button>
        </div>
      </div>
      
      {isSignUp ? <SignUpForm /> : <SignInForm />}
    </div>
  )
}