import { useQuery } from "@tanstack/react-query"
import api from "@/lib/axios"
import type { Analytics } from "@/types/api"
import { BarChart3, TrendingUp } from "lucide-react"
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts"

export default function AnalyticsPage() {
  const { data: analytics, isLoading } = useQuery<Analytics[]>({
    queryKey: ["analytics"],
    queryFn: () => api.get("/analytics").then((r) => r.data),
  })

  const { data: engagementData } = useQuery<{ avgEngagementRate: number; hasData: boolean }>({
    queryKey: ["engagement-rate"],
    queryFn: () => api.get("/analytics/engagement-rate").then((r) => r.data),
  })

  const chartData =
    analytics?.map((a) => ({
      date: new Date(a.fetchedAt).toLocaleDateString(),
      impressions: a.impressions,
      likes: a.likes,
      comments: a.comments,
      engagement: a.engagementRate,
    })) ?? []

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Analytics</h1>

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-2 text-muted-foreground">
            <BarChart3 className="h-5 w-5" />
            <p className="text-sm font-medium">Avg Engagement</p>
          </div>
          <p className="mt-2 text-3xl font-bold">
            {engagementData?.hasData
              ? `${engagementData.avgEngagementRate.toFixed(1)}%`
              : "N/A"}
          </p>
        </div>
        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-2 text-muted-foreground">
            <TrendingUp className="h-5 w-5" />
            <p className="text-sm font-medium">Total Impressions</p>
          </div>
          <p className="mt-2 text-3xl font-bold">
            {analytics?.reduce((sum, a) => sum + a.impressions, 0).toLocaleString() ?? 0}
          </p>
        </div>
        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-2 text-muted-foreground">
            <TrendingUp className="h-5 w-5" />
            <p className="text-sm font-medium">Total Likes</p>
          </div>
          <p className="mt-2 text-3xl font-bold">
            {analytics?.reduce((sum, a) => sum + a.likes, 0).toLocaleString() ?? 0}
          </p>
        </div>
      </div>

      {chartData.length > 0 && (
        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-semibold">Engagement Over Time</h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Line type="monotone" dataKey="engagement" stroke="#3b82f6" name="Engagement %" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : analytics?.length === 0 ? (
        <div className="py-12 text-center text-muted-foreground">
          <BarChart3 className="mx-auto mb-4 h-12 w-12 opacity-50" />
          <p>No analytics data yet. Analytics are collected after posts are published.</p>
        </div>
      ) : (
        <div className="rounded-lg border bg-card shadow-sm">
          <div className="border-b p-4">
            <h2 className="text-lg font-semibold">Recent Analytics</h2>
          </div>
          <div className="divide-y">
            {analytics?.slice(0, 10).map((a) => (
              <div key={a.id} className="flex items-center justify-between p-4">
                <div className="text-sm text-muted-foreground">
                  {new Date(a.fetchedAt).toLocaleString()}
                </div>
                <div className="flex gap-6 text-sm">
                  <span>{a.impressions} impressions</span>
                  <span>{a.likes} likes</span>
                  <span>{a.comments} comments</span>
                  <span>{a.shares} shares</span>
                  <span className="font-medium">{a.engagementRate.toFixed(1)}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
