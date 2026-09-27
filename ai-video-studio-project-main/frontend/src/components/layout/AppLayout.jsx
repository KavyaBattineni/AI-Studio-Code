import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  Sparkles,
  Clapperboard,
  WalletMinimal,
  Gem,
  Gift,
  Receipt,
  UserRound,
} from 'lucide-react'
import Sidebar from './Sidebar'
import Navbar from './Navbar'
import { WalletProvider } from '../../context/WalletContext'

const NAV = [
  {
    items: [
      { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, end: true },
      { to: '/generate', label: 'Generate', icon: Sparkles },
      { to: '/my-videos', label: 'My Videos', icon: Clapperboard },
    ],
  },
  {
    label: 'Diamonds',
    items: [
      { to: '/wallet', label: 'Wallet', icon: WalletMinimal },
      { to: '/buy-diamonds', label: 'Buy Diamonds', icon: Gem },
      { to: '/daily-reward', label: 'Daily Reward', icon: Gift },
      { to: '/transactions', label: 'Transactions', icon: Receipt },
    ],
  },
  {
    label: 'Account',
    items: [{ to: '/profile', label: 'Profile', icon: UserRound }],
  },
]

const TITLES = {
  '/dashboard': 'Dashboard',
  '/generate': 'Generate a video',
  '/my-videos': 'My videos',
  '/wallet': 'Wallet',
  '/buy-diamonds': 'Buy diamonds',
  '/daily-reward': 'Daily reward',
  '/transactions': 'Transactions',
  '/profile': 'Profile',
}

export default function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const title = TITLES[location.pathname] || 'AI Video Studio'

  return (
    <WalletProvider>
      <div className="app-shell">
        <Sidebar links={NAV} open={sidebarOpen} onNavigate={() => setSidebarOpen(false)} />
        {sidebarOpen && <div className="sidebar-backdrop" onClick={() => setSidebarOpen(false)} />}
        <div className="main-area">
          <Navbar title={title} onMenuClick={() => setSidebarOpen((o) => !o)} />
          <main className="page-content">
            <Outlet />
          </main>
        </div>
      </div>
    </WalletProvider>
  )
}
