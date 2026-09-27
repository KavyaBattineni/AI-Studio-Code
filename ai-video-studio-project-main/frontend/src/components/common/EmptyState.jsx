import { Inbox } from 'lucide-react'

export default function EmptyState({ icon: Icon = Inbox, title = 'Nothing here yet', description, action }) {
  return (
    <div className="empty-state">
      <Icon />
      <h3 style={{ marginBottom: 6 }}>{title}</h3>
      {description && <p className="text-muted">{description}</p>}
      {action}
    </div>
  )
}
