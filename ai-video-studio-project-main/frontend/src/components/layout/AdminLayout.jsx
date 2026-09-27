import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  Users,
  CreditCard,
  Clapperboard,
  ShieldAlert,
  Receipt,
  TrendingUp,
} from 'lucide-react'
import Sidebar from './Sidebar'
import Navbar from './Navbar'
import { WalletProvider } from '../../context/WalletContext'

const NAV = [
  {
    label: 'Admin',
    items: [
      { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
      { to: '/admin/users', label: 'Users', icon: Users },
      { to: '/admin/payments', label: 'Payments', icon: CreditCard },
      { to: '/admin/videos', label: 'Videos', icon: Clapperboard },
      { to: '/admin/moderation', label: 'Moderation', icon: ShieldAlert },
      { to: '/admin/transactions', label: 'Transactions', icon: Receipt },
      { to: '/admin/revenue', label: 'Revenue', icon: TrendingUp },
    ],
  },
]

const TITLES = {
  '/admin': 'Admin dashboard',
  '/admin/users': 'Users',
  '/admin/payments': 'Payments',
  '/admin/videos': 'Videos',
  '/admin/moderation': 'Moderation',
  '/admin/transactions': 'Transactions',
  '/admin/revenue': 'Revenue',
}

export default function AdminLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const title = TITLES[location.pathname] || 'Admin'

  return (
    <WalletProvider>
      <div className="app-shell">
        <Sidebar links={NAV} isAdmin open={sidebarOpen} onNavigate={() => setSidebarOpen(false)} />
        {sidebarOpen && <div className="sidebar-backdrop" onClick={() => setSidebarOpen(false)} />}
        <div className="main-area">
          <Navbar title={title} onMenuClick={() => setSidebarOpen((o) => !o)} showBalance={false} />
          <main className="page-content">
            <Outlet />
          </main>
        </div>
      </div>
    </WalletProvider>
  )
}
