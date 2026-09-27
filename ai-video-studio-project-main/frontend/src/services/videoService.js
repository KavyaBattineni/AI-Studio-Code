import api from './api'

const videoService = {
  generate: (payload) => api.post('/videos/generate', payload).then((r) => r.data),
  list: (page = 0, size = 12) => api.get('/videos', { params: { page, size } }).then((r) => r.data),
  get: (id) => api.get(`/videos/${id}`).then((r) => r.data),
  remove: (id) => api.delete(`/videos/${id}`).then((r) => r.data),
}

export default videoService
