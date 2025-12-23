import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Music, Sparkles, TrendingUp, Users } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { getSpotifyAuthUrl } from '../utils/spotify'

const LoginPage = () => {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard')
    }
  }, [isAuthenticated, navigate])

  const handleLogin = () => {
    window.location.href = getSpotifyAuthUrl()
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="max-w-4xl w-full">
        {/* Header */}
        <div className="text-center mb-12">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-green-500 to-emerald-600 rounded-2xl mb-6 shadow-2xl">
            <Music className="w-10 h-10 text-white" />
          </div>
          <h1 className="text-5xl font-bold text-white mb-4">
            AI Music Recommender
          </h1>
          <p className="text-xl text-slate-300">
            Discover new music powered by Machine Learning
          </p>
        </div>

        {/* Features */}
        <div className="grid md:grid-cols-3 gap-6 mb-12">
          <div className="card text-center">
            <div className="inline-flex items-center justify-center w-12 h-12 bg-purple-600 rounded-lg mb-4">
              <Sparkles className="w-6 h-6 text-white" />
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">
              Smart Discovery
            </h3>
            <p className="text-sm text-slate-400">
              ML algorithms learn from your preferences
            </p>
          </div>

          <div className="card text-center">
            <div className="inline-flex items-center justify-center w-12 h-12 bg-blue-600 rounded-lg mb-4">
              <TrendingUp className="w-6 h-6 text-white" />
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">
              Personalized
            </h3>
            <p className="text-sm text-slate-400">
              Recommendations tailored to your taste
            </p>
          </div>

          <div className="card text-center">
            <div className="inline-flex items-center justify-center w-12 h-12 bg-green-600 rounded-lg mb-4">
              <Users className="w-6 h-6 text-white" />
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">
              Multi-User
            </h3>
            <p className="text-sm text-slate-400">
              Isolated data for each user account
            </p>
          </div>
        </div>

        {/* CTA */}
        <div className="card text-center">
          <h2 className="text-2xl font-bold text-white mb-4">
            Ready to discover new music?
          </h2>
          <p className="text-slate-400 mb-6">
            Connect your Spotify account to get started
          </p>
          <button
            onClick={handleLogin}
            className="btn btn-primary text-lg px-8 py-4 inline-flex items-center gap-2 group"
          >
            <Music className="w-5 h-5 group-hover:scale-110 transition-transform" />
            Login with Spotify
          </button>
          <p className="text-xs text-slate-500 mt-4">
            Your data is private and never shared
          </p>
        </div>

        {/* Footer */}
        <div className="text-center mt-12 text-slate-500 text-sm">
          <p>Powered by Spotify + Last.fm + Machine Learning</p>
          <p className="mt-2">Built with Spring Boot & React</p>
        </div>
      </div>
    </div>
  )
}

export default LoginPage