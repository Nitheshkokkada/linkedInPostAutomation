import { useQuery } from "@tanstack/react-query"
import api from "@/lib/axios"
import type { DashboardSummary, GeminiUsage } from "@/types/api"
import StatCard from "@/components/ui/StatCard"
import GeminiUsageBar from "@/components/dashboard/GeminiUsageBar"
import { FileText, Calendar, BarChart3, Target } from "lucide-react"

export default function DashboardPage() {
  const { data: summary } = useQuery<DashboardSummary>({
    queryKey: ["dashboard"],
    queryFn: () => api.get("/dashboard").then((r) => r.data),
  })

  const { data: usage } = useQuery<GeminiUsage>({
    queryKey: ["gemini-usage"],
    queryFn: () => api.get("/usage/gemini").then((r) => r.data),
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Dashboard</h1>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Posts"
          value={summary?.totalPosts ?? 0}
          description={`${summary?.publishedPosts ?? 0} published`}
          icon={<FileText className="h-5 w-5" />}
        />
        <StatCard
          title="Scheduled"
          value={summary?.scheduledPosts ?? 0}
          description="Queued for publishing"
          icon={<Calendar className="h-5 w-5" />}
        />
        <StatCard
          title="Engagement Rate"
          value={`${(summary?.avgEngagementRate ?? 0).toFixed(1)}%`}
          description="Average across all posts"
          icon={<BarChart3 className="h-5 w-5" />}
        />
        <StatCard
          title="Active Topics"
          value={summary?.totalTopics ?? 0}
          description="Research topics configured"
          icon={<Target className="h-5 w-5" />}
        />
      </div>

      {usage && (
        <GeminiUsageBar used={usage.todayCount} limit={usage.dailyLimit} />
      )}
    </div>
  )
}
