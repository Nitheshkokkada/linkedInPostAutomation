export interface User {
  id: string
  email: string
  fullName: string
  timezone: string
  postingMode: "auto" | "manual" | "draft"
  linkedinConnected: boolean
}

export interface Topic {
  id: string
  name: string
  category: "ai" | "software_engineering" | "java" | "spring_boot" | "cloud" | "system_design" | "career"
  isActive: boolean
  priority: number
  createdAt: string
}

export interface Post {
  id: string
  title: string
  hook: string
  body: string
  keyTakeaways: string[]
  callToAction: string
  fullText: string
  qualityScore: number | null
  qualityFeedback: Record<string, unknown> | null
  status: "draft" | "approved" | "rejected" | "scheduled" | "published"
  rejectionReason: string | null
  wordCount: number | null
  topicId: string | null
  researchId: string | null
  createdAt: string
  updatedAt: string
}

export interface ScheduledPost {
  id: string
  postId: string
  imageId: string | null
  scheduledFor: string
  status: "queued" | "processing" | "published" | "failed"
  retryCount: number
  lastError: string | null
  createdAt: string
}

export interface Analytics {
  id: string
  publishedPostId: string
  impressions: number
  likes: number
  comments: number
  shares: number
  engagementRate: number
  fetchedAt: string
}

export interface AgentLog {
  id: string
  userId: string
  agentName: string
  runId: string
  status: "running" | "success" | "failed"
  inputSummary: string | null
  outputSummary: string | null
  errorMessage: string | null
  durationMs: number | null
  startedAt: string
  finishedAt: string | null
}

export interface DashboardSummary {
  totalPosts: number
  publishedPosts: number
  scheduledPosts: number
  totalTopics: number
  avgEngagementRate: number
  geminiUsageToday: number
  geminiDailyLimit: number
  recentActivity: Record<string, unknown>
}

export interface GeminiUsage {
  todayCount: number
  dailyLimit: number
  monthlyCount: number
  platformDailyLimit: number
  asOf: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
