import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { UserPlus } from 'lucide-react'
import authService from '../../services/authService'
import ErrorMessage from '../../components/common/ErrorMessage'
import AuthHero from './AuthHero'

export default function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', mobile: '', password: '', confirmPassword: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setSubmitting(true)
    try {
      await authService.register(form)
      navigate('/verify-otp', { state: { email: form.email } })
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to create your account. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-shell">
      <AuthHero
        title="Your first video is a few clicks away."
        subtitle="Create an account, verify your email, and claim your first daily diamonds."
      />
      <div className="auth-form-panel">
        <div className="auth-form-box">
          <div className="auth-brand">
            <span className="brand-mark" /> AI Video Studio
          </div>
          <h1>Create your account</h1>
          <p className="text-muted">Start generating AI videos in minutes.</p>

          <ErrorMessage message={error} />

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="label" htmlFor="name">
                Full name
              </label>
              <input id="name" className="input" required value={form.name} onChange={(e) => update('name', e.target.value)} />
            </div>
            <div className="form-group">
              <label className="label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                className="input"
                required
                value={form.email}
                onChange={(e) => update('email', e.target.value)}
              />
            </div>
            <div className="form-group">
              <label className="label" htmlFor="mobile">
                Mobile number
              </label>
              <input
                id="mobile"
                className="input"
                required
                pattern="[0-9]{10,15}"
                title="10 to 15 digits"
                value={form.mobile}
                onChange={(e) => update('mobile', e.target.value)}
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="label" htmlFor="password">
                  Password
                </label>
                <input
                  id="password"
                  type="password"
                  className="input"
                  required
                  minLength={8}
                  value={form.password}
                  onChange={(e) => update('password', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="label" htmlFor="confirmPassword">
                  Confirm password
                </label>
                <input
                  id="confirmPassword"
                  type="password"
                  className="input"
                  required
                  minLength={8}
                  value={form.confirmPassword}
                  onChange={(e) => update('confirmPassword', e.target.value)}
                />
              </div>
            </div>
            <p className="field-hint" style={{ marginTop: -8, marginBottom: 18 }}>
              At least 8 characters, with a letter and a number.
            </p>

            <button type="submit" className="btn btn-accent btn-block" disabled={submitting}>
              <UserPlus size={16} /> {submitting ? 'Creating account...' : 'Create Account'}
            </button>
          </form>

          <p className="auth-footer-note">
            Already have an account? <Link to="/login">Log in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
