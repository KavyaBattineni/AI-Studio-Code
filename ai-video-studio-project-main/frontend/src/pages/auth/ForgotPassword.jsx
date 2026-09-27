import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { KeyRound } from 'lucide-react'
import authService from '../../services/authService'
import ErrorMessage from '../../components/common/ErrorMessage'
import AuthHero from './AuthHero'

export default function ForgotPassword() {
  const navigate = useNavigate()
  const [step, setStep] = useState('email') // 'email' | 'reset'
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleRequestCode(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const res = await authService.forgotPassword({ email })
      setInfo(res.message)
      setStep('reset')
    } catch (err) {
      setError(err.response?.data?.message || 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReset(e) {
    e.preventDefault()
    setError('')
    if (newPassword !== confirmNewPassword) {
      setError('Passwords do not match')
      return
    }
    setSubmitting(true)
    try {
      await authService.resetPassword({ email, otp, newPassword, confirmNewPassword })
      navigate('/login', { replace: true })
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to reset your password. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-shell">
      <AuthHero title="Forgot your password?" subtitle="No problem - we'll send you a code to reset it." />
      <div className="auth-form-panel">
        <div className="auth-form-box">
          <div className="auth-brand">
            <span className="brand-mark" /> AI Video Studio
          </div>

          {step === 'email' ? (
            <>
              <h1>Reset your password</h1>
              <p className="text-muted">Enter your email and we&apos;ll send you a verification code.</p>
              <ErrorMessage message={error} />
              <form onSubmit={handleRequestCode}>
                <div className="form-group">
                  <label className="label" htmlFor="email">
                    Email
                  </label>
                  <input id="email" type="email" className="input" required value={email} onChange={(e) => setEmail(e.target.value)} />
                </div>
                <button type="submit" className="btn btn-accent btn-block" disabled={submitting}>
                  {submitting ? 'Sending...' : 'Send code'}
                </button>
              </form>
            </>
          ) : (
            <>
              <h1>Enter code & new password</h1>
              {info && <p className="text-muted">{info}</p>}
              <ErrorMessage message={error} />
              <form onSubmit={handleReset}>
                <div className="form-group">
                  <label className="label" htmlFor="otp">
                    Verification code
                  </label>
                  <input
                    id="otp"
                    className="input"
                    required
                    inputMode="numeric"
                    maxLength={6}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                  />
                </div>
                <div className="form-group">
                  <label className="label" htmlFor="newPassword">
                    New password
                  </label>
                  <input
                    id="newPassword"
                    type="password"
                    className="input"
                    required
                    minLength={8}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="label" htmlFor="confirmNewPassword">
                    Confirm new password
                  </label>
                  <input
                    id="confirmNewPassword"
                    type="password"
                    className="input"
                    required
                    minLength={8}
                    value={confirmNewPassword}
                    onChange={(e) => setConfirmNewPassword(e.target.value)}
                  />
                </div>
                <button type="submit" className="btn btn-accent btn-block" disabled={submitting}>
                  <KeyRound size={16} /> {submitting ? 'Resetting...' : 'Reset password'}
                </button>
              </form>
            </>
          )}

          <p className="auth-footer-note">
            <Link to="/login">Back to log in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
