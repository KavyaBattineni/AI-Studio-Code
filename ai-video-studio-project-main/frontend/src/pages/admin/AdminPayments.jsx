import { useEffect, useState, useCallback } from 'react'
import { CreditCard } from 'lucide-react'
import adminService from '../../services/adminService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import EmptyState from '../../components/common/EmptyState'
import { format } from '../../utils/formatters'
import { PAYMENT_STATUS_BADGE } from '../../utils/constants'

const STATUS_OPTIONS = ['', 'CREATED', 'PENDING', 'SUCCESS', 'FAILED', 'REFUNDED']

export default function AdminPayments() {
  const [data, setData] = useState(null)
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    adminService.payments({ status: status || undefined, page, size: 20 }).then((res) => setData(res.data))
  }, [status, page])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <div className="page-header">
        <h1>Payments</h1>
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
        <EmptyState icon={CreditCard} title="No payments found" />
      ) : (
        <>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>User</th>
                  <th>Package</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id}>
                    <td>{p.orderId}</td>
                    <td>{p.userEmail}</td>
                    <td>{p.packageName}</td>
                    <td>{format.currency(p.amount, p.currency)}</td>
                    <td>
                      <span className={`badge ${PAYMENT_STATUS_BADGE[p.status] || 'badge-neutral'}`}>{p.status}</span>
                    </td>
                    <td>{format.dateTime(p.createdAt)}</td>
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
