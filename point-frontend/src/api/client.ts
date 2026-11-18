import axios from 'axios'
import router from '@/router'

const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Session auth: the HttpOnly SameSite=Lax cookie rides along automatically
// on same-origin requests — no token handling here.

// Handle errors
client.interceptors.response.use(
  (res) => res,
  (err) => {
    // freeway 2.0 error bodies are { error, code }; expose the message under
    // the same `message` field the SPA already reads.
    const body = err.response?.data
    if (body && body.error && !body.message) {
      body.message = body.error
    }
    if (err.response?.status === 401) {
      router.push('/login')
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
