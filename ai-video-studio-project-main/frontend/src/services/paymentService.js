import api from './api'

const paymentService = {
  createOrder: (packageId) => api.post('/payments/create-order', { packageId }).then((r) => r.data),
  verify: (payload) => api.post('/payments/verify', payload).then((r) => r.data),
  history: (page = 0, size = 20) => api.get('/payments/history', { params: { page, size } }).then((r) => r.data),
}

export default paymentService
