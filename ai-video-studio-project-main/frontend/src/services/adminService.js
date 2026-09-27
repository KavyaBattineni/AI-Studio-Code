import api from './api'

const adminService = {
  dashboard: () => api.get('/admin/dashboard').then((r) => r.data),
  users: (params) => api.get('/admin/users', { params }).then((r) => r.data),
  updateUserStatus: (id, status) => api.put(`/admin/users/${id}/status`, { status }).then((r) => r.data),
  payments: (params) => api.get('/admin/payments', { params }).then((r) => r.data),
  videos: (params) => api.get('/admin/videos', { params }).then((r) => r.data),
  moderation: (params) => api.get('/admin/moderation', { params }).then((r) => r.data),
  transactions: (params) => api.get('/admin/transactions', { params }).then((r) => r.data),
  revenue: () => api.get('/admin/revenue').then((r) => r.data),
}

export default adminService
