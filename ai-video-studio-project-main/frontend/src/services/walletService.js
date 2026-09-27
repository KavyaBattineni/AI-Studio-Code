import api from './api'

const walletService = {
  getWallet: () => api.get('/wallet').then((r) => r.data),
  getTransactions: (page = 0, size = 20) =>
    api.get('/wallet/transactions', { params: { page, size } }).then((r) => r.data),
}

export default walletService
