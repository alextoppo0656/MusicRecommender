import React, { useEffect, useState, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { authAPI } from '../services/api'
import toast from 'react-hot-toast'

const CallbackPage = () => {
  const [searchParams] = useSearchParams()
  const [error, setError] = useState(null)
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const hasCalledAuth = useRef(false) // Prevent duplicate calls

  useEffect(() => {
    // If already authenticated, go to dashboard
    if (isAuthenticated) {
      console.log('CallbackPage: Already authenticated, redirecting...')
      navigate('/dashboard')
      return
    }

    // Prevent duplicate calls (React StrictMode issue)
    if (hasCalledAuth.current) {
      console.log('CallbackPage: Auth already in progress, skipping...')
      return
    }

    const handleCallback = async () => {
      console.log('CallbackPage: Starting auth callback')
      hasCalledAuth.current = true // Mark as called
      
      const code = searchParams.get('code')
      const errorParam = searchParams.get('error')

      if (errorParam) {
        console.error('CallbackPage: Authorization error:', errorParam)
        setError('Authorization cancelled or failed')
        toast.error('Authorization failed')
        setTimeout(() => navigate('/login'), 3000)
        return
      }

      if (!code) {
        console.error('CallbackPage: No authorization code received')
        setError('No authorization code received')
        toast.error('Invalid callback')
        setTimeout(() => navigate('/login'), 3000)
        return
      }

      console.log('CallbackPage: Auth code received, calling backend...')

      try {
        const redirectUri = `${window.location.origin}/callback`
        console.log('CallbackPage: Using redirect URI:', redirectUri)
        
        const authData = await authAPI.handleCallback(code, redirectUri)
        console.log('CallbackPage: Auth successful, logging in...')
        
        login(authData)
        // Navigation handled by login function in AuthContext
      } catch (err) {
        console.error('CallbackPage: Auth callback error:', err)
        
        // Don't show error if already navigating
        if (!isAuthenticated) {
          setError('Authentication failed. Please try again.')
          toast.error('Authentication failed')
          setTimeout(() => navigate('/login'), 3000)
        }
      }
    }

    handleCallback()
  }, []) // Empty deps - only run once

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      <div className="card max-w-md w-full text-center">
        {error ? (
          <>
            <div className="text-red-500 text-5xl mb-4">⚠️</div>
            <h2 className="text-xl font-semibold text-white mb-2">
              Authentication Error
            </h2>
            <p className="text-slate-400 mb-4">{error}</p>
            <p className="text-sm text-slate-500">
              Redirecting to login...
            </p>
          </>
        ) : (
          <>
            <Loader2 className="w-16 h-16 animate-spin text-green-500 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-white mb-2">
              Authenticating...
            </h2>
            <p className="text-slate-400">
              Please wait while we log you in
            </p>
          </>
        )}
      </div>
    </div>
  )
}

export default CallbackPage