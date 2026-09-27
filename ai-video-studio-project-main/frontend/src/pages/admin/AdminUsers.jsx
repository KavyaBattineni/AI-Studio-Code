import { useEffect, useState, useCallback } from 'react'
import { Users } from 'lucide-react'
import adminService from '../../services/adminService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import EmptyState from '../../components/common/EmptyState'
import { useToast } from '../../context/ToastContext'
import { format } from '../../utils/formatters'
import { USER_STATUS_BADGE } from '../../utils/constants'

const STATUS_OPTIONS = ['', 'ACTIVE', 'INACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED']

export default function AdminUsers() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [updatingId, setUpdatingId] = useState(null)

  const load = useCallback(() => {
    adminService.users({ search: search || undefined, status: status || undefined, page, size: 20 }).then((res) => setData(res.data))
  }, [search, status, page])

  useEffect(() => {
    load()
  }, [load])

  async function updateStatus(id, newStatus) {
    setUpdatingId(id)
    try {
      await adminService.updateUserStatus(id, newStatus)
      toast.success('User status updated')
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Unable to update user status')
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Users</h1>
      </div>

      <div className="filters-bar">
        <input
          className="input"
          placeholder="Search name or email..."
          value={search}
          onChange={(e) => {
            setPage(0)
            setSearch(e.target.value)
          }}
        />
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
        <EmptyState icon={Users} title="No users found" />
      ) : (
        <>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Balance</th>
                  <th>Joined</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.name}</td>
                    <td>{u.email}</td>
                    <td>{u.role}</td>
                    <td>
                      <span className={`badge ${USER_STATUS_BADGE[u.status] || 'badge-neutral'}`}>{u.status}</span>
                    </td>
                    <td>{u.diamondBalance} 💎</td>
                    <td>{format.date(u.createdAt)}</td>
                    <td>
                      <div className="flex gap-8">
                        {u.status !== 'ACTIVE' && (
                          <button className="btn btn-secondary btn-sm" disabled={updatingId === u.id} onClick={() => updateStatus(u.id, 'ACTIVE')}>
                            Activate
                          </button>
                        )}
                        {u.status !== 'INACTIVE' && (
                          <button className="btn btn-ghost btn-sm" disabled={updatingId === u.id} onClick={() => updateStatus(u.id, 'INACTIVE')}>
                            Deactivate
                          </button>
                        )}
                        {u.status !== 'SUSPENDED' && (
                          <button className="btn btn-danger btn-sm" disabled={updatingId === u.id} onClick={() => updateStatus(u.id, 'SUSPENDED')}>
                            Suspend
                          </button>
                        )}
                      </div>
                    </td>
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
