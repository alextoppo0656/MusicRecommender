// Location: frontend/src/contexts/AuthContext.jsx

import React, { createContext, useState, useContext, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'

const AuthContext = createContext(null)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  // Initialize auth from localStorage on mount
  useEffect(() => {
    const initializeAuth = async () => {
      console.log('AuthContext: Initializing...')
      try {
        const storedToken = localStorage.getItem('token')
        const storedUser = localStorage.getItem('user')
        
        if (storedToken && storedUser) {
          console.log('AuthContext: Found stored credentials')
          setToken(storedToken)
          setUser(JSON.parse(storedUser))
        } else {
          console.log('AuthContext: No stored credentials')
        }
      } catch (error) {
        console.error('AuthContext: Initialization error:', error)
        // Clear corrupted data
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      } finally {
        setLoading(false)
      }
    }

    initializeAuth()
  }, [])

  const login = (authData) => {
    console.log('AuthContext: Login called', authData)
    
    // authData comes from backend: { accessToken, userInfo: { userId, displayName, email } }
    const { accessToken, userInfo } = authData
    
    // Store in localStorage
    localStorage.setItem('token', accessToken)
    localStorage.setItem('user', JSON.stringify(userInfo))
    
    // Update state
    setToken(accessToken)
    setUser(userInfo)
    
    // Show success message
    toast.success(`Welcome, ${userInfo.displayName}!`)
    
    // Navigate to dashboard
    console.log('AuthContext: Navigating to dashboard')
    navigate('/dashboard')
  }

  const logout = async () => {
    console.log('AuthContext: Logout called')
    
    try {
      // Call backend logout endpoint
      if (token) {
        await fetch('/api/auth/logout', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }).catch(err => console.error('Logout API call failed:', err))
      }
    } catch (error) {
      console.error('AuthContext: Logout error:', error)
    } finally {
      // Clear state and storage regardless of API call result
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      setToken(null)
      setUser(null)
      
      toast.success('Logged out successfully')
      navigate('/login')
    }
  }

  const value = {
    user,
    token,
    loading,
    isAuthenticated: !!token && !!user,
    login,
    logout
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}