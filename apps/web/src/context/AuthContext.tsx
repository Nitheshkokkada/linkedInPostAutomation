import { createContext, useContext, useState, useEffect, useCallback } from "react"
import type { User } from "@/types/api"
import api, { setAccessToken } from "@/lib/axios"

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, fullName: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const checkAuth = useCallback(async () => {
    try {
      const { data } = await api.get("/me")
      setUser(data)
    } catch {
      setUser(null)
      setAccessToken(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  const login = async (email: string, password: string) => {
    const { data } = await api.post("/auth/login", { email, password })
    setAccessToken(data.accessToken)
    setUser(data.user)
  }

  const register = async (email: string, password: string, fullName: string) => {
    const { data } = await api.post("/auth/register", { email, password, fullName })
    setAccessToken(data.accessToken)
    setUser(data.user)
  }

  const logout = async () => {
    await api.post("/auth/logout")
    setAccessToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
