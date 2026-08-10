import axios from 'axios';

const API_BASE_URL = 'https://madadwala-backend.vercel.app/api';
const RENDER_BASE_URL = 'https://madadwala-backend.onrender.com/api';

const api = axios.create({
  baseURL: API_BASE_URL,
});

const trackingApi = axios.create({
  baseURL: RENDER_BASE_URL,
});

export const adminApi = {
  getAnalytics: () => api.get('/admin/analytics'),
  getPendingProviders: () => api.get('/admin/pending-providers'),
  getAllProviders: () => api.get('/admin/providers-all'),
  getAllUsers: () => api.get('/admin/users'),
  toggleBlock: (uid) => api.patch(`/admin/users/${uid}/toggle-block`),
  approveProvider: (uid) => api.post('/admin/approve-provider', { uid }),
  getPendingWithdrawals: () => api.get('/admin/withdrawals/pending'),
  getAllWithdrawals: () => api.get('/admin/withdrawals/all'),
  updateWithdrawal: (id, data) => api.patch(`/admin/withdrawals/${id}`, data),
  getActiveJobs: () => api.get('/admin/active-jobs'),
  getAllBookings: () => api.get('/admin/all-bookings'),
  getBookingDetails: (id) => api.get(`/bookings/${id}`),
  getSOSBookingDetails: (id) => trackingApi.get(`/bookings/${id}`),
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
  getReports: () => api.get('/admin/reports'),
  updateReport: (id, status) => api.patch(`/admin/reports/${id}`, { status }),
  getTransactions: () => api.get('/admin/transactions'),
  sendWarning: (data) => api.post('/admin/send-notification', data),
  rejectProvider: (data) => api.post('/admin/reject-provider', data),
  adjustWallet: (data) => api.post('/admin/wallet/adjust', data),
  deleteUser: (uid) => api.delete(`/admin/users/${uid}`),
  getAllReviews: () => api.get('/admin/reviews'),
  deleteReview: (id) => api.delete(`/admin/reviews/${id}`),
  broadcast: (data) => api.post('/admin/broadcast', data),
  getMonitor: () => trackingApi.get('/admin/operations-monitor'),
  startCall: (data) => trackingApi.post('/call/start', data),
};

export default api;
