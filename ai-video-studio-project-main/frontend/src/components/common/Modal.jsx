export default function Modal({ title, children, onClose, actions }) {
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose?.()}>
      <div className="modal-panel" role="dialog" aria-modal="true" aria-label={title}>
        {title && <h3 className="modal-title">{title}</h3>}
        <div>{children}</div>
        {actions && <div className="modal-actions">{actions}</div>}
      </div>
    </div>
  )
}
