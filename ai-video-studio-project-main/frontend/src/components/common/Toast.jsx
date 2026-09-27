import { CheckCircle2, XCircle, Info, X } from 'lucide-react'

const ICONS = {
  success: CheckCircle2,
  error: XCircle,
  info: Info,
}

export default function Toast({ type = 'info', message, onClose }) {
  const Icon = ICONS[type] || Info
  return (
    <div className={`toast toast-${type}`}>
      <Icon size={18} />
      <div style={{ flex: 1 }}>{message}</div>
      <button
        onClick={onClose}
        aria-label="Dismiss"
        style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', opacity: 0.7 }}
      >
        <X size={15} />
      </button>
    </div>
  )
}
