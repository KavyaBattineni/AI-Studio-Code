import { format } from '../../utils/formatters'

const TYPE_LABELS = {
  PURCHASE: 'Purchase',
  VIDEO_GENERATION: 'Video generation',
  DAILY_REWARD: 'Daily reward',
  AD_REWARD: 'Ad reward',
  REFUND: 'Refund',
  ADMIN_ADJUSTMENT: 'Admin adjustment',
}

export default function TransactionTable({ transactions, showUser = false }) {
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>Date</th>
            {showUser && <th>User</th>}
            <th>Type</th>
            <th>Amount</th>
            <th>Balance</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((t) => (
            <tr key={t.id}>
              <td>{format.dateTime(t.createdAt)}</td>
              {showUser && <td>{t.userEmail}</td>}
              <td>{TYPE_LABELS[t.type] || t.type}</td>
              <td className={t.amount >= 0 ? 'amount-positive' : 'amount-negative'}>
                {t.amount >= 0 ? '+' : ''}
                {t.amount}
              </td>
              <td>{t.balanceAfter}</td>
              <td className="text-muted">{t.description}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
