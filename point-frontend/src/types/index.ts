/** Shared domain types for the point frontend. */

export interface UserInfo {
  id: number
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
  type: number
  status: number
  createTime: number
  extraData?: string
}

export interface Favorite {
  id: number
  entityType: string
  entityId: number
  createTime: number
}
