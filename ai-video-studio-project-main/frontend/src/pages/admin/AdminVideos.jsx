import { useEffect, useState, useCallback } from 'react'
import { Clapperboard } from 'lucide-react'
import adminService from '../../services/adminService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import EmptyState from '../../components/common/EmptyState'
import { format } from '../../utils/formatters'
import { VIDEO_STATUS_BADGE } from '../../utils/constants'

const STATUS_OPTIONS = ['', 'QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED']

export default function AdminVideos() {
  const [data, setData] = useState(null)
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    adminService.videos({ status: status || undefined, page, size: 20 }).then((res) => setData(res.data))
  }, [status, page])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <div className="page-header">
        <h1>Videos</h1>
      </div>

      <div className="filters-bar">
        <select
          className="select"
          value={status}
          onChange={(e) => {
            setPage(0)
            setStatus(e.target.value)
          }}
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s || 'All statuses'}
            </option>
          ))}
        </select>
      </div>

      {!data ? (
        <LoadingSpinner center size="lg" />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Clapperboard} title="No video jobs found" />
      ) : (
        <>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Prompt</th>
                  <th>Duration</th>
                  <th>Cost</th>
                  <th>Status</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((v) => (
                  <tr key={v.id}>
                    <td>{v.userEmail}</td>
                    <td style={{ maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={v.prompt}>
                      {v.prompt}
                    </td>
                    <td>{v.duration}s</td>
                    <td>{v.diamondCost} 💎</td>
                    <td>
                      <span className={`badge ${VIDEO_STATUS_BADGE[v.status] || 'badge-neutral'}`}>{v.status}</span>
                    </td>
                    <td>{format.dateTime(v.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {data.totalPages > 1 && (
            <div className="pagination">
              <button className="btn btn-secondary btn-sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Previous
              </button>
              <span>
                Page {page + 1} of {data.totalPages}
              </span>
              <button className="btn btn-secondary btn-sm" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
