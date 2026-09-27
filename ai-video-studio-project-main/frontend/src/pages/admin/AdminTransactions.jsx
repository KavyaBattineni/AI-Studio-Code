import { useEffect, useState, useCallback } from 'react'
import { Receipt } from 'lucide-react'
import adminService from '../../services/adminService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import EmptyState from '../../components/common/EmptyState'
import TransactionTable from '../../components/wallet/TransactionTable'

const TYPE_OPTIONS = ['', 'PURCHASE', 'VIDEO_GENERATION', 'DAILY_REWARD', 'AD_REWARD', 'REFUND', 'ADMIN_ADJUSTMENT']

export default function AdminTransactions() {
  const [data, setData] = useState(null)
  const [type, setType] = useState('')
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    adminService.transactions({ type: type || undefined, page, size: 25 }).then((res) => setData(res.data))
  }, [type, page])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <div className="page-header">
        <h1>Transactions</h1>
      </div>

      <div className="filters-bar">
        <select
          className="select"
          value={type}
          onChange={(e) => {
            setPage(0)
            setType(e.target.value)
          }}
        >
          {TYPE_OPTIONS.map((t) => (
            <option key={t} value={t}>
              {t || 'All types'}
            </option>
          ))}
        </select>
      </div>

      {!data ? (
        <LoadingSpinner center size="lg" />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Receipt} title="No transactions found" />
      ) : (
        <>
          <TransactionTable transactions={data.content} showUser />
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
