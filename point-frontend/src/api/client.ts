import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Inject auth token
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('point_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle errors
client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('point_token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export default client
