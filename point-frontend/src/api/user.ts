import client from './client'
import type { UserInfo } from '@/types'

export async function searchUsers(q: string): Promise<UserInfo[]> {
  try {
    const { data } = await client.get('/users/search', { params: { q } })
    if (data.code === 0) return data.data || []
  } catch {
    /* ignore */
  }
  return []
}
