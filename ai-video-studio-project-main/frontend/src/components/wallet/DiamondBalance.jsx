import { Gem } from 'lucide-react'

export default function DiamondBalance({ balance, size = 'md' }) {
  return (
    <span className="diamond-pill" style={size === 'lg' ? { fontSize: '1.05rem', padding: '9px 16px' } : undefined}>
      <Gem /> {balance ?? '—'} diamonds
    </span>
  )
}
