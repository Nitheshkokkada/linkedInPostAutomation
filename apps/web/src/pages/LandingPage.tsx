import { Link } from "react-router-dom"
import { Bot, ArrowRight } from "lucide-react"

export default function LandingPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="flex items-center justify-between border-b px-6 py-4">
        <div className="flex items-center gap-2">
          <Bot className="h-8 w-8 text-primary" />
          <span className="text-xl font-bold">LinkedIn AI Agent</span>
        </div>
        <div className="flex gap-4">
          <Link
            to="/login"
            className="rounded-lg px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground"
          >
            Login
          </Link>
          <Link
            to="/register"
            className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            Get Started
          </Link>
        </div>
      </header>

      <main className="flex flex-1 items-center justify-center px-6">
        <div className="max-w-2xl text-center">
          <h1 className="text-5xl font-bold tracking-tight">
            Automate Your LinkedIn Presence
          </h1>
          <p className="mt-6 text-lg text-muted-foreground">
            Multi-agent AI system that researches topics, creates engaging LinkedIn posts
            with images, schedules, publishes, and learns from analytics — all on free-tier services.
          </p>
          <div className="mt-8 flex items-center justify-center gap-4">
            <Link
              to="/register"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-base font-medium text-primary-foreground hover:bg-primary/90"
            >
              Start Free <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
          <div className="mt-16 grid grid-cols-2 gap-6 text-left md:grid-cols-4">
            {[
              { title: "Research", desc: "AI-powered topic research" },
              { title: "Create", desc: "Engaging post generation" },
              { title: "Schedule", desc: "Optimal time publishing" },
              { title: "Learn", desc: "Self-improving patterns" },
            ].map((item) => (
              <div key={item.title} className="rounded-lg border p-4">
                <h3 className="font-semibold">{item.title}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </main>

      <footer className="border-t px-6 py-4 text-center text-sm text-muted-foreground">
        LinkedIn AI Agent — Built with Spring Boot, React, and Gemini AI
      </footer>
    </div>
  )
}
