import { Gem } from 'lucide-react'

const OPTIONS = [
  { duration: 4, cost: 5, label: '4 Seconds' },
  { duration: 8, cost: 10, label: '8 Seconds' },
]

export default function DurationSelector({ value, onChange }) {
  return (
    <div className="form-group">
      <label className="label">Duration</label>
      <div className="duration-options">
        {OPTIONS.map((opt) => (
          <button
            type="button"
            key={opt.duration}
            className={`duration-option ${value === opt.duration ? 'selected' : ''}`}
            onClick={() => onChange(opt.duration)}
          >
            <div className="duration-label">{opt.label}</div>
            <div className="duration-cost">
              <Gem size={14} /> {opt.cost} diamonds
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
