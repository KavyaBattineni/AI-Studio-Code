import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'
import authService from '../../services/authService'
import { useAuth } from '../../context/AuthContext'
import ErrorMessage from '../../components/common/ErrorMessage'
import AuthHero from './AuthHero'

const OTP_LENGTH = 6
const RESEND_COOLDOWN_SECONDS = 60

export default function VerifyOtp() {
  const location = useLocation()
  const navigate = useNavigate()
  const { completeVerification } = useAuth()
  const email = location.state?.email

  const [digits, setDigits] = useState(Array(OTP_LENGTH).fill(''))
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [resending, setResending] = useState(false)
  const [cooldown, setCooldown] = useState(RESEND_COOLDOWN_SECONDS)
  const inputRefs = useRef([])

  useEffect(() => {
    if (!email) return
    const timer = setInterval(() => setCooldown((c) => Math.max(0, c - 1)), 1000)
    return () => clearInterval(timer)
  }, [email])

  if (!email) {
    return (
      <div className="auth-shell">
        <AuthHero title="Almost there." subtitle="Verify your email to activate your account." />
        <div className="auth-form-panel">
          <div className="auth-form-box text-center">
            <p>We couldn&apos;t find a pending verification. Please register again.</p>
            <Link to="/register" className="btn btn-accent">
              Back to register
            </Link>
          </div>
        </div>
      </div>
    )
  }

  function handleDigitChange(index, value) {
    if (!/^[0-9]?$/.test(value)) return
    const next = [...digits]
    next[index] = value
    setDigits(next)
    if (value && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus()
    }
  }

  function handleKeyDown(index, e) {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  function handlePaste(e) {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH)
    if (!pasted) return
    e.preventDefault()
    setDigits(pasted.split('').concat(Array(OTP_LENGTH).fill('')).slice(0, OTP_LENGTH))
    inputRefs.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus()
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const otp = digits.join('')
    if (otp.length !== OTP_LENGTH) {
      setError('Please enter the full 6-digit code.')
      return
    }
    setSubmitting(true)
    try {
      const res = await authService.verifyOtp({ email, otp })
      completeVerification(res.data)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid code. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleResend() {
    setError('')
    setResending(true)
    try {
      await authService.resendOtp({ email })
      setCooldown(RESEND_COOLDOWN_SECONDS)
      setDigits(Array(OTP_LENGTH).fill(''))
      inputRefs.current[0]?.focus()
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to resend the code right now.')
    } finally {
      setResending(false)
    }
  }

  return (
    <div className="auth-shell">
      <AuthHero title="Check your inbox." subtitle="We sent a 6-digit verification code to your email." />
      <div className="auth-form-panel">
        <div className="auth-form-box">
          <div className="auth-brand">
            <span className="brand-mark" /> AI Video Studio
          </div>
          <h1>Enter 6-digit OTP</h1>
          <p className="text-muted">Sent to {email}</p>

          <ErrorMessage message={error} />

          <form onSubmit={handleSubmit}>
            <div className="otp-input-group" onPaste={handlePaste}>
              {digits.map((digit, i) => (
                <input
                  key={i}
                  ref={(el) => (inputRefs.current[i] = el)}
                  className="otp-digit"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleDigitChange(i, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(i, e)}
                  autoFocus={i === 0}
                />
              ))}
            </div>

            <button type="submit" className="btn btn-accent btn-block" disabled={submitting} style={{ marginTop: 14 }}>
              <ShieldCheck size={16} /> {submitting ? 'Verifying...' : 'Verify'}
            </button>
          </form>

          <div className="text-center countdown-text">
            {cooldown > 0 ? (
              <>Resend code in {cooldown}s</>
            ) : (
              <button className="btn btn-ghost btn-sm" onClick={handleResend} disabled={resending}>
                {resending ? 'Resending...' : 'Resend OTP'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
