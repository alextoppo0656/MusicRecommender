import axios from 'axios'
import toast from 'react-hot-toast'

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 180000, // 3 minutes for expand operations
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
      toast.error('Session expired. Please log in again.')
    } else if (error.response?.status === 429) {
      toast.error('Too many requests. Please wait a moment.')
    } else if (error.response?.status >= 500) {
      toast.error('Server error. Please try again later.')
    } else if (error.code === 'ECONNABORTED') {
      toast.error('Request timeout. Please try again.')
    } else if (!error.response) {
      toast.error('Network error. Check your connection.')
    }
    return Promise.reject(error)
  }
)

// Auth endpoints
export const authAPI = {
  handleCallback: async (code, redirectUri) => {
    const response = await api.post('/auth/callback', { code, redirectUri })
    return response.data
  },
  
  logout: async () => {
    const response = await api.post('/auth/logout')
    return response.data
  }
}

// Music endpoints
export const musicAPI = {
  expandDataset: async () => {
    const response = await api.post('/expand')
    return response.data
  },
  
  getStats: async () => {
    const response = await api.get('/stats')
    return response.data
  },
  
  getRecommendations: async () => {
    const response = await api.post('/recommend')
    return response.data
  },
  
  getNextRecommendations: async () => {
    const response = await api.post('/recommend/next')
    return response.data
  },
  
  getPreviousRecommendations: async () => {
    const response = await api.post('/recommend/previous')
    return response.data
  },
  
  submitFeedback: async (feedbackData) => {
    const response = await api.post('/feedback', feedbackData)
    return response.data
  }
}

// Health check
export const healthCheck = async () => {
  try {
    const response = await api.get('/health')
    return response.data
  } catch (error) {
    console.error('Health check failed:', error)
    return null
  }
}

export default api