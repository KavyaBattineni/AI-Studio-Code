import { NavLink } from 'react-router-dom'
import { Shield } from 'lucide-react'

export default function Sidebar({ links, isAdmin, open, onNavigate }) {
  return (
    <aside className={`sidebar ${open ? 'open' : ''}`}>
      <div className="sidebar-brand">
        <span className="brand-mark" />
        AI Video Studio
      </div>

      {isAdmin && (
        <span className="sidebar-admin-tag">
          <Shield size={12} /> Admin console
        </span>
      )}

      <nav className="sidebar-nav">
        {links.map((section, idx) => (
          <div key={idx}>
            {section.label && <div className="sidebar-section-label">{section.label}</div>}
            {section.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                onClick={onNavigate}
                className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
              >
                <item.icon />
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
    </aside>
  )
}
