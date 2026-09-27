import { Link } from 'react-router-dom'
import { Compass } from 'lucide-react'

export default function NotFound() {
  return (
    <div className="loading-center" style={{ flexDirection: 'column', gap: 14, minHeight: '100vh' }}>
      <Compass size={40} color="var(--line)" />
      <h2 className="mt-0">Page not found</h2>
      <Link to="/dashboard" className="btn btn-accent">
        Back to dashboard
      </Link>
    </div>
  )
}
