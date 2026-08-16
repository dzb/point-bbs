/** Shared domain types for the point frontend. */

export interface UserInfo {
  id: number
  username?: string
  nickname: string
  avatar?: string | null
  email?: string
  description?: string
  score?: number
  topicCount?: number
  commentCount?: number
  fansCount?: number
  followCount?: number
}

export interface Topic {
  id: number
  type: number
  title: string
  content: string
  contentType: string
  categoryId: number
  userId: number
  viewCount: number
  commentCount: number
  likeCount: number
  status: number
  sticky: boolean
  recommend: boolean
  createTime: number
  lastCommentTime: number
  imageList?: string
  hideContent?: string
  qaStatus?: string
  bountyScore?: number
  user?: UserInfo
  liked?: boolean
  favorited?: boolean
}

export interface Article {
  id: number
  title: string
  summary?: string
  content: string
  contentType: string
  cover?: string
  sourceUrl?: string
  viewCount: number
  commentCount: number
  likeCount: number
  userId: number
  createTime: number
  user?: UserInfo
  tags?: string[]
}

export interface Comment {
  id: number
  entityType: string
  entityId: number
  content: string
  contentType: string
  quoteId: number
  quoteContent?: string | null
  /** client-side like state (set by views, not from the API) */
  _liked?: boolean
  likeCount: number
  commentCount: number
  status: number
  createTime: number
  imageList?: string
  user?: UserInfo
}

export interface Message {
  id: number
  fromId: number
  userId: number
  title: string
  content: string
  quoteContent?: string | null
  type: number
  status: number
  createTime: number
  extraData?: string
  senderName?: string
}

export interface Favorite {
  id: number
  entityType: string
  entityId: number
  createTime: number
}

/** Standard paginated list envelope returned by list endpoints. */
export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

/** Auth endpoints return a minimal profile alongside the token. */
export interface AuthResult {
  token: string
  id: number
  nickname: string
  avatar: string | null
}

/** API envelope: { code, message, data }. */
export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}
