import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import authService from '../services/authService'
import userService from '../services/userService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })
  const [isLoading, setIsLoading] = useState(true)

  const persistSession = useCallback((authResponse) => {
    localStorage.setItem('accessToken', authResponse.accessToken)
    localStorage.setItem('refreshToken', authResponse.refreshToken)
    localStorage.setItem('user', JSON.stringify(authResponse.user))
    setUser(authResponse.user)
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      setIsLoading(false)
      return
    }
    // Refresh the cached profile in the background so role/status changes made
    // by an admin (e.g. suspension) are picked up without waiting for re-login.
    userService
      .me()
      .then((res) => {
        setUser(res.data)
        localStorage.setItem('user', JSON.stringify(res.data))
      })
      .catch(() => {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        setUser(null)
      })
      .finally(() => setIsLoading(false))
  }, [])

  const login = useCallback(
    async (email, password) => {
      const res = await authService.login({ email, password })
      persistSession(res.data)
      return res.data.user
    },
    [persistSession],
  )

  const completeVerification = useCallback(
    (authResponse) => {
      persistSession(authResponse)
    },
    [persistSession],
  )

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    setUser(null)
  }, [])

  const refreshProfile = useCallback(async () => {
    const res = await userService.me()
    setUser(res.data)
    localStorage.setItem('user', JSON.stringify(res.data))
    return res.data
  }, [])

  const value = {
    user,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ADMIN',
    isLoading,
    login,
    logout,
    completeVerification,
    refreshProfile,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
