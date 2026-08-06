import axios from 'axios';

const API_BASE_URL = 'https://madadwala-backend.vercel.app/api';

const api = axios.create({
  baseURL: API_BASE_URL,
});

export const adminApi = {
  getAnalytics: () => api.get('/admin/analytics'),
  getPendingProviders: () => api.get('/admin/pending-providers'),
  approveProvider: (uid) => api.post('/admin/approve-provider', { uid }),
  getPendingWithdrawals: () => api.get('/admin/withdrawals/pending'),
  updateWithdrawal: (id, data) => api.patch(`/admin/withdrawals/${id}`, data),
  getActiveJobs: () => api.get('/admin/active-jobs'),
  getCategories: () => api.get('/categories'),
  addCategory: (data) => api.post('/admin/categories', data),
  deleteCategory: (id) => api.delete(`/admin/categories/${id}`),
  getOffers: () => api.get('/offers'),
  addOffer: (data) => api.post('/admin/offers', data),
  updateOffer: (id, data) => api.put(`/admin/offers/${id}`, data),
  deleteOffer: (id) => api.delete(`/admin/offers/${id}`),
  getBanners: () => api.get('/banners'),
  addBanner: (formData) => api.post('/admin/banners', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  deleteBanner: (id) => api.delete(`/admin/banners/${id}`),
  getSettings: () => api.get('/admin/settings'),
  updateSetting: (key, value) => api.post('/admin/settings', { key, value }),
  getSupportChats: () => api.get('/admin/support/chats'),
  getChatMessages: (userId) => api.get(`/support/messages/${userId}`),
  sendSupportMessage: (data) => api.post('/support/messages', data),
};

export default api;
