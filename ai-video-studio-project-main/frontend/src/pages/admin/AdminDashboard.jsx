import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import adminService from '../../services/adminService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import { format } from '../../utils/formatters'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    adminService.dashboard().then((res) => setStats(res.data))
  }, [])

  if (!stats) return <LoadingSpinner center size="lg" />

  const diamondFlowData = [
    { name: 'Purchased', value: stats.diamondsPurchased },
    { name: 'Used', value: stats.diamondsUsed },
    { name: 'Free (rewards)', value: stats.freeDiamondsDistributed },
  ]

  const cards = [
    { label: 'Total users', value: stats.totalUsers },
    { label: 'Active users', value: stats.activeUsers },
    { label: 'New users (7d)', value: stats.newUsersLast7Days },
    { label: 'Total videos', value: stats.totalVideos },
    { label: 'Completed videos', value: stats.completedVideos },
    { label: 'Failed videos', value: stats.failedVideos },
    { label: 'Blocked requests', value: stats.blockedModerationRequests },
    { label: 'Ad rewards claimed', value: stats.adRewardsClaimed },
    { label: 'Payment revenue', value: format.currency(stats.totalPaymentRevenue) },
  ]

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Admin dashboard</h1>
          <p className="page-subtitle">Platform-wide activity at a glance.</p>
        </div>
      </div>

      <div className="stat-grid">
        {cards.map((c) => (
          <div className="stat-card" key={c.label}>
            <div className="stat-label">{c.label}</div>
            <div className="stat-value">{c.value}</div>
          </div>
        ))}
      </div>

      <div className="card" style={{ marginTop: 10 }}>
        <h3 className="mt-0">Diamond flow</h3>
        <div style={{ width: '100%', height: 260 }}>
          <ResponsiveContainer>
            <BarChart data={diamondFlowData}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--line)" />
              <XAxis dataKey="name" stroke="var(--ink-faint)" fontSize={12} />
              <YAxis stroke="var(--ink-faint)" fontSize={12} />
              <Tooltip />
              <Bar dataKey="value" fill="var(--facet)" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}
