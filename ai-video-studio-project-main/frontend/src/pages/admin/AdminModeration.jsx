import { useEffect, useState, useCallback } from 'react'
import { ShieldAlert } from 'lucide-react'
import adminService from '../../services/adminService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import EmptyState from '../../components/common/EmptyState'
import { format } from '../../utils/formatters'

const DECISION_OPTIONS = ['', 'ALLOWED', 'BLOCKED']

export default function AdminModeration() {
  const [data, setData] = useState(null)
  const [decision, setDecision] = useState('BLOCKED')
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    adminService.moderation({ decision: decision || undefined, page, size: 20 }).then((res) => setData(res.data))
  }, [decision, page])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Moderation</h1>
          <p className="page-subtitle">Prompt moderation events. Internal scoring details are never shown here or to users.</p>
        </div>
      </div>

      <div className="filters-bar">
        <select
          className="select"
          value={decision}
          onChange={(e) => {
            setPage(0)
            setDecision(e.target.value)
          }}
        >
          {DECISION_OPTIONS.map((d) => (
            <option key={d} value={d}>
              {d || 'All decisions'}
            </option>
          ))}
        </select>
      </div>

      {!data ? (
        <LoadingSpinner center size="lg" />
      ) : data.content.length === 0 ? (
        <EmptyState icon={ShieldAlert} title="No moderation events found" />
      ) : (
        <>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Input type</th>
                  <th>Snippet</th>
                  <th>Decision</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((m) => (
                  <tr key={m.id}>
                    <td>{m.userEmail}</td>
                    <td>{m.inputType}</td>
                    <td style={{ maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={m.inputSnippet}>
                      {m.inputSnippet}
                    </td>
                    <td>
                      <span className={`badge ${m.decision === 'BLOCKED' ? 'badge-danger' : 'badge-success'}`}>{m.decision}</span>
                    </td>
                    <td>{format.dateTime(m.createdAt)}</td>
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
