import React from 'react'
import { Brain, Shuffle } from 'lucide-react'

const ModeBadge = ({ mode }) => {
  const isML = mode === 'ML'

  return (
    <div className="flex items-center gap-2">
      <div
        className={`inline-flex items-center gap-2 px-4 py-2 rounded-full font-semibold text-sm ${
          isML
            ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white'
            : 'bg-gradient-to-r from-orange-600 to-red-600 text-white'
        }`}
      >
        {isML ? <Brain className="w-4 h-4" /> : <Shuffle className="w-4 h-4" />}
        {mode} Mode
      </div>
      <p className="text-sm text-slate-400">
        {isML
          ? 'ML-powered predictions based on your feedback'
          : 'Random selection from your dataset'}
      </p>
    </div>
  )
}

export default ModeBadge