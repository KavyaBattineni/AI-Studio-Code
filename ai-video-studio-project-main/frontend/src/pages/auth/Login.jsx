import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import ErrorMessage from '../../components/common/ErrorMessage'
import AuthHero from './AuthHero'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(email, password)
      const redirectTo = location.state?.from?.pathname || '/dashboard'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to log in. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-shell">
      <AuthHero
        title="Turn a sentence into a scene."
        subtitle="Describe what you want to see, spend a few diamonds, and get a ready-to-share AI video in moments."
      />
      <div className="auth-form-panel">
        <div className="auth-form-box">
          <div className="auth-brand">
            <span className="brand-mark" /> AI Video Studio
          </div>
          <h1>Welcome back</h1>
          <p className="text-muted">Log in to keep creating.</p>

          <ErrorMessage message={error} />

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                className="input"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="form-group">
              <div className="flex-between">
                <label className="label" htmlFor="password">
                  Password
                </label>
                <Link to="/forgot-password" style={{ fontSize: '0.82rem' }}>
                  Forgot password?
                </Link>
              </div>
              <input
                id="password"
                type="password"
                className="input"
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <button type="submit" className="btn btn-accent btn-block" disabled={submitting}>
              <Sparkles size={16} /> {submitting ? 'Logging in...' : 'Log in'}
            </button>
          </form>

          <p className="auth-footer-note">
            New user? <Link to="/register">Create account</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
