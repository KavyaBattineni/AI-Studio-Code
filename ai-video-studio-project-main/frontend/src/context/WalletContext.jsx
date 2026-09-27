import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import walletService from '../services/walletService'
import { useAuth } from './AuthContext'

const WalletContext = createContext(null)

export function WalletProvider({ children }) {
  const { isAuthenticated } = useAuth()
  const [balance, setBalance] = useState(null)
  const [loading, setLoading] = useState(false)

  const refreshBalance = useCallback(async () => {
    if (!isAuthenticated) return
    setLoading(true)
    try {
      const res = await walletService.getWallet()
      setBalance(res.data.diamondBalance)
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated])

  useEffect(() => {
    if (isAuthenticated) {
      refreshBalance()
    } else {
      setBalance(null)
    }
  }, [isAuthenticated, refreshBalance])

  return (
    <WalletContext.Provider value={{ balance, loading, refreshBalance }}>{children}</WalletContext.Provider>
  )
}

export function useWallet() {
  const ctx = useContext(WalletContext)
  if (!ctx) throw new Error('useWallet must be used within a WalletProvider')
  return ctx
}
