import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Menu, Gem, ChevronDown, LogOut, User as UserIcon } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { useWallet } from '../../context/WalletContext'

export default function Navbar({ title, onMenuClick, showBalance = true }) {
  const { user, logout } = useAuth()
  const { balance } = useWallet()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef(null)

  useEffect(() => {
    function handleClickOutside(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const initials = user?.name
    ?.split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  return (
    <header className="topbar">
      <div className="flex" style={{ gap: 12, alignItems: 'center' }}>
        <button className="topbar-menu-btn" onClick={onMenuClick} aria-label="Toggle menu">
          <Menu size={22} />
        </button>
        <div className="topbar-title">{title}</div>
      </div>

      <div className="topbar-right">
        {showBalance && (
          <button
            className="diamond-pill"
            style={{ border: 'none', cursor: 'pointer' }}
            onClick={() => navigate('/buy-diamonds')}
            title="Buy more diamonds"
          >
            <Gem /> {balance ?? '—'}
          </button>
        )}

        <div style={{ position: 'relative' }} ref={menuRef}>
          <button className="user-menu-btn" onClick={() => setMenuOpen((o) => !o)}>
            <span className="user-avatar">{initials || <UserIcon size={14} />}</span>
            <ChevronDown size={15} />
          </button>

          {menuOpen && (
            <div
              className="card"
              style={{
                position: 'absolute',
                right: 0,
                top: 44,
                minWidth: 190,
                padding: 8,
                zIndex: 50,
                boxShadow: 'var(--shadow-md)',
              }}
            >
              <div style={{ padding: '8px 10px', borderBottom: '1px solid var(--line)', marginBottom: 6 }}>
                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{user?.name}</div>
                <div className="text-muted" style={{ fontSize: '0.78rem' }}>
                  {user?.email}
                </div>
              </div>
              <button
                className="dropdown-item"
                onClick={() => {
                  setMenuOpen(false)
                  navigate('/profile')
                }}
              >
                <UserIcon size={16} /> Profile
              </button>
              <button
                className="dropdown-item"
                style={{ color: 'var(--danger)' }}
                onClick={() => {
                  logout()
                  navigate('/login')
                }}
              >
                <LogOut size={16} /> Log out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
