import api from './api'

const userService = {
  me: () => api.get('/users/me').then((r) => r.data),
  updateMe: (payload) => api.put('/users/me', payload).then((r) => r.data),
}

export default userService
