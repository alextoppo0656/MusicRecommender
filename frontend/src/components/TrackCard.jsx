import React, { useState } from 'react'
import { ThumbsUp, ThumbsDown, ExternalLink, Music2 } from 'lucide-react'
import { getSpotifySearchUrl } from '../utils/spotify'
import toast from 'react-hot-toast'

const TrackCard = ({ track, onFeedback, feedbackGiven }) => {
  const [loading, setLoading] = useState(false)

  const handleFeedback = async (liked) => {
    setLoading(true)
    try {
      await onFeedback(track, liked)
      toast.success(liked ? '💚 Liked!' : '👎 Skipped!')
    } catch (error) {
      toast.error('Failed to save feedback')
    } finally {
      setLoading(false)
    }
  }

  const spotifyUrl = getSpotifySearchUrl(track.trackName, track.artist)

  return (
    <div className="card hover:bg-slate-800/70 transition-all duration-300 group">
      <div className="flex items-start gap-4">
        {/* Album Art Placeholder */}
        <div className="flex-shrink-0 w-16 h-16 bg-gradient-to-br from-purple-600 to-blue-600 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
          <Music2 className="w-8 h-8 text-white" />
        </div>

        {/* Track Info */}
        <div className="flex-1 min-w-0">
          <h3 className="text-lg font-semibold text-white truncate mb-1">
            {track.trackName}
          </h3>
          <p className="text-slate-400 text-sm truncate mb-2">
            {track.artist}
          </p>

          <div className="flex flex-wrap gap-2 text-xs text-slate-500">
            {track.album && (
              <span className="bg-slate-700/50 px-2 py-1 rounded">
                💿 {track.album}
              </span>
            )}
            {track.year && (
              <span className="bg-slate-700/50 px-2 py-1 rounded">
                📅 {track.year}
              </span>
            )}
            {track.source && (
              <span className="bg-slate-700/50 px-2 py-1 rounded capitalize">
                🔍 {track.source.replace(/_/g, ' ')}
              </span>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="flex flex-col gap-2">
          <a
            href={spotifyUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary text-xs px-3 py-1.5 flex items-center gap-1"
          >
            <ExternalLink className="w-3 h-3" />
            Spotify
          </a>

          {!feedbackGiven && (
            <div className="flex gap-2">
              <button
                onClick={() => handleFeedback(true)}
                disabled={loading}
                className="btn btn-primary text-xs px-3 py-1.5 flex items-center gap-1"
              >
                <ThumbsUp className="w-3 h-3" />
              </button>
              <button
                onClick={() => handleFeedback(false)}
                disabled={loading}
                className="btn btn-danger text-xs px-3 py-1.5 flex items-center gap-1"
              >
                <ThumbsDown className="w-3 h-3" />
              </button>
            </div>
          )}

          {feedbackGiven && (
            <div className="text-xs text-slate-400 text-center mt-1">
              {feedbackGiven === 'liked' ? '💚 Liked' : '👎 Skipped'}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default TrackCard