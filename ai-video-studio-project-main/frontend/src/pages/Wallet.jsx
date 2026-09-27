import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Gem, Receipt, ArrowRight } from 'lucide-react'
import { useWallet } from '../context/WalletContext'
import walletService from '../services/walletService'
import TransactionTable from '../components/wallet/TransactionTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import EmptyState from '../components/common/EmptyState'

export default function Wallet() {
  const { balance } = useWallet()
  const [recent, setRecent] = useState(null)

  useEffect(() => {
    walletService.getTransactions(0, 10).then((res) => setRecent(res.data.content))
  }, [])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Wallet</h1>
          <p className="page-subtitle">Your diamond balance at a glance.</p>
        </div>
        <Link to="/buy-diamonds" className="btn btn-accent">
          <Gem size={16} /> Buy diamonds
        </Link>
      </div>

      <div className="stat-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 260px))', marginBottom: 30 }}>
        <div className="stat-card">
          <div className="stat-label">Current balance</div>
          <div className="stat-value">{balance ?? '—'} 💎</div>
        </div>
      </div>

      <div className="section-heading" style={{ marginTop: 0 }}>
        <h2>Recent activity</h2>
        <Link to="/transactions" style={{ fontSize: '0.88rem', display: 'flex', alignItems: 'center', gap: 4 }}>
          View all <ArrowRight size={14} />
        </Link>
      </div>

      {!recent ? (
        <LoadingSpinner center />
      ) : recent.length === 0 ? (
        <EmptyState icon={Receipt} title="No transactions yet" />
      ) : (
        <TransactionTable transactions={recent} />
      )}
    </div>
  )
}
