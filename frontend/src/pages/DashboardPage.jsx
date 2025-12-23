import React, { useState, useEffect } from 'react'
import {
  LogOut,
  Music,
  TrendingUp,
  Heart,
  MessageSquare,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Sparkles
} from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { musicAPI } from '../services/api'
import toast from 'react-hot-toast'
import StatCard from '../components/StatCard'
import TrackCard from '../components/TrackCard'
import ModeBadge from '../components/ModeBadge'
import LoadingSpinner from '../components/LoadingSpinner'

const DashboardPage = () => {
  const { user, logout } = useAuth()
  const [stats, setStats] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [mode, setMode] = useState(null)
  const [loading, setLoading] = useState(false)
  const [expanding, setExpanding] = useState(false)
  const [feedbackMap, setFeedbackMap] = useState({})

  useEffect(() => {
    loadStats()
  }, [])

  const loadStats = async () => {
    try {
      const data = await musicAPI.getStats()
      setStats(data)
    } catch (error) {
      console.error('Failed to load stats:', error)
    }
  }

  const handleExpand = async () => {
    setExpanding(true)
    try {
      const result = await musicAPI.expandDataset()
      toast.success(
        `✅ Added ${result.expandedAdded} tracks! Total: ${result.totalRows}`
      )
      await loadStats()
    } catch (error) {
      toast.error('Failed to expand dataset')
    } finally {
      setExpanding(false)
    }
  }

  const handleGenerateRecommendations = async () => {
    setLoading(true)
    try {
      const data = await musicAPI.getRecommendations()
      setRecommendations(data.recommendations || [])
      setMode(data.mode)
      setFeedbackMap({})
      toast.success(`🎵 Generated ${data.recommendations?.length || 0} recommendations!`)
    } catch (error) {
      toast.error('Failed to generate recommendations')
    } finally {
      setLoading(false)
    }
  }

  const handleNext = async () => {
    setLoading(true)
    try {
      const data = await musicAPI.getNextRecommendations()
      setRecommendations(data.recommendations || [])
      setMode(data.mode)
    } catch (error) {
      toast.error('Failed to load next batch')
    } finally {
      setLoading(false)
    }
  }

  const handlePrevious = async () => {
    setLoading(true)
    try {
      const data = await musicAPI.getPreviousRecommendations()
      setRecommendations(data.recommendations || [])
      setMode(data.mode)
    } catch (error) {
      toast.error('Failed to load previous batch')
    } finally {
      setLoading(false)
    }
  }

  const handleFeedback = async (track, liked) => {
    try {
      await musicAPI.submitFeedback({
        trackName: track.trackName,
        artist: track.artist,
        liked,
        album: track.album,
        year: track.year,
        spotifyId: track.spotifyId
      })
      
      const key = `${track.trackName}-${track.artist}`
      setFeedbackMap(prev => ({
        ...prev,
        [key]: liked ? 'liked' : 'skipped'
      }))
      
      await loadStats()
    } catch (error) {
      throw error
    }
  }

  return (
    <div className="min-h-screen p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-white flex items-center gap-3">
              <Music className="w-8 h-8 text-green-500" />
              AI Music Recommender
            </h1>
            <p className="text-slate-400 mt-1">
              Welcome back, <span className="text-green-400 font-semibold">{user?.displayName}</span>
            </p>
          </div>
          <button onClick={logout} className="btn btn-danger flex items-center gap-2">
            <LogOut className="w-4 h-4" />
            Logout
          </button>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <StatCard
            icon={Music}
            label="Total Songs"
            value={stats?.totalSongs}
            color="green"
          />
          <StatCard
            icon={Heart}
            label="Liked Songs"
            value={stats?.totalLiked}
            color="blue"
          />
          <StatCard
            icon={MessageSquare}
            label="Total Feedback"
            value={stats?.totalFeedback}
            color="purple"
          />
          <StatCard
            icon={TrendingUp}
            label="Feedback Liked"
            value={stats?.feedbackLiked}
            color="orange"
          />
        </div>

        {/* Expand Dataset */}
        <div className="card mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-white mb-2">
                Expand Your Dataset
              </h2>
              <p className="text-slate-400 text-sm">
                Import Spotify liked songs and discover similar tracks from Last.fm
              </p>
            </div>
            <button
              onClick={handleExpand}
              disabled={expanding}
              className="btn btn-primary flex items-center gap-2"
            >
              <RefreshCw className={`w-4 h-4 ${expanding ? 'animate-spin' : ''}`} />
              {expanding ? 'Expanding...' : 'Expand Dataset'}
            </button>
          </div>
        </div>

        {/* Recommendations Section */}
        <div className="card mb-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-white mb-2">
                Personalized Recommendations
              </h2>
              {mode && <ModeBadge mode={mode} />}
            </div>
            <div className="flex gap-2">
              <button
                onClick={handlePrevious}
                disabled={loading || !recommendations.length}
                className="btn btn-secondary flex items-center gap-2"
              >
                <ChevronLeft className="w-4 h-4" />
                Previous
              </button>
              <button
                onClick={handleGenerateRecommendations}
                disabled={loading}
                className="btn btn-primary flex items-center gap-2"
              >
                <Sparkles className="w-4 h-4" />
                Generate
              </button>
              <button
                onClick={handleNext}
                disabled={loading || !recommendations.length}
                className="btn btn-secondary flex items-center gap-2"
              >
                Next
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>

          {loading && <LoadingSpinner text="Loading recommendations..." />}

          {!loading && recommendations.length === 0 && (
            <div className="text-center py-12">
              <Music className="w-16 h-16 text-slate-600 mx-auto mb-4" />
              <p className="text-slate-400 mb-2">No recommendations yet</p>
              <p className="text-sm text-slate-500">
                Click "Generate" to get started
              </p>
            </div>
          )}

          {!loading && recommendations.length > 0 && (
            <div className="space-y-4">
              {recommendations.map((track, index) => {
                const key = `${track.trackName}-${track.artist}`
                return (
                  <TrackCard
                    key={index}
                    track={track}
                    onFeedback={handleFeedback}
                    feedbackGiven={feedbackMap[key]}
                  />
                )
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="text-center text-slate-500 text-sm">
          <p>💚 Powered by Spring Boot + React + Machine Learning</p>
        </div>
      </div>
    </div>
  )
}

export default DashboardPage