import { AlertTriangle } from 'lucide-react'

export default function ErrorMessage({ message }) {
  if (!message) return null
  return (
    <div className="error-banner">
      <AlertTriangle size={15} style={{ verticalAlign: '-3px', marginRight: 7 }} />
      {message}
    </div>
  )
}
