import React from 'react'
import { Loader2 } from 'lucide-react'

const LoadingSpinner = ({ text = 'Loading...' }) => {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <Loader2 className="w-12 h-12 animate-spin text-green-500 mb-4" />
      <p className="text-slate-400 text-sm">{text}</p>
    </div>
  )
}

export default LoadingSpinner