export default function AuthHero({ title, subtitle }) {
  return (
    <div className="auth-hero">
      <div className="auth-brand" style={{ color: 'var(--white)' }}>
        <span className="brand-mark" /> AI Video Studio
      </div>
      <div className="auth-hero-content">
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
      <svg className="facet-graphic" width="320" height="320" viewBox="0 0 320 320" fill="none" xmlns="http://www.w3.org/2000/svg">
        <polygon points="160,20 260,90 220,220 100,220 60,90" stroke="#E8A33D" strokeWidth="1.2" opacity="0.5" />
        <polygon points="160,20 220,220 100,220" stroke="#6C5CE0" strokeWidth="1.2" opacity="0.4" />
        <line x1="160" y1="20" x2="160" y2="220" stroke="#E8A33D" strokeWidth="1" opacity="0.35" />
        <line x1="60" y1="90" x2="260" y2="90" stroke="#6C5CE0" strokeWidth="1" opacity="0.3" />
      </svg>
    </div>
  )
}
