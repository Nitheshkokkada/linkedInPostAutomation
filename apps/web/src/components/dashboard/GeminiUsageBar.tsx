interface GeminiUsageBarProps {
  used: number
  limit: number
}

export default function GeminiUsageBar({ used, limit }: GeminiUsageBarProps) {
  const percentage = Math.min((used / limit) * 100, 100)
  const isWarning = percentage > 80
  const isDanger = percentage > 95

  return (
    <div className="rounded-lg border bg-card p-6 shadow-sm">
      <div className="flex items-center justify-between mb-2">
        <p className="text-sm font-medium text-muted-foreground">Gemini API Usage</p>
        <span className="text-sm text-muted-foreground">
          {used} / {limit} calls today
        </span>
      </div>
      <div className="h-3 w-full overflow-hidden rounded-full bg-secondary">
        <div
          className={`h-full rounded-full transition-all ${
            isDanger
              ? "bg-destructive"
              : isWarning
              ? "bg-yellow-500"
              : "bg-primary"
          }`}
          style={{ width: `${percentage}%` }}
        />
      </div>
      <p className="mt-2 text-xs text-muted-foreground">
        {isDanger
          ? "Daily limit nearly reached. New content generation may be paused."
          : isWarning
          ? "Approaching daily limit."
          : "Usage within normal range."}
      </p>
    </div>
  )
}
