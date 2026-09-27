export default function LoadingSpinner({ size = 'md', center = false }) {
  const spinner = <div className={`spinner ${size === 'lg' ? 'spinner-lg' : ''}`} role="status" aria-label="Loading" />
  if (center) {
    return <div className="loading-center">{spinner}</div>
  }
  return spinner
}
