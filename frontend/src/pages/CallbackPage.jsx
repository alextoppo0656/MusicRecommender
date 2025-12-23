import React, { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { authAPI } from '../services/api'
import toast from 'react-hot-toast'

const CallbackPage = () => {
  const [searchParams] = useSearchParams()
  const [error, setError] = useState(null)
  const { login } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    const handleCallback = async () => {
      const code = searchParams.get('code')
      const errorParam = searchParams.get('error')

      if (errorParam) {
        setError('Authorization cancelled or failed')
        toast.error('Authorization failed')
        setTimeout(() => navigate('/login'), 3000)
        return
      }

      if (!code) {
        setError('No authorization code received')
        toast.error('Invalid callback')
        setTimeout(() => navigate('/login'), 3000)
        return
      }

      try {
        const redirectUri = `${window.location.origin}/callback`
        const authData = await authAPI.handleCallback(code, redirectUri)
        
        login(authData)
      } catch (err) {
        console.error('Auth callback error:', err)
        setError('Authentication failed. Please try again.')
        toast.error('Authentication failed')
        setTimeout(() => navigate('/login'), 3000)
      }
    }

    handleCallback()
  }, [searchParams, login, navigate])

  return (
    <div className="min-h-screen flex items-center justify-center">
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