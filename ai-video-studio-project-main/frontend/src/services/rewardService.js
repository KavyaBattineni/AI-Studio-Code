import api from './api'

const rewardService = {
  status: () => api.get('/rewards/status').then((r) => r.data),
  verifyAd: (payload) => api.post('/rewards/ad/verify', payload).then((r) => r.data),
}

export default rewardService
