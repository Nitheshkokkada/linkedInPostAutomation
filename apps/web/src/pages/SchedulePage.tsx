import { useQuery } from "@tanstack/react-query"
import api from "@/lib/axios"
import type { ScheduledPost } from "@/types/api"
import { Calendar, Clock, AlertCircle } from "lucide-react"

const statusColors: Record<string, string> = {
  queued: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300",
  processing: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300",
  published: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300",
  failed: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300",
}

export default function SchedulePage() {
  const { data: scheduledPosts, isLoading } = useQuery<ScheduledPost[]>({
    queryKey: ["schedule"],
    queryFn: () => api.get("/schedule").then((r) => r.data),
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Schedule</h1>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : (
        <div className="space-y-4">
          {scheduledPosts?.map((post) => (
            <div key={post.id} className="rounded-lg border bg-card p-4 shadow-sm">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Calendar className="h-5 w-5 text-muted-foreground" />
                  <div>
                    <p className="text-sm font-medium">
                      Post {post.postId.slice(0, 8)}...
                    </p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <Clock className="h-3 w-3" />
                      {new Date(post.scheduledFor).toLocaleString()}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {post.lastError && (
                    <AlertCircle className="h-4 w-4 text-destructive" title={post.lastError} />
                  )}
                  <span
                    className={`rounded-full px-2 py-1 text-xs font-medium ${
                      statusColors[post.status] ?? ""
                    }`}
                  >
                    {post.status}
                  </span>
                </div>
              </div>
            </div>
          ))}
          {scheduledPosts?.length === 0 && (
            <div className="py-12 text-center text-muted-foreground">
              <Calendar className="mx-auto mb-4 h-12 w-12 opacity-50" />
              <p>No scheduled posts</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
