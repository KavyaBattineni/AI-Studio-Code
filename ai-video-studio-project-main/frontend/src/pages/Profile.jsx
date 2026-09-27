import { useState } from 'react'
import { Save, KeyRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import userService from '../services/userService'
import authService from '../services/authService'
import ErrorMessage from '../components/common/ErrorMessage'
import { format } from '../utils/formatters'
import { USER_STATUS_BADGE } from '../utils/constants'

export default function Profile() {
  const { user, refreshProfile } = useAuth()
  const toast = useToast()

  const [name, setName] = useState(user?.name || '')
  const [mobile, setMobile] = useState(user?.mobile || '')
  const [savingProfile, setSavingProfile] = useState(false)
  const [profileError, setProfileError] = useState('')

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [changingPassword, setChangingPassword] = useState(false)
  const [passwordError, setPasswordError] = useState('')

  const initials = user?.name
    ?.split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  async function handleProfileSubmit(e) {
    e.preventDefault()
    setProfileError('')
    setSavingProfile(true)
    try {
      await userService.updateMe({ name, mobile })
      await refreshProfile()
      toast.success('Profile updated')
    } catch (err) {
      setProfileError(err.response?.data?.message || 'Unable to update your profile.')
    } finally {
      setSavingProfile(false)
    }
  }

  async function handlePasswordSubmit(e) {
    e.preventDefault()
    setPasswordError('')
    if (newPassword !== confirmNewPassword) {
      setPasswordError('New passwords do not match')
      return
    }
    setChangingPassword(true)
    try {
      await authService.changePassword({ currentPassword, newPassword, confirmNewPassword })
      toast.success('Password changed')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmNewPassword('')
    } catch (err) {
      setPasswordError(err.response?.data?.message || 'Unable to change your password.')
    } finally {
      setChangingPassword(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Profile</h1>
          <p className="page-subtitle">Manage your account details.</p>
        </div>
      </div>

      <div className="profile-grid">
        <div className="card profile-summary">
          <div className="user-avatar">{initials}</div>
          <h3 className="mt-0">{user?.name}</h3>
          <p className="text-muted" style={{ marginBottom: 14 }}>
            {user?.email}
          </p>
          <div>
            <span className={`badge ${USER_STATUS_BADGE[user?.status] || 'badge-neutral'}`}>{user?.status}</span>
          </div>
          <p className="text-muted" style={{ marginTop: 14, fontSize: '0.82rem' }}>
            Member since {format.date(user?.createdAt)}
          </p>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div className="card">
            <h3 className="mt-0">Account details</h3>
            <ErrorMessage message={profileError} />
            <form onSubmit={handleProfileSubmit}>
              <div className="form-group">
                <label className="label" htmlFor="name">
                  Full name
                </label>
                <input id="name" className="input" value={name} onChange={(e) => setName(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="label">Email</label>
                <input className="input" value={user?.email || ''} disabled />
                <div className="field-hint">Email address can&apos;t be changed.</div>
              </div>
              <div className="form-group">
                <label className="label" htmlFor="mobile">
                  Mobile number
                </label>
                <input
                  id="mobile"
                  className="input"
                  value={mobile}
                  onChange={(e) => setMobile(e.target.value)}
                  pattern="[0-9]{10,15}"
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={savingProfile}>
                <Save size={15} /> {savingProfile ? 'Saving...' : 'Save changes'}
              </button>
            </form>
          </div>

          <div className="card">
            <h3 className="mt-0">Change password</h3>
            <ErrorMessage message={passwordError} />
            <form onSubmit={handlePasswordSubmit}>
              <div className="form-group">
                <label className="label" htmlFor="currentPassword">
                  Current password
                </label>
                <input
                  id="currentPassword"
                  type="password"
                  className="input"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="label" htmlFor="newPassword">
                    New password
                  </label>
                  <input
                    id="newPassword"
                    type="password"
                    className="input"
                    minLength={8}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
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
                    minLength={8}
                    value={confirmNewPassword}
                    onChange={(e) => setConfirmNewPassword(e.target.value)}
                    required
                  />
                </div>
              </div>
              <button type="submit" className="btn btn-secondary" disabled={changingPassword}>
                <KeyRound size={15} /> {changingPassword ? 'Updating...' : 'Change password'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
