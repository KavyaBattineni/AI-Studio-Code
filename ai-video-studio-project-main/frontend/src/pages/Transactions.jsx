import { useEffect, useState, useCallback } from 'react'
import { Receipt } from 'lucide-react'
import walletService from '../services/walletService'
import TransactionTable from '../components/wallet/TransactionTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import EmptyState from '../components/common/EmptyState'

const PAGE_SIZE = 20

export default function Transactions() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)

  const load = useCallback((p) => {
    walletService.getTransactions(p, PAGE_SIZE).then((res) => setData(res.data))
  }, [])

  useEffect(() => {
    load(page)
  }, [page, load])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Transactions</h1>
          <p className="page-subtitle">Every diamond credit and debit on your account.</p>
        </div>
      </div>

      {!data ? (
        <LoadingSpinner center size="lg" />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Receipt} title="No transactions yet" />
      ) : (
        <>
          <TransactionTable transactions={data.content} />
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
