import React from 'react'

const StatCard = ({ icon: Icon, label, value, color = 'green' }) => {
  const colorClasses = {
    green: 'from-green-600 to-emerald-600',
    blue: 'from-blue-600 to-cyan-600',
    purple: 'from-purple-600 to-pink-600',
    orange: 'from-orange-600 to-red-600'
  }

  return (
    <div className="card hover:shadow-2xl transition-all duration-300">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-slate-400 text-sm font-medium mb-1">{label}</p>
          <p className="text-3xl font-bold text-white">{value?.toLocaleString() || 0}</p>
        </div>
        <div className={`p-3 rounded-lg bg-gradient-to-br ${colorClasses[color]}`}>
          <Icon className="w-6 h-6 text-white" />
        </div>
      </div>
    </div>
  )
}

export default StatCard