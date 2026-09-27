import { useEffect, useRef, useState } from 'react'
import { Sparkles, Gem } from 'lucide-react'
import PromptBox from './PromptBox'
import DurationSelector from './DurationSelector'
import Modal from '../common/Modal'
import ErrorMessage from '../common/ErrorMessage'
import { useWallet } from '../../context/WalletContext'
import { useToast } from '../../context/ToastContext'
import videoService from '../../services/videoService'
import { VIDEO_STATUS_LABELS, VIDEO_STATUS_BADGE } from '../../utils/constants'

const COST_BY_DURATION = { 4: 5, 8: 10 }
const POLL_INTERVAL_MS = 2500
const TERMINAL_STATUSES = ['COMPLETED', 'FAILED', 'CANCELLED']

export default function VideoGenerator({ onCompleted }) {
  const { balance, refreshBalance } = useWallet()
  const toast = useToast()

  const [prompt, setPrompt] = useState('')
  const [duration, setDuration] = useState(4)
  const [promptError, setPromptError] = useState('')
  const [showConfirm, setShowConfirm] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [activeJob, setActiveJob] = useState(null)
  const [error, setError] = useState('')
  const pollRef = useRef(null)

  const cost = COST_BY_DURATION[duration]

  useEffect(() => () => clearInterval(pollRef.current), [])

  function handleGenerateClick() {
    setError('')
    if (!prompt.trim()) {
      setPromptError('Please describe what you want to see.')
      return
    }
    setPromptError('')
    setShowConfirm(true)
  }

  async function confirmGenerate() {
    setShowConfirm(false)
    setSubmitting(true)
    setError('')
    try {
      const res = await videoService.generate({ prompt: prompt.trim(), duration })
      setActiveJob(res.data)
      await refreshBalance()
      setPrompt('')
      startPolling(res.data.id)
    } catch (err) {
      setError(err.response?.data?.message || 'Something went wrong starting your video. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  function startPolling(jobId) {
    clearInterval(pollRef.current)
    pollRef.current = setInterval(async () => {
      try {
        const res = await videoService.get(jobId)
        setActiveJob(res.data)
        if (TERMINAL_STATUSES.includes(res.data.status)) {
          clearInterval(pollRef.current)
          refreshBalance() // pick up a refund if the job failed
          if (res.data.status === 'COMPLETED') toast.success('Your video is ready!')
          if (res.data.status === 'FAILED') toast.error('Generation failed - diamonds were refunded.')
          onCompleted?.()
        }
      } catch {
        clearInterval(pollRef.current)
      }
    }, POLL_INTERVAL_MS)
  }

  const remaining = balance !== null ? balance - cost : null

  return (
    <div>
      <div className="balance-banner">
        <span>
          Current balance: <strong>{balance ?? '—'} diamonds</strong>
        </span>
        <Gem size={16} color="var(--facet-deep)" />
      </div>

      <ErrorMessage message={error} />

      <PromptBox value={prompt} onChange={setPrompt} error={promptError} />
      <DurationSelector value={duration} onChange={setDuration} />

      <button className="btn btn-accent btn-block" onClick={handleGenerateClick} disabled={submitting}>
        <Sparkles size={16} /> {submitting ? 'Starting...' : 'Generate Video'}
      </button>

      {activeJob && (
        <div className="card" style={{ marginTop: 22 }}>
          <div className="flex-between" style={{ marginBottom: 10 }}>
            <strong>Video generation {TERMINAL_STATUSES.includes(activeJob.status) ? 'finished' : 'started'}</strong>
            <span className={`badge ${VIDEO_STATUS_BADGE[activeJob.status]}`}>
              {VIDEO_STATUS_LABELS[activeJob.status]}
            </span>
          </div>
          <p className="text-muted mt-0">{activeJob.prompt}</p>
          {activeJob.status === 'COMPLETED' && activeJob.videoUrl && (
            <video controls style={{ width: '100%', borderRadius: 10, background: '#000', marginTop: 6 }} src={activeJob.videoUrl} />
          )}
          {activeJob.status === 'FAILED' && <ErrorMessage message={activeJob.errorMessage || 'Generation failed. Diamonds were refunded.'} />}
        </div>
      )}

      {showConfirm && (
        <Modal
          title="Confirm generation"
          onClose={() => setShowConfirm(false)}
          actions={
            <>
              <button className="btn btn-secondary" onClick={() => setShowConfirm(false)}>
                Cancel
              </button>
              <button className="btn btn-accent" onClick={confirmGenerate}>
                Generate
              </button>
            </>
          }
        >
          <p className="mt-0">
            You have <strong>{balance ?? '—'} diamonds</strong>.
          </p>
          <p>
            Generation cost: <strong>{cost} diamonds</strong>
          </p>
          <p style={{ marginBottom: 0 }}>
            Remaining after generation: <strong>{remaining ?? '—'} diamonds</strong>
          </p>
        </Modal>
      )}
    </div>
  )
}
