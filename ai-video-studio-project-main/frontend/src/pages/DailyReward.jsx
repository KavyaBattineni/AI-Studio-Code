import { useEffect, useState, useCallback } from 'react'
import AdReward from '../components/rewards/AdReward'
import LoadingSpinner from '../components/common/LoadingSpinner'
import rewardService from '../services/rewardService'

export default function DailyReward() {
  const [status, setStatus] = useState(null)

  const load = useCallback(() => {
    rewardService.status().then((res) => setStatus(res.data))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Daily reward</h1>
          <p className="page-subtitle">Watch a short ad once a day for free diamonds.</p>
        </div>
      </div>

      {!status ? <LoadingSpinner center size="lg" /> : <AdReward status={status} onClaimed={load} />}
    </div>
  )
}
