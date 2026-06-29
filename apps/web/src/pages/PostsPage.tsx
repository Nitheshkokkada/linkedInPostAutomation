import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import api from "@/lib/axios"
import type { Post, PaginatedResponse } from "@/types/api"
import { FileText, CheckCircle, XCircle, Clock } from "lucide-react"

const statusColors: Record<string, string> = {
  draft: "bg-secondary text-secondary-foreground",
  approved: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300",
  rejected: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300",
  scheduled: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300",
  published: "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300",
}

export default function PostsPage() {
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<string>("")
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery<PaginatedResponse<Post>>({
    queryKey: ["posts", page, statusFilter],
    queryFn: () => {
      const url = statusFilter
        ? `/posts/status/${statusFilter}?page=${page}&size=10`
        : `/posts?page=${page}&size=10`
      return api.get(url).then((r) => r.data)
    },
  })

  const approveMutation = useMutation({
    mutationFn: (postId: string) => api.post(`/posts/${postId}/approve`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["posts"] }),
  })

  const rejectMutation = useMutation({
    mutationFn: (postId: string) => api.post(`/posts/${postId}/reject`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["posts"] }),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Posts</h1>
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value)
            setPage(0)
          }}
          className="rounded-lg border bg-input px-3 py-2 text-sm"
        >
          <option value="">All Status</option>
          <option value="draft">Draft</option>
          <option value="approved">Approved</option>
          <option value="rejected">Rejected</option>
          <option value="scheduled">Scheduled</option>
          <option value="published">Published</option>
        </select>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : (
        <div className="space-y-4">
          {data?.content.map((post) => (
            <div key={post.id} className="rounded-lg border bg-card p-4 shadow-sm">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <h3 className="font-semibold">{post.title || "Untitled Post"}</h3>
                  <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">
                    {post.fullText || post.hook || "No content"}
                  </p>
                  <div className="mt-2 flex items-center gap-4 text-xs text-muted-foreground">
                    <span>{post.wordCount ?? 0} words</span>
                    {post.qualityScore != null && (
                      <span>Score: {post.qualityScore}/100</span>
                    )}
                    <span>
                      {new Date(post.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={`rounded-full px-2 py-1 text-xs font-medium ${
                      statusColors[post.status] ?? ""
                    }`}
                  >
                    {post.status}
                  </span>
                  {post.status === "draft" && (
                    <div className="flex gap-1">
                      <button
                        onClick={() => approveMutation.mutate(post.id)}
                        className="rounded p-1 text-green-600 hover:bg-green-50"
                        title="Approve"
                      >
                        <CheckCircle className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => rejectMutation.mutate(post.id)}
                        className="rounded p-1 text-red-600 hover:bg-red-50"
                        title="Reject"
                      >
                        <XCircle className="h-4 w-4" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
          {data?.content.length === 0 && (
            <div className="py-12 text-center text-muted-foreground">
              <FileText className="mx-auto mb-4 h-12 w-12 opacity-50" />
              <p>No posts found</p>
            </div>
          )}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border px-3 py-1 text-sm disabled:opacity-50"
          >
            Previous
          </button>
          <span className="px-3 py-1 text-sm text-muted-foreground">
            Page {page + 1} of {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border px-3 py-1 text-sm disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
