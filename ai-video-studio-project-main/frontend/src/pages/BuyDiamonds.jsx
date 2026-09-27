import { useEffect, useState } from 'react'
import { Gem, Loader2 } from 'lucide-react'
import diamondService from '../services/diamondService'
import paymentService from '../services/paymentService'
import { useWallet } from '../context/WalletContext'
import { useToast } from '../context/ToastContext'
import LoadingSpinner from '../components/common/LoadingSpinner'
import Modal from '../components/common/Modal'
import ErrorMessage from '../components/common/ErrorMessage'
import { format } from '../utils/formatters'

export default function BuyDiamonds() {
  const { refreshBalance } = useWallet()
  const toast = useToast()

  const [packages, setPackages] = useState(null)
  const [checkoutOrder, setCheckoutOrder] = useState(null)
  const [paying, setPaying] = useState(false)
  const [creatingFor, setCreatingFor] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    diamondService.getPackages().then((res) => setPackages(res.data))
  }, [])

  async function startCheckout(pkg) {
    setError('')
    setCreatingFor(pkg.id)
    try {
      const res = await paymentService.createOrder(pkg.id)
      setCheckoutOrder(res.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to start checkout. Please try again.')
    } finally {
      setCreatingFor(null)
    }
  }

  async function confirmPayment() {
    setPaying(true)
    setError('')
    try {
      await paymentService.verify({ orderId: checkoutOrder.orderId })
      toast.success(`${checkoutOrder.diamonds} diamonds added to your wallet!`)
      await refreshBalance()
      setCheckoutOrder(null)
    } catch (err) {
      setError(err.response?.data?.message || 'Payment verification failed.')
    } finally {
      setPaying(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Buy diamonds</h1>
          <p className="page-subtitle">Top up your balance to keep generating videos.</p>
        </div>
      </div>

      <ErrorMessage message={error} />

      {!packages ? (
        <LoadingSpinner center size="lg" />
      ) : (
        <div className="package-grid">
          {packages.map((pkg) => (
            <div key={pkg.id} className="package-card">
              <Gem size={30} color="var(--facet-deep)" />
              <div className="package-diamonds">{pkg.diamonds}</div>
              <div className="text-muted mt-0">diamonds</div>
              <div className="package-price">{format.currency(pkg.price, pkg.currency)}</div>
              <button className="btn btn-accent btn-block" onClick={() => startCheckout(pkg)} disabled={creatingFor === pkg.id}>
                {creatingFor === pkg.id ? <Loader2 size={16} className="spin-icon" /> : 'Buy Now'}
              </button>
            </div>
          ))}
        </div>
      )}

      {checkoutOrder && (
        <Modal
          title="Complete your purchase"
          onClose={() => !paying && setCheckoutOrder(null)}
          actions={
            <>
              <button className="btn btn-secondary" disabled={paying} onClick={() => setCheckoutOrder(null)}>
                Cancel
              </button>
              <button className="btn btn-accent" disabled={paying} onClick={confirmPayment}>
                {paying ? 'Processing...' : 'Pay Now'}
              </button>
            </>
          }
        >
          <p className="mt-0">
            {checkoutOrder.diamonds} diamonds for {format.currency(checkoutOrder.amount, checkoutOrder.currency)}
          </p>
          <p className="text-muted" style={{ fontSize: '0.85rem' }}>
            This is a simulated checkout (mock payment provider) - no real charge will be made. In production this
            step is replaced by your payment gateway's real checkout widget.
          </p>
        </Modal>
      )}
    </div>
  )
}
