import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { Clapperboard, Sparkles } from 'lucide-react'
import videoService from '../services/videoService'
import VideoCard from '../components/video/VideoCard'
import LoadingSpinner from '../components/common/LoadingSpinner'
import EmptyState from '../components/common/EmptyState'

const PAGE_SIZE = 12

export default function MyVideos() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = useCallback((p) => {
    setLoading(true)
    videoService
      .list(p, PAGE_SIZE)
      .then((res) => setData(res.data))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load(page)
  }, [page, load])

  async function handleDelete(id) {
    await videoService.remove(id)
    load(page)
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My videos</h1>
          <p className="page-subtitle">Everything you&apos;ve generated, in one place.</p>
        </div>
        <Link to="/generate" className="btn btn-accent">
          <Sparkles size={16} /> Generate video
        </Link>
      </div>

      {loading && !data ? (
        <LoadingSpinner center size="lg" />
      ) : data && data.content.length === 0 ? (
        <EmptyState
          icon={Clapperboard}
          title="No videos yet"
          description="Your generated videos will show up here."
          action={
            <Link to="/generate" className="btn btn-accent btn-sm" style={{ marginTop: 10 }}>
              Generate your first video
            </Link>
          }
        />
      ) : (
        <>
          <div className="video-grid">
            {data.content.map((job) => (
              <VideoCard key={job.id} job={job} onDelete={handleDelete} />
            ))}
          </div>

          {data.totalPages > 1 && (
            <div className="pagination">
              <button className="btn btn-secondary btn-sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Previous
              </button>
              <span>
                Page {page + 1} of {data.totalPages}
              </span>
              <button
                className="btn btn-secondary btn-sm"
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
