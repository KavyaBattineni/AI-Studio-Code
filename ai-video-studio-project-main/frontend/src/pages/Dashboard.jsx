import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Sparkles, Gem, Gift, Clapperboard, Receipt } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useWallet } from '../context/WalletContext'
import videoService from '../services/videoService'
import walletService from '../services/walletService'
import VideoCard from '../components/video/VideoCard'
import TransactionTable from '../components/wallet/TransactionTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import EmptyState from '../components/common/EmptyState'

const QUICK_ACTIONS = [
  { to: '/generate', icon: Sparkles, title: 'Generate Video', desc: 'Turn a prompt into a video' },
  { to: '/buy-diamonds', icon: Gem, title: 'Buy Diamonds', desc: 'Top up your balance' },
  { to: '/daily-reward', icon: Gift, title: 'Daily Reward', desc: 'Claim free diamonds' },
]

export default function Dashboard() {
  const { user } = useAuth()
  const { balance } = useWallet()
  const [videos, setVideos] = useState(null)
  const [transactions, setTransactions] = useState(null)

  useEffect(() => {
    videoService.list(0, 4).then((res) => setVideos(res.data.content))
    walletService.getTransactions(0, 5).then((res) => setTransactions(res.data.content))
  }, [])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Welcome back, {user?.name?.split(' ')[0]}</h1>
          <p className="page-subtitle">Here&apos;s what&apos;s happening with your account.</p>
        </div>
        <div className="diamond-pill" style={{ fontSize: '1rem', padding: '10px 18px' }}>
          <Gem /> {balance ?? '—'} diamonds
        </div>
      </div>

      <div className="quick-actions-grid">
        {QUICK_ACTIONS.map((a) => (
          <Link key={a.to} to={a.to} className="quick-action-card">
            <div className="quick-action-icon">
              <a.icon />
            </div>
            <h3>{a.title}</h3>
            <p className="text-muted">{a.desc}</p>
          </Link>
        ))}
      </div>

      <div className="section-heading">
        <h2>Recent videos</h2>
        <Link to="/my-videos" style={{ fontSize: '0.88rem' }}>
          View all
        </Link>
      </div>
      {videos === null ? (
        <LoadingSpinner center />
      ) : videos.length === 0 ? (
        <EmptyState
          icon={Clapperboard}
          title="No videos yet"
          description="Generate your first AI video to see it here."
          action={
            <Link to="/generate" className="btn btn-accent btn-sm" style={{ marginTop: 10 }}>
              Generate a video
            </Link>
          }
        />
      ) : (
        <div className="video-grid">
          {videos.map((v) => (
            <VideoCard key={v.id} job={v} onDelete={() => videoService.remove(v.id).then(() => videoService.list(0, 4).then((r) => setVideos(r.data.content)))} />
          ))}
        </div>
      )}

      <div className="section-heading">
        <h2>Recent transactions</h2>
        <Link to="/transactions" style={{ fontSize: '0.88rem' }}>
          View all
        </Link>
      </div>
      {transactions === null ? (
        <LoadingSpinner center />
      ) : transactions.length === 0 ? (
        <EmptyState icon={Receipt} title="No transactions yet" />
      ) : (
        <TransactionTable transactions={transactions} />
      )}
    </div>
  )
}
