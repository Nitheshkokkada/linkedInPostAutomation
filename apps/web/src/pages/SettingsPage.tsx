import { useState, useEffect } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import api from "@/lib/axios"
import { useAuth } from "@/context/AuthContext"
import { Settings, Save, Link2, Unlink } from "lucide-react"

export default function SettingsPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const [fullName, setFullName] = useState("")
  const [timezone, setTimezone] = useState("UTC")
  const [postingMode, setPostingMode] = useState("draft")
  const [saved, setSaved] = useState(false)

  const { data: settings } = useQuery({
    queryKey: ["settings"],
    queryFn: () => api.get("/settings").then((r) => r.data),
  })

  useEffect(() => {
    if (settings) {
      setFullName(settings.fullName || "")
      setTimezone(settings.timezone || "UTC")
      setPostingMode(settings.postingMode || "draft")
    }
  }, [settings])

  const updateMutation = useMutation({
    mutationFn: () =>
      api.patch("/settings", { fullName, timezone, postingMode }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["settings"] })
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    },
  })

  const timezones = [
    "UTC",
    "America/New_York",
    "America/Chicago",
    "America/Denver",
    "America/Los_Angeles",
    "Europe/London",
    "Europe/Berlin",
    "Asia/Tokyo",
    "Asia/Shanghai",
    "Australia/Sydney",
  ]

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Settings</h1>

      <div className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <Settings className="h-5 w-5 text-muted-foreground" />
          <h2 className="text-lg font-semibold">Profile</h2>
        </div>

        <div className="space-y-4">
          <div>
            <label className="text-sm font-medium">Full Name</label>
            <input
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="mt-1 w-full rounded-lg border bg-input px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div>
            <label className="text-sm font-medium">Email</label>
            <input
              type="email"
              value={user?.email || ""}
              disabled
              className="mt-1 w-full rounded-lg border bg-input px-3 py-2 text-sm opacity-50"
            />
          </div>

          <div>
            <label className="text-sm font-medium">Timezone</label>
            <select
              value={timezone}
              onChange={(e) => setTimezone(e.target.value)}
              className="mt-1 w-full rounded-lg border bg-input px-3 py-2 text-sm"
            >
              {timezones.map((tz) => (
                <option key={tz} value={tz}>
                  {tz}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-sm font-medium">Posting Mode</label>
            <select
              value={postingMode}
              onChange={(e) => setPostingMode(e.target.value)}
              className="mt-1 w-full rounded-lg border bg-input px-3 py-2 text-sm"
            >
              <option value="draft">Draft (save locally only)</option>
              <option value="manual">Manual (email approval required)</option>
              <option value="auto">Auto (publish immediately)</option>
            </select>
          </div>

          <button
            onClick={() => updateMutation.mutate()}
            disabled={updateMutation.isPending}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            <Save className="h-4 w-4" />
            {saved ? "Saved!" : updateMutation.isPending ? "Saving..." : "Save Changes"}
          </button>
        </div>
      </div>

      <div className="rounded-lg border bg-card p-6 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <Link2 className="h-5 w-5 text-muted-foreground" />
          <h2 className="text-lg font-semibold">LinkedIn Connection</h2>
        </div>

        <div className="flex items-center gap-4">
          {user?.linkedinConnected ? (
            <>
              <span className="text-sm text-green-600">Connected</span>
              <a
                href="/api/v1/auth/linkedin/disconnect"
                className="inline-flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/10"
              >
                <Unlink className="h-4 w-4" />
                Disconnect
              </a>
            </>
          ) : (
            <a
              href="/api/v1/auth/linkedin/connect"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <Link2 className="h-4 w-4" />
              Connect LinkedIn
            </a>
          )}
        </div>
      </div>
    </div>
  )
}
