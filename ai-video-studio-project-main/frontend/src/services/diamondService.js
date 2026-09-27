import api from './api'

const diamondService = {
  getPackages: () => api.get('/diamonds/packages').then((r) => r.data),
}

export default diamondService
