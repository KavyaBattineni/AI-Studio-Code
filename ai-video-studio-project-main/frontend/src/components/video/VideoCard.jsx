import { useState } from 'react'
import { Play, Trash2, Clock, AlertCircle, Loader2 } from 'lucide-react'
import { VIDEO_STATUS_LABELS, VIDEO_STATUS_BADGE } from '../../utils/constants'
import { format } from '../../utils/formatters'
import Modal from '../common/Modal'

export default function VideoCard({ job, onDelete }) {
  const [showPlayer, setShowPlayer] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)

  return (
    <div className="video-card">
      <div className="video-card-thumb">
        {job.status === 'COMPLETED' && job.thumbnailUrl ? (
          <img src={job.thumbnailUrl} alt={job.prompt} onError={(e) => (e.target.style.display = 'none')} />
        ) : job.status === 'PROCESSING' || job.status === 'QUEUED' ? (
          <Loader2 className="spin-icon" size={26} />
        ) : job.status === 'FAILED' ? (
          <AlertCircle size={26} />
        ) : (
          <Clock size={26} />
        )}
      </div>
      <div className="video-card-body">
        <div className="video-card-prompt" title={job.prompt}>
          {job.prompt}
        </div>
        <div className="video-card-meta">
          <span className={`badge ${VIDEO_STATUS_BADGE[job.status]}`}>{VIDEO_STATUS_LABELS[job.status]}</span>
          <span>
            {job.duration}s · {format.date(job.createdAt)}
          </span>
        </div>
        {job.status === 'FAILED' && job.errorMessage && (
          <div className="field-error" style={{ marginTop: -2 }}>
            {job.errorMessage}
          </div>
        )}
        <div className="video-card-actions">
          {job.status === 'COMPLETED' && (
            <button className="btn btn-secondary btn-sm w-full" onClick={() => setShowPlayer(true)}>
              <Play size={14} /> View
            </button>
          )}
          <button className="btn btn-ghost btn-sm" onClick={() => setConfirmDelete(true)} aria-label="Delete video">
            <Trash2 size={14} />
          </button>
        </div>
      </div>

      {showPlayer && (
        <Modal title="Your video" onClose={() => setShowPlayer(false)}>
          <video controls autoPlay style={{ width: '100%', borderRadius: 10, background: '#000' }} src={job.videoUrl} />
          <p style={{ marginTop: 10 }} className="text-muted">
            {job.prompt}
          </p>
        </Modal>
      )}

      {confirmDelete && (
        <Modal
          title="Delete this video?"
          onClose={() => setConfirmDelete(false)}
          actions={
            <>
              <button className="btn btn-secondary" onClick={() => setConfirmDelete(false)}>
                Cancel
              </button>
              <button
                className="btn btn-danger"
                onClick={() => {
                  setConfirmDelete(false)
                  onDelete(job.id)
                }}
              >
                Delete
              </button>
            </>
          }
        >
          <p className="mt-0">This action can&apos;t be undone.</p>
        </Modal>
      )}
    </div>
  )
}
