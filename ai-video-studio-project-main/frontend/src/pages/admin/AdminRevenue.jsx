import { useEffect, useState } from 'react'
import adminService from '../../services/adminService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import { format } from '../../utils/formatters'

export default function AdminRevenue() {
  const [revenue, setRevenue] = useState(null)

  useEffect(() => {
    adminService.revenue().then((res) => setRevenue(res.data))
  }, [])

  if (!revenue) return <LoadingSpinner center size="lg" />

  return (
    <div>
      <div className="page-header">
        <h1>Revenue</h1>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="stat-label">Application payment revenue</div>
        <div className="stat-value" style={{ fontSize: '2.2rem' }}>
          {format.currency(revenue.applicationPaymentRevenue)}
        </div>
        <p className="text-muted" style={{ marginTop: 10, marginBottom: 0 }}>
          {revenue.note}
        </p>
      </div>

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-label">Successful payments</div>
          <div className="stat-value">{revenue.successfulPayments}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Failed payments</div>
          <div className="stat-value">{revenue.failedPayments}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Refunded payments</div>
          <div className="stat-value">{revenue.refundedPayments}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Diamonds purchased</div>
          <div className="stat-value">{revenue.diamondsPurchased}</div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 20, background: 'var(--paper-dim)', border: 'none' }}>
        <h3 className="mt-0" style={{ fontSize: '1rem' }}>
          Ad network publisher revenue
        </h3>
        <p className="text-muted" style={{ marginBottom: 0 }}>
          Not shown here. The ad network calculates and reports publisher earnings separately according to its own
          policies and reporting tools - this platform only tracks diamond rewards granted to users, which are a
          fixed 3 diamonds/day and are not tied to any specific ad-revenue figure.
        </p>
      </div>
    </div>
  )
}
