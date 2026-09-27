export const format = {
  dateTime(value) {
    if (!value) return '—'
    const d = new Date(value)
    return d.toLocaleString(undefined, {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  },
  date(value) {
    if (!value) return '—'
    const d = new Date(value)
    return d.toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' })
  },
  currency(amount, currency = 'INR') {
    if (amount === null || amount === undefined) return '—'
    try {
      return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount)
    } catch {
      return `${currency} ${amount}`
    }
  },
}
