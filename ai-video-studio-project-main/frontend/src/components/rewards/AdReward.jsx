import { useState } from 'react'
import { Gift, Loader2 } from 'lucide-react'
import rewardService from '../../services/rewardService'
import { useWallet } from '../../context/WalletContext'
import { useToast } from '../../context/ToastContext'
import ErrorMessage from '../common/ErrorMessage'

const SIMULATED_AD_SECONDS = 4

export default function AdReward({ status, onClaimed }) {
  const { refreshBalance } = useWallet()
  const toast = useToast()
  const [watching, setWatching] = useState(false)
  const [countdown, setCountdown] = useState(SIMULATED_AD_SECONDS)
  const [error, setError] = useState('')

  function watchAd() {
    setError('')
    setWatching(true)
    setCountdown(SIMULATED_AD_SECONDS)

    const timer = setInterval(() => {
      setCountdown((c) => {
        if (c <= 1) {
          clearInterval(timer)
          completeAdWatch()
          return 0
        }
        return c - 1
      })
    }, 1000)
  }

  async function completeAdWatch() {
    try {
      // In production this verificationId comes from the ad network's rewarded-ad SDK
      // callback, not the client generating it itself - see MockAdProvider's class comment.
      const verificationId = `mock-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
      const res = await rewardService.verifyAd({ provider: 'MOCK_AD_NETWORK', verificationId })
      toast.success(`+${res.data.diamondsCredited} diamonds credited!`)
      await refreshBalance()
      onClaimed?.()
    } catch (err) {
      setError(err.response?.data?.message || 'We could not verify the ad. Please try again.')
    } finally {
      setWatching(false)
    }
  }

  if (status?.claimedToday) {
    return (
      <div className="reward-card card">
        <div className="reward-diamond-icon">
          <Gift />
        </div>
        <h3>Today&apos;s reward already claimed</h3>
        <p className="text-muted">Come back tomorrow for {status.diamondsAvailableToday} more diamonds.</p>
      </div>
    )
  }

  return (
    <div className="reward-card card">
      <div className="reward-diamond-icon">
        <Gift />
      </div>
      <h3>Earn up to {status?.diamondsAvailableToday ?? 3} diamonds today</h3>
      <p className="text-muted">Watch a short rewarded ad to claim your free daily diamonds.</p>

      <ErrorMessage message={error} />

      <button className="btn btn-accent" onClick={watchAd} disabled={watching}>
        {watching ? (
          <>
            <Loader2 size={16} className="spin-icon" /> Playing ad... {countdown}s
          </>
        ) : (
          'Watch Rewarded Ad'
        )}
      </button>
    </div>
  )
}
