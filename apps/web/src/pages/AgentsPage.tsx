import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import api from "@/lib/axios"
import type { AgentLog, PaginatedResponse } from "@/types/api"
import { Bot, CheckCircle, XCircle, Clock } from "lucide-react"

const statusIcons: Record<string, React.ReactNode> = {
  running: <Clock className="h-4 w-4 text-yellow-500" />,
  success: <CheckCircle className="h-4 w-4 text-green-500" />,
  failed: <XCircle className="h-4 w-4 text-red-500" />,
}

const agentNames = [
  "TopicResearchAgent",
  "ContentCreationAgent",
  "ReviewAgent",
  "ImageAgent",
  "SchedulerAgent",
  "LinkedInPublishingAgent",
  "AnalyticsAgent",
  "SelfLearningAgent",
]

export default function AgentsPage() {
  const [page, setPage] = useState(0)
  const [agentFilter, setAgentFilter] = useState("")

  const { data, isLoading } = useQuery<PaginatedResponse<AgentLog>>({
    queryKey: ["agent-logs", page, agentFilter],
    queryFn: () => {
      const url = agentFilter
        ? `/agents/logs/agent/${agentFilter}?page=${page}&size=20`
        : `/agents/logs?page=${page}&size=20`
      return api.get(url).then((r) => r.data)
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Agent Monitoring</h1>
        <select
          value={agentFilter}
          onChange={(e) => {
            setAgentFilter(e.target.value)
            setPage(0)
          }}
          className="rounded-lg border bg-input px-3 py-2 text-sm"
        >
          <option value="">All Agents</option>
          {agentNames.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : (
        <div className="space-y-2">
          {data?.content.map((log) => (
            <div key={log.id} className="rounded-lg border bg-card p-4 shadow-sm">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  {statusIcons[log.status]}
                  <div>
                    <p className="text-sm font-medium">{log.agentName}</p>
                    <p className="text-xs text-muted-foreground">
                      {log.inputSummary}
                    </p>
                  </div>
                </div>
                <div className="text-right text-xs text-muted-foreground">
                  <p>{new Date(log.startedAt).toLocaleString()}</p>
                  {log.durationMs != null && (
                    <p>{(log.durationMs / 1000).toFixed(1)}s</p>
                  )}
                </div>
              </div>
              {log.outputSummary && (
                <p className="mt-2 text-xs text-muted-foreground">
                  {log.outputSummary}
                </p>
              )}
              {log.errorMessage && (
                <p className="mt-2 text-xs text-destructive">
                  {log.errorMessage}
                </p>
              )}
            </div>
          ))}
          {data?.content.length === 0 && (
            <div className="py-12 text-center text-muted-foreground">
              <Bot className="mx-auto mb-4 h-12 w-12 opacity-50" />
              <p>No agent logs found</p>
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
