'use client';
import React, { useState, useEffect } from 'react';
import {
  Users, Briefcase, CheckCircle, XCircle, BarChart3,
  Settings, LogOut, MessageSquare, Image as ImageIcon,
  Tag, CreditCard, Bell, ChevronRight, Search, ShieldAlert,
  UserCheck, UserMinus, Clock, Filter, FileText, AlertTriangle,
  Trash2, Plus, Star, Wallet, Send, X, MoreVertical, Eye, Check,
  Activity, Zap, MapPin, Navigation, Info, RefreshCw, Maximize2, Phone, ExternalLink
} from 'lucide-react';
import { adminApi } from '@/lib/api';
import dynamic from 'next/dynamic';

const MapComponent = dynamic(() => import('@/components/MapComponent'), {
  ssr: false,
  loading: () => <div className="h-full w-full bg-gray-50 flex items-center justify-center text-sm font-medium text-gray-400">Loading map…</div>
});

/* ---------- Font (Inter) ---------- */
const FontStyles = () => (
  <style jsx global>{`
    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');
    .madadwala-app, .madadwala-app * {
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    }
    .custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 8px; }
    .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #9ca3af; }
  `}</style>
);

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('monitor');
  const [data, setData] = useState({
    analytics: null, pendingProviders: [], withdrawals: [],
    activeJobs: [], categories: [], offers: [], banners: [],
    settings: {}, chats: [], allUsers: [], allProviders: [],
    allBookings: [], reports: [], reviews: [], monitor: []
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeModal, setActiveModal] = useState(null);
  const [selectedUser, setSelectedUser] = useState(null);
  const [formState, setFormState] = useState({ title: '', message: '', amount: '', type: 'credit', description: '', role: 'all' });
  const [toast, setToast] = useState(null);
  const [selectedPartner, setSelectedPartner] = useState(null);

  useEffect(() => { fetchTabData(activeTab); }, [activeTab]);
  useEffect(() => {
    if (toast) { const timer = setTimeout(() => setToast(null), 3000); return () => clearTimeout(timer); }
  }, [toast]);

  const showToast = (message, type = 'success') => setToast({ message, type });

  const fetchTabData = async (tab) => {
    setLoading(true); setError(null);
    try {
      let res;
      switch(tab) {
        case 'analytics': res = await adminApi.getAnalytics(); setData(p => ({...p, analytics: res.data})); break;
        case 'monitor': res = await adminApi.getMonitor(); setData(p => ({...p, monitor: res.data})); if(res.data.length > 0 && !selectedPartner) setSelectedPartner(res.data[0]); break;
        case 'providers-pending': res = await adminApi.getPendingProviders(); setData(p => ({...p, pendingProviders: res.data})); break;
        case 'customers': res = await adminApi.getAllUsers(); setData(p => ({...p, allUsers: res.data})); break;
        case 'providers-all': res = await adminApi.getAllProviders(); setData(p => ({...p, allProviders: res.data})); break;
        case 'withdrawals': res = await adminApi.getPendingWithdrawals(); setData(p => ({...p, withdrawals: res.data})); break;
        case 'jobs': res = await adminApi.getActiveJobs(); setData(p => ({...p, activeJobs: res.data})); break;
        case 'bookings-all': res = await adminApi.getAllBookings(); setData(p => ({...p, allBookings: res.data})); break;
        case 'categories': res = await adminApi.getCategories(); setData(p => ({...p, categories: res.data})); break;
        case 'offers': res = await adminApi.getOffers(); setData(p => ({...p, offers: res.data})); break;
        case 'banners': res = await adminApi.getBanners(); setData(p => ({...p, banners: res.data})); break;
        case 'settings': res = await adminApi.getSettings(); setData(p => ({...p, settings: res.data})); break;
        case 'reports': res = await adminApi.getReports(); setData(p => ({...p, reports: res.data})); break;
        case 'reviews': res = await adminApi.getAllReviews(); setData(p => ({...p, reviews: res.data})); break;
        case 'support': res = await adminApi.getSupportChats(); setData(p => ({...p, chats: res.data})); break;
      }
    } catch (err) { setError("Couldn't load this data. Try refreshing."); }
    setLoading(false);
  };

  const openAction = (type, user = null) => {
    setSelectedUser(user); setActiveModal(type);
    setFormState({ title: '', message: '', amount: '', type: 'credit', description: '', role: 'all' });
  };

  const handleAction = async () => {
    try {
      if (activeModal === 'warning') await adminApi.sendWarning({ uid: selectedUser.uid, title: formState.title, message: formState.message, type: 'warning' });
      else if (activeModal === 'wallet') { await adminApi.adjustWallet({ uid: selectedUser.uid, amount: formState.amount, type: formState.type, description: formState.description }); fetchTabData(activeTab); }
      else if (activeModal === 'broadcast') await adminApi.broadcast({ role: formState.role, title: formState.title, message: formState.message });
      else if (activeModal === 'delete') { await adminApi.deleteUser(selectedUser.uid); fetchTabData(activeTab); }
      showToast("Done"); setActiveModal(null);
    } catch (e) { showToast("Something went wrong", "error"); }
  };

  const tabTitles = {
    monitor: 'Live map', analytics: 'Analytics', customers: 'Customers', 'providers-all': 'Partners',
    'providers-pending': 'Pending approvals', withdrawals: 'Withdrawals', jobs: 'Active jobs',
    'bookings-all': 'Booking history', categories: 'Categories', offers: 'Offers', banners: 'Banners',
    settings: 'Settings', reports: 'Reports', reviews: 'Reviews', support: 'Support'
  };

  return (
    <div className="madadwala-app flex h-screen bg-gray-50 text-gray-900 overflow-hidden text-[13px]">
      <FontStyles />

      {toast && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-[100] animate-in fade-in slide-in-from-top-2 duration-200">
          <div className={`px-4 py-2.5 rounded-lg shadow-lg flex items-center gap-2 border text-sm font-medium ${toast.type === 'success' ? 'bg-white border-emerald-200 text-emerald-700' : 'bg-white border-red-200 text-red-700'}`}>
            {toast.type === 'success' ? <Check size={15} /> : <XCircle size={15} />}
            <span>{toast.message}</span>
          </div>
        </div>
      )}

      {/* Sidebar */}
      <aside className="w-60 bg-[#111827] text-gray-300 flex flex-col shrink-0">
        <div className="h-16 px-5 flex items-center border-b border-white/10">
          <span className="font-bold text-lg text-white tracking-tight">Madadwala</span>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto custom-scrollbar">
          <NavItem icon={<Activity size={16}/>} label="Live map" active={activeTab==='monitor'} onClick={()=>setActiveTab('monitor')} />
          <NavItem icon={<BarChart3 size={16}/>} label="Analytics" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />

          <SectionLabel label="People" />
          <NavItem icon={<Users size={16}/>} label="Customers" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={16}/>} label="Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={16}/>} label="Approvals" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />

          <SectionLabel label="Operations" />
          <NavItem icon={<CreditCard size={16}/>} label="Withdrawals" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Navigation size={16}/>} label="Active jobs" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={16}/>} label="History" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />

          <SectionLabel label="Feedback" />
          <NavItem icon={<FileText size={16}/>} label="Reports" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />
          <NavItem icon={<Star size={16}/>} label="Reviews" active={activeTab==='reviews'} onClick={()=>setActiveTab('reviews')} />
          <NavItem icon={<MessageSquare size={16}/>} label="Support" active={activeTab==='support'} onClick={()=>setActiveTab('support')} />

          <SectionLabel label="Configuration" />
          <NavItem icon={<Tag size={16}/>} label="Categories" active={activeTab==='categories'} onClick={()=>setActiveTab('categories')} />
          <NavItem icon={<ImageIcon size={16}/>} label="Banners" active={activeTab==='banners'} onClick={()=>setActiveTab('banners')} />
          <NavItem icon={<Bell size={16}/>} label="Offers" active={activeTab==='offers'} onClick={()=>setActiveTab('offers')} />
          <NavItem icon={<Settings size={16}/>} label="Settings" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
        <div className="p-3 border-t border-white/10">
          <button onClick={()=>openAction('broadcast')} className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-[12.5px] rounded-lg flex items-center justify-center gap-2 transition-colors">
            <Send size={14}/> Broadcast message
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 shrink-0">
          <h2 className="text-[15px] font-semibold text-gray-900">{tabTitles[activeTab] || activeTab}</h2>
          <div className="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white font-semibold text-xs">A</div>
        </header>

        <main className="flex-1 overflow-y-auto custom-scrollbar bg-gray-50">
          {loading ? (
            <div className="flex flex-col items-center justify-center h-full gap-3">
              <div className="w-6 h-6 border-2 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
              <p className="text-gray-400 text-sm font-medium">Loading…</p>
            </div>
          ) : (
            <div className={activeTab !== 'monitor' ? 'p-6 max-w-[1400px] mx-auto' : 'h-full'}>
              {activeTab === 'monitor' && <MonitorView data={data.monitor} onRefresh={()=>fetchTabData('monitor')} selectedPartner={selectedPartner} setSelectedPartner={setSelectedPartner} />}
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('customers'); showToast("Updated");})} onDetails={(u)=>openAction('details', u)} title="Customers" />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('providers-all'); showToast("Updated");})} onDetails={(u)=>openAction('details', u)} title="Partners" isPartner />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>{fetchTabData('providers-pending'); showToast("Approved");})} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>{fetchTabData('withdrawals'); showToast("Processed");})} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Active jobs" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="Booking history" />}
              {activeTab === 'categories' && <CategoriesView categories={data.categories} refresh={()=>fetchTabData('categories')} />}
              {activeTab === 'offers' && <OffersView offers={data.offers} refresh={()=>fetchTabData('offers')} />}
              {activeTab === 'banners' && <BannersView banners={data.banners} refresh={()=>fetchTabData('banners')} />}
              {activeTab === 'reports' && <ReportsView reports={data.reports} />}
              {activeTab === 'reviews' && <ReviewsView reviews={data.reviews} onDelete={(id)=>adminApi.deleteReview(id).then(()=>{fetchTabData('reviews'); showToast("Deleted");})} />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>{fetchTabData('settings'); showToast("Saved");}} />}
              {activeTab === 'support' && <SupportView chats={data.chats} />}
            </div>
          )}
        </main>
      </div>

      {/* Slide-over modal */}
      {activeModal && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-gray-900/40" onClick={()=>setActiveModal(null)}></div>
          <div className="relative w-96 bg-white shadow-2xl h-full flex flex-col">
            <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
              <h3 className="font-semibold text-gray-900 text-[15px] capitalize">
                {activeModal === 'warning' && 'Send warning'}
                {activeModal === 'wallet' && 'Adjust wallet'}
                {activeModal === 'broadcast' && 'Broadcast message'}
                {activeModal === 'delete' && 'Delete account'}
                {activeModal === 'details' && 'Account details'}
              </h3>
              <button onClick={()=>setActiveModal(null)} className="p-1.5 hover:bg-gray-100 rounded-full text-gray-400 transition-colors"><X size={18}/></button>
            </div>
            <div className="flex-1 overflow-y-auto custom-scrollbar p-6 space-y-6">
              {selectedUser && activeModal !== 'broadcast' && (
                <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-xl border border-gray-100">
                  <img src={selectedUser.profileImage || 'https://via.placeholder.com/40'} className="w-11 h-11 rounded-full object-cover" />
                  <div>
                    <p className="font-semibold text-gray-900 text-sm">{selectedUser.name}</p>
                    <p className="text-[12px] text-gray-500">{selectedUser.phoneNumber}</p>
                  </div>
                </div>
              )}

              {activeModal === 'warning' && (
                <div className="space-y-3">
                  <div>
                    <label className="text-[12px] font-medium text-gray-500 mb-1.5 block">Title</label>
                    <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="e.g. Policy violation" className="w-full p-3 bg-gray-50 border border-gray-200 rounded-lg outline-none text-[13px] focus:border-indigo-400 focus:bg-white transition-colors" />
                  </div>
                  <div>
                    <label className="text-[12px] font-medium text-gray-500 mb-1.5 block">Message</label>
                    <textarea rows={7} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Describe what happened…" className="w-full p-3 bg-gray-50 border border-gray-200 rounded-lg outline-none text-[13px] resize-none focus:border-indigo-400 focus:bg-white transition-colors" />
                  </div>
                </div>
              )}

              {activeModal === 'wallet' && (
                <div className="space-y-5">
                  <div className="flex gap-2">
                    <button onClick={()=>setFormState({...formState, type: 'credit'})} className={`flex-1 py-2.5 rounded-lg font-semibold text-[12.5px] border transition-colors ${formState.type === 'credit' ? 'bg-emerald-600 border-emerald-600 text-white' : 'bg-gray-50 border-gray-200 text-gray-500'}`}>Add funds</button>
                    <button onClick={()=>setFormState({...formState, type: 'debit'})} className={`flex-1 py-2.5 rounded-lg font-semibold text-[12.5px] border transition-colors ${formState.type === 'debit' ? 'bg-red-600 border-red-600 text-white' : 'bg-gray-50 border-gray-200 text-gray-500'}`}>Deduct funds</button>
                  </div>
                  <div>
                    <label className="text-[12px] font-medium text-gray-500 mb-1.5 block">Amount</label>
                    <input type="number" value={formState.amount} onChange={e=>setFormState({...formState, amount: e.target.value})} placeholder="₹0" className="w-full p-4 bg-gray-50 border border-gray-200 rounded-xl outline-none font-semibold text-2xl text-center focus:border-indigo-400 focus:bg-white transition-colors" />
                  </div>
                  <div>
                    <label className="text-[12px] font-medium text-gray-500 mb-1.5 block">Note (optional)</label>
                    <input value={formState.description} onChange={e=>setFormState({...formState, description: e.target.value})} placeholder="Reason for this adjustment" className="w-full p-3 bg-gray-50 border border-gray-200 rounded-lg outline-none text-[13px] focus:border-indigo-400 focus:bg-white transition-colors" />
                  </div>
                </div>
              )}

              {activeModal === 'broadcast' && (
                <div className="space-y-3">
                  <div>
                    <label className="text-[12px] font-medium text-gray-500 mb-1.5 block">Send to</label>
                    <select value={formState.role} onChange={e=>setFormState({...formState, role: e.target.value})} className="w-full p-3 bg-gray-50 border border-gray-200 rounded-lg outline-none text-[13px] focus:border-indigo-400">
                      <option value="all">Everyone</option>
                      <option value="customer">Customers</option>
                      <option value="provider">Partners</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-[12px] font-medium text-gray-500 mb-1.5 block">Title</label>
                    <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="Notification title" className="w-full p-3 bg-gray-50 border border-gray-200 rounded-lg outline-none text-[13px] focus:border-indigo-400 focus:bg-white transition-colors" />
                  </div>
                  <div>
                    <label className="text-[12px] font-medium text-gray-500 mb-1.5 block">Message</label>
                    <textarea rows={6} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Write your announcement…" className="w-full p-3 bg-gray-50 border border-gray-200 rounded-lg outline-none text-[13px] resize-none focus:border-indigo-400 focus:bg-white transition-colors" />
                  </div>
                </div>
              )}

              {activeModal === 'delete' && (
                <p className="text-[13px] text-gray-600 leading-relaxed">This permanently removes the account and its data. This can't be undone.</p>
              )}

              {activeModal === 'details' && selectedUser && (
                <div className="space-y-8">
                  <div className="grid grid-cols-2 gap-3">
                    <div className="p-4 bg-indigo-50 rounded-xl border border-indigo-100">
                      <p className="text-indigo-500 text-[11px] font-medium mb-1">Earnings</p>
                      <p className="font-bold text-lg text-indigo-700">₹{selectedUser.totalEarnings || 0}</p>
                    </div>
                    <div className="p-4 bg-gray-900 rounded-xl text-white">
                      <p className="text-gray-400 text-[11px] font-medium mb-1">Jobs completed</p>
                      <p className="font-bold text-lg">{selectedUser.totalJobs || 0}</p>
                    </div>
                  </div>
                  <div className="space-y-3">
                    <h4 className="text-[12px] font-semibold text-gray-900 uppercase tracking-wide border-b border-gray-100 pb-2">Recent activity</h4>
                    <div className="space-y-2">
                      {selectedUser.activityLog?.slice(-8).reverse().map((log, i) => (
                        <div key={i} className="p-3 bg-gray-50 border border-gray-100 rounded-lg">
                          <div className="flex justify-between items-center mb-1">
                            <span className="text-[11px] font-semibold text-indigo-600 uppercase">{log.event}</span>
                            <span className="text-[10px] text-gray-400">{new Date(log.timestamp).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}</span>
                          </div>
                          <p className="text-[12px] text-gray-600">{log.description}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
            <div className="p-5 border-t border-gray-100">
              <button onClick={handleAction} className={`w-full py-3 rounded-xl font-semibold text-[13px] shadow-sm transition-colors active:scale-[0.98] ${activeModal === 'delete' ? 'bg-red-600 hover:bg-red-700 text-white' : 'bg-gray-900 hover:bg-gray-800 text-white'}`}>
                {activeModal === 'delete' ? 'Delete account' : activeModal === 'broadcast' ? 'Send broadcast' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function SectionLabel({ label }) {
  return <div className="pt-4 pb-1 px-3 text-[10px] font-semibold text-gray-500 uppercase tracking-wide">{label}</div>;
}

function NavItem({ icon, label, active, onClick, count }) {
  return (
    <button onClick={onClick} className={`w-full flex items-center justify-between px-3 py-2 rounded-lg transition-colors ${active ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:bg-white/5 hover:text-white'}`}>
      <div className="flex items-center gap-2.5">
        {icon}<span className="font-medium text-[13px]">{label}</span>
      </div>
      {count > 0 && <span className={`px-1.5 py-0.5 text-[10px] font-semibold rounded-full min-w-[18px] text-center ${active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'}`}>{count}</span>}
    </button>
  );
}

/* ---------- Live map ---------- */
const MonitorView = ({ data, onRefresh, selectedPartner, setSelectedPartner }) => {
  const [filter, setFilter] = useState('all');
  const filteredData = data.filter(p => filter === 'all' || p.status === filter);
  const counts = { total: data.length, online: data.filter(p => p.status === 'online').length, busy: data.filter(p => p.status === 'busy').length };

  return (
    <div className="p-6 space-y-5 h-full flex flex-col">
      <div className="flex justify-between items-end">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">Live partner tracking</h3>
          <p className="text-gray-500 text-[12.5px] mt-0.5">See where partners are and what they're doing right now</p>
        </div>
        <div className="flex gap-2">
          <StatusPill color="bg-emerald-500" label="Online" count={counts.online} />
          <StatusPill color="bg-indigo-500" label="Busy" count={counts.busy} />
        </div>
      </div>

      <div className="flex-1 flex gap-5 min-h-0">
        <div className="w-72 flex flex-col bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="p-2.5 border-b border-gray-100 flex gap-1.5 bg-gray-50">
            {['all', 'online', 'busy'].map(f => (
              <button key={f} onClick={()=>setFilter(f)} className={`flex-1 py-1.5 rounded-md text-[11px] font-semibold capitalize transition-colors ${filter===f ? 'bg-gray-900 text-white' : 'bg-white text-gray-500 border border-gray-200'}`}>{f}</button>
            ))}
          </div>
          <div className="flex-1 overflow-y-auto custom-scrollbar p-2 space-y-1">
            {filteredData.map(p => (
              <div key={p.uid} onClick={()=>setSelectedPartner(p)} className={`p-2.5 rounded-lg cursor-pointer transition-colors border ${selectedPartner?.uid === p.uid ? 'bg-indigo-50 border-indigo-200' : 'bg-transparent border-transparent hover:bg-gray-50'}`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <img src={p.profileImage || 'https://via.placeholder.com/24'} className="w-9 h-9 rounded-lg object-cover" />
                    <div>
                      <p className="font-semibold text-gray-800 text-[12.5px] truncate w-24">{p.name}</p>
                      <p className="text-[11px] text-gray-400">{p.phoneNumber}</p>
                    </div>
                  </div>
                  <div className={`w-2 h-2 rounded-full ${p.status === 'busy' ? 'bg-indigo-500' : 'bg-emerald-500'}`}></div>
                </div>
              </div>
            ))}
          </div>
          <div className="p-2.5 bg-gray-50 border-t border-gray-100 text-[11px] font-medium text-gray-400 text-center">{filteredData.length} partners shown</div>
        </div>

        <div className="flex-1 bg-white rounded-xl relative overflow-hidden border border-gray-200">
          <div className="absolute inset-0 z-0"><MapComponent partners={filteredData} selectedPartner={selectedPartner} /></div>
          <button onClick={onRefresh} className="absolute top-4 right-4 p-2.5 bg-white shadow-md rounded-lg z-10 hover:bg-gray-50 transition-colors border border-gray-100"><RefreshCw size={14}/></button>
          <div className="absolute bottom-4 left-4 p-3 bg-white/95 backdrop-blur rounded-xl shadow-md flex gap-5 border border-gray-100 z-10">
            <Legend color="bg-emerald-500" label="Online" /><Legend color="bg-indigo-600" label="Busy" /><Legend color="bg-gray-300" label="Offline" />
          </div>
        </div>
      </div>

      {selectedPartner && (
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-200 flex items-center justify-between gap-8">
          <div className="flex items-center gap-4 flex-shrink-0">
            <img src={selectedPartner.profileImage || 'https://via.placeholder.com/64'} className="w-14 h-14 rounded-xl object-cover" />
            <div>
              <div className="flex items-center gap-2.5">
                <h4 className="font-semibold text-gray-900 text-[15px]">{selectedPartner.name}</h4>
                <span className="px-2.5 py-0.5 bg-emerald-50 text-emerald-600 rounded-full text-[10px] font-semibold capitalize">{selectedPartner.status}</span>
              </div>
              <p className="font-medium text-gray-400 text-[12.5px] mt-0.5">{selectedPartner.phoneNumber}</p>
            </div>
          </div>
          <div className="h-10 w-px bg-gray-100"></div>
          <div>
            <p className="text-[11px] font-medium text-gray-400 mb-1">Current location</p>
            <p className="font-medium text-gray-700 text-[12.5px] font-mono">{selectedPartner.lat?.toFixed(5) || '0.00'}, {selectedPartner.lng?.toFixed(5) || '0.00'}</p>
          </div>
          <div className="flex flex-col gap-2 ml-auto">
            <button className="px-6 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-600 rounded-lg font-semibold text-[12px] transition-colors">Center on map</button>
            <button className="px-6 py-2 bg-gray-900 hover:bg-gray-800 text-white rounded-lg font-semibold text-[12px] transition-colors">Contact partner</button>
          </div>
        </div>
      )}
    </div>
  );
};

const StatusPill = ({ color, label, count }) => (
  <div className="px-3.5 py-2 bg-white border border-gray-200 rounded-full flex items-center gap-2.5">
    <div className={`w-2 h-2 rounded-full ${color}`}></div>
    <span className="text-[11.5px] font-medium text-gray-500">{label}</span>
    <span className="text-[12.5px] font-semibold text-gray-900">{count}</span>
  </div>
);

const Legend = ({ color, label }) => (
  <div className="flex items-center gap-2">
    <div className={`w-2.5 h-2.5 rounded-full ${color}`}></div>
    <span className="text-[11px] font-medium text-gray-500">{label}</span>
  </div>
);

/* ---------- Analytics ---------- */
const AnalyticsView = ({ data }) => !data ? null : (
  <div className="grid grid-cols-1 md:grid-cols-4 gap-4 animate-in fade-in duration-300">
    <StatCard title="Customers" value={data.totalUsers} color="text-indigo-600" />
    <StatCard title="Partners" value={data.totalProviders} color="text-emerald-600" />
    <StatCard title="Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} color="text-gray-900" />
    <StatCard title="Total jobs" value={data.totalBookings} color="text-blue-600" />
    <div className="col-span-full bg-white p-8 rounded-2xl border border-gray-200 mt-2">
      <h3 className="font-semibold text-gray-900 text-[14px] mb-8">Jobs by category</h3>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-x-16 gap-y-7">
        {data.categories?.map((cat, i) => (
          <div key={i}>
            <div className="flex justify-between items-end mb-2">
              <p className="text-[12.5px] font-medium text-gray-500">{cat.name}</p>
              <p className="text-[15px] font-semibold text-gray-900">{Math.round(cat.ratio * 100)}%</p>
            </div>
            <div className="w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
              <div className="bg-indigo-500 h-full rounded-full transition-all duration-700" style={{ width: `${cat.ratio * 100}%` }}></div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-5 rounded-2xl border border-gray-200 hover:shadow-md transition-shadow">
    <p className="text-[12px] font-medium text-gray-500 mb-1.5">{title}</p>
    <p className={`text-2xl font-bold tracking-tight ${color}`}>{value || 0}</p>
  </div>
);

/* ---------- Users table ---------- */
const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, onDetails, title }) => (
  <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden animate-in fade-in duration-300">
    <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center">
      <h3 className="font-semibold text-gray-900 text-[14px]">{title}</h3>
      <span className="px-3 py-1 bg-gray-50 rounded-full text-[11.5px] font-medium text-gray-500">{users?.length || 0} total</span>
    </div>
    <div className="overflow-x-auto custom-scrollbar">
      <table className="w-full text-left">
        <thead>
          <tr className="bg-gray-50/60 text-[11px] font-semibold text-gray-500 uppercase tracking-wide border-b border-gray-100">
            <th className="px-6 py-3">Name</th>
            <th className="px-6 py-3 text-center">Status</th>
            <th className="px-6 py-3 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {!users || users.length === 0 ? (
            <tr><td colSpan="3" className="px-6 py-24 text-center text-gray-300 font-medium text-[13px]">No records yet</td></tr>
          ) : (
            users.map(u => (
              <tr key={u.uid} className="hover:bg-gray-50/60 transition-colors group">
                <td className="px-6 py-3.5">
                  <div className="flex items-center gap-3 cursor-pointer" onClick={()=>onDetails(u)}>
                    <img src={u.profileImage || 'https://via.placeholder.com/32'} className="w-9 h-9 rounded-full object-cover" />
                    <div>
                      <p className="font-semibold text-gray-800 text-[13px]">{u.name || 'Unnamed user'}</p>
                      <p className="text-[11.5px] text-gray-400">{u.phoneNumber}</p>
                    </div>
                  </div>
                </td>
                <td className="px-6 py-3.5 text-center">
                  <span className={`px-3 py-1 rounded-full text-[11px] font-semibold ${u.isBlocked ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-600'}`}>{u.isBlocked ? 'Blocked' : 'Active'}</span>
                </td>
                <td className="px-6 py-3.5 text-right space-x-1.5">
                  <button title="Warn" onClick={()=>onWarn(u)} className="p-2 bg-amber-50 text-amber-500 rounded-lg hover:bg-amber-500 hover:text-white transition-colors"><AlertTriangle size={15}/></button>
                  <button title="Wallet" onClick={()=>onWallet(u)} className="p-2 bg-indigo-50 text-indigo-600 rounded-lg hover:bg-indigo-600 hover:text-white transition-colors"><Wallet size={15}/></button>
                  <button title={u.isBlocked ? 'Unblock' : 'Block'} onClick={()=>onBlock(u)} className={`p-2 rounded-lg transition-colors ${u.isBlocked ? 'bg-emerald-50 text-emerald-500 hover:bg-emerald-500 hover:text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-900 hover:text-white'}`}><ShieldAlert size={15}/></button>
                  <button title="Delete" onClick={()=>onDelete(u)} className="p-2 bg-red-50 text-red-400 rounded-lg hover:bg-red-600 hover:text-white transition-colors"><Trash2 size={15}/></button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

/* ---------- Pending approvals ---------- */
const PendingView = ({ providers, onApprove }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 animate-in fade-in duration-300">
    {providers.length === 0 ? (
      <div className="col-span-full p-20 bg-white rounded-2xl border border-dashed border-gray-200 text-center text-gray-300 font-medium text-[13px]">No pending approvals</div>
    ) : (
      providers.map(p => (
        <div key={p.uid} className="bg-white p-6 rounded-2xl border border-gray-200 relative hover:shadow-md transition-shadow text-center">
          <button onClick={()=>onApprove(p.uid)} className="absolute top-3.5 right-3.5 px-3 py-1.5 bg-indigo-600 text-white rounded-lg font-semibold text-[11px] hover:bg-indigo-700 active:scale-95 transition-all">Approve</button>
          <img src={p.profileImage || 'https://via.placeholder.com/100'} className="w-16 h-16 rounded-2xl object-cover ring-4 ring-gray-50 mx-auto mb-4" />
          <h4 className="font-semibold text-[14px] text-gray-900">{p.name}</h4>
          <p className="text-[12px] font-medium text-indigo-500 mt-1 mb-5">{p.profession}</p>
          <div className="p-3 bg-gray-50 rounded-xl border border-gray-100 flex justify-between items-center">
            <p className="text-[11px] font-medium text-gray-400">ID: <span className="text-gray-700 font-semibold">{p.aadhaarNumber}</span></p>
            <Eye size={16} className="text-indigo-400 cursor-pointer hover:scale-110 transition-transform"/>
          </div>
        </div>
      ))
    )}
  </div>
);

/* ---------- Withdrawals ---------- */
const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden animate-in fade-in duration-300">
    <div className="px-6 py-4 border-b border-gray-100 font-semibold text-gray-900 text-[14px]">Withdrawal requests</div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead>
          <tr className="bg-gray-50/60 text-[11px] font-semibold text-gray-500 uppercase tracking-wide border-b border-gray-100">
            <th className="px-6 py-3">Partner</th>
            <th className="px-6 py-3 text-right">Amount &amp; action</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {withdrawals.length === 0 ? (
            <tr><td colSpan="2" className="px-6 py-24 text-center text-gray-300 font-medium text-[13px]">No pending requests</td></tr>
          ) : (
            withdrawals.map(w => (
              <tr key={w._id} className="hover:bg-gray-50/60 transition-colors">
                <td className="px-6 py-5 font-semibold text-gray-900 text-[15px]">{w.providerName}</td>
                <td className="px-6 py-5 text-right space-x-5">
                  <span className="text-indigo-600 font-bold text-xl">₹{w.amount}</span>
                  <button onClick={()=>onHandle(w._id, 'approved')} className="px-5 py-2 bg-gray-900 text-white text-[12px] font-semibold rounded-lg hover:bg-indigo-600 active:scale-95 transition-all">Approve payout</button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

/* ---------- Jobs / bookings ---------- */
const JobsTable = ({ jobs, title }) => (
  <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden animate-in fade-in duration-300">
    <div className="px-6 py-4 border-b border-gray-100 font-semibold text-gray-900 text-[14px] flex justify-between items-center">
      <span>{title}</span>
      <span className="px-3 py-1 bg-indigo-50 text-indigo-600 rounded-full font-medium text-[11.5px]">{jobs?.length || 0} total</span>
    </div>
    <table className="w-full text-left border-collapse">
      <thead>
        <tr className="bg-gray-50/60 text-[11px] font-semibold text-gray-500 uppercase tracking-wide border-b border-gray-100">
          <th className="px-6 py-3">Service</th>
          <th className="px-6 py-3">Status</th>
          <th className="px-6 py-3 text-right">Amount</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-gray-50">
        {(!jobs || jobs.length === 0) ? (
          <tr><td colSpan="3" className="px-6 py-24 text-center text-gray-300 font-medium text-[13px]">Nothing here yet</td></tr>
        ) : (
          jobs.map(j => (
            <tr key={j._id} className="hover:bg-gray-50/60 transition-colors group">
              <td className="px-6 py-4">
                <p className="font-semibold text-gray-800 text-[13.5px] group-hover:text-indigo-600 transition-colors">{j.serviceName}</p>
                <p className="text-[11px] font-medium text-gray-400 mt-0.5">{new Date(j.createdAt).toLocaleDateString()}</p>
              </td>
              <td className="px-6 py-4">
                <span className={`px-3 py-1 rounded-full text-[11px] font-semibold capitalize ${j.status === 'done' ? 'bg-emerald-50 text-emerald-600' : 'bg-indigo-50 text-indigo-600'}`}>{j.status.replace('_',' ')}</span>
              </td>
              <td className="px-6 py-4 text-right font-bold text-[15px] text-gray-900">₹{j.totalAmount}</td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  </div>
);

/* ---------- Categories ---------- */
const CategoriesView = ({ categories, refresh }) => {
  const [n, setN] = useState(''); const [i, setI] = useState('');
  const handleAdd = () => adminApi.addCategory({name:n, icon:i}).then(()=>{setN('');setI('');refresh();}).catch(()=>{});
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 animate-in fade-in duration-300">
      <div className="bg-white p-7 rounded-2xl border border-gray-200 h-fit">
        <h3 className="font-semibold mb-6 text-[14px] text-gray-900">Add category</h3>
        <div className="space-y-3">
          <input value={n} onChange={e=>setN(e.target.value)} placeholder="Category name" className="w-full p-3 bg-gray-50 border border-gray-200 rounded-lg font-medium outline-none focus:border-indigo-400 text-[13px]" />
          <input value={i} onChange={e=>setI(e.target.value)} placeholder="Icon URL" className="w-full p-3 bg-gray-50 border border-gray-200 rounded-lg font-medium outline-none focus:border-indigo-400 text-[13px]" />
          <button onClick={handleAdd} className="w-full py-3 bg-indigo-600 text-white font-semibold rounded-lg text-[13px] hover:bg-indigo-700 active:scale-[0.98] transition-all">Add category</button>
        </div>
      </div>
      <div className="lg:col-span-2 grid grid-cols-2 md:grid-cols-3 gap-5">
        {categories.map(c => (
          <div key={c._id} className="bg-white p-6 rounded-2xl border border-gray-200 flex flex-col items-center hover:shadow-md transition-shadow group relative">
            <div className="w-16 h-16 bg-gray-50 rounded-2xl flex items-center justify-center mb-4"><img src={c.icon || 'https://via.placeholder.com/80'} className="w-10 h-10 object-contain" /></div>
            <p className="font-semibold text-gray-800 text-[12.5px] text-center">{c.name}</p>
            <button onClick={()=>adminApi.deleteCategory(c._id).then(refresh)} className="absolute top-3 right-3 p-2 bg-red-50 text-red-400 rounded-full opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-500 hover:text-white"><Trash2 size={14}/></button>
          </div>
        ))}
      </div>
    </div>
  );
};

/* ---------- Offers ---------- */
const OffersView = ({ offers, refresh }) => (
  <div className="grid grid-cols-1 md:grid-cols-3 gap-5 animate-in fade-in duration-300">
    {offers.map(o => (
      <div key={o._id} className="bg-white p-6 rounded-2xl border border-gray-200 relative group hover:shadow-md transition-shadow text-center">
        <div className="absolute top-0 right-0 px-4 py-1.5 bg-indigo-600 text-white font-semibold text-[10.5px] rounded-bl-xl rounded-tr-2xl">{o.code}</div>
        <h4 className="text-3xl font-bold text-indigo-600 mb-1 mt-3">{o.discount}% off</h4>
        <p className="font-semibold text-gray-800 text-[13px]">{o.title}</p>
        <p className="text-[11.5px] text-gray-400 mt-3">Expires {new Date(o.expiryDate).toLocaleDateString()}</p>
        <div className="mt-5 flex justify-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <button onClick={()=>adminApi.deleteOffer(o._id).then(refresh)} className="p-2.5 bg-red-50 text-red-400 rounded-lg hover:bg-red-500 hover:text-white transition-colors"><Trash2 size={16}/></button>
        </div>
      </div>
    ))}
    <button className="bg-white border-2 border-dashed border-gray-200 rounded-2xl p-6 flex flex-col items-center justify-center text-gray-300 hover:text-indigo-400 hover:border-indigo-200 transition-colors">
      <Plus size={28} strokeWidth={2.5} />
      <span className="font-semibold text-[12px] mt-2.5">New offer</span>
    </button>
  </div>
);

/* ---------- Banners ---------- */
const BannersView = ({ banners, refresh }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-in fade-in duration-300">
    {banners.map(b => (
      <div key={b._id} className="bg-white rounded-2xl border border-gray-200 overflow-hidden group hover:shadow-md transition-shadow relative">
        <img src={b.image} className="h-48 w-full object-cover" />
        <div className="p-5">
          <h4 className="font-semibold text-gray-900 text-[14px]">{b.title}</h4>
          <p className="text-gray-500 text-[12.5px] mt-1">{b.subtitle}</p>
          <button onClick={()=>adminApi.deleteBanner(b._id).then(refresh)} className="absolute top-4 right-4 p-2.5 bg-white text-red-400 rounded-full opacity-0 group-hover:opacity-100 transition-all hover:bg-red-500 hover:text-white shadow-md"><Trash2 size={16}/></button>
        </div>
      </div>
    ))}
    <button className="bg-indigo-600 rounded-2xl min-h-[220px] flex flex-col items-center justify-center text-white hover:bg-indigo-500 transition-colors">
      <div className="p-5 bg-white/10 rounded-full"><Plus size={28} strokeWidth={2.5}/></div>
      <span className="font-semibold text-[12.5px] mt-4">Add banner</span>
    </button>
  </div>
);

/* ---------- Reports ---------- */
const ReportsView = ({ reports }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-in fade-in duration-300">
    {reports.length === 0 ? (
      <div className="col-span-full p-20 bg-white rounded-2xl border border-dashed border-gray-200 text-center text-gray-300 font-medium text-[13px]">No open reports</div>
    ) : reports.map(r => (
      <div key={r._id} className="bg-white p-7 rounded-2xl border-l-4 border-red-500 border border-gray-200 relative">
        <span className="absolute top-4 right-4 px-3 py-1 bg-red-50 text-red-600 text-[10.5px] font-semibold rounded-full capitalize">{r.status}</span>
        <h4 className="font-semibold text-gray-900 text-[15px] mb-4 pr-20">{r.reason}</h4>
        <div className="bg-gray-50 p-4 rounded-xl mb-5 border border-gray-100 text-gray-600 text-[12.5px] leading-relaxed">{r.description}</div>
        <div className="flex gap-3 overflow-x-auto pb-2 custom-scrollbar">
          {r.evidenceUrls?.map((u, i) => (
            <img key={i} src={u} className="w-20 h-20 rounded-xl border border-gray-100 object-cover flex-shrink-0 cursor-zoom-in hover:scale-105 transition-transform" />
          ))}
        </div>
      </div>
    ))}
  </div>
);

/* ---------- Reviews ---------- */
const ReviewsView = ({ reviews, onDelete }) => (
  <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden animate-in fade-in duration-300">
    <div className="px-6 py-4 border-b border-gray-100 font-semibold text-gray-900 text-[14px]">Customer reviews</div>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="bg-gray-50/60 text-[11px] font-semibold text-gray-500 uppercase tracking-wide border-b border-gray-100">
            <th className="px-6 py-3 text-left">Customer</th>
            <th className="px-6 py-3 text-left">Review</th>
            <th className="px-6 py-3 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {reviews.map(r => (
            <tr key={r._id} className="hover:bg-gray-50/60 transition-colors group">
              <td className="px-6 py-5 align-top">
                <p className="font-semibold text-gray-900 text-[13.5px]">{r.customerName}</p>
                <div className="flex gap-0.5 mt-1.5">
                  {[...Array(5)].map((_, i) => <Star key={i} size={13} className={i < r.rating ? 'text-amber-400 fill-amber-400' : 'text-gray-200'} />)}
                </div>
              </td>
              <td className="px-6 py-5 align-top"><p className="text-gray-600 text-[13px] leading-relaxed max-w-md">{r.comment}</p></td>
              <td className="px-6 py-5 text-right align-top"><button onClick={()=>onDelete(r._id)} className="p-2 bg-red-50 text-red-400 rounded-lg hover:bg-red-600 hover:text-white transition-colors"><Trash2 size={15}/></button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
);

/* ---------- Settings ---------- */
const SettingsView = ({ settings, refresh }) => {
  const [c, setC] = useState(settings.commission_percentage || 15);
  return (
    <div className="bg-white p-10 rounded-2xl border border-gray-200 max-w-xl mx-auto animate-in fade-in duration-300">
      <h3 className="font-semibold text-[15px] text-gray-900 mb-8">Platform settings</h3>
      <div className="flex justify-between items-center gap-8 mb-8">
        <div>
          <p className="font-medium text-gray-800 text-[13.5px]">Commission rate</p>
          <p className="text-gray-400 text-[12px] mt-1 leading-relaxed">Percentage the platform keeps from every completed job</p>
        </div>
        <div className="flex items-center bg-gray-50 border border-gray-200 rounded-xl px-3">
          <input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-16 py-3 text-center bg-transparent font-bold text-xl text-indigo-600 outline-none" />
          <span className="font-semibold text-gray-400 text-lg">%</span>
        </div>
      </div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(refresh)} className="w-full py-3 bg-indigo-600 text-white font-semibold rounded-xl text-[13.5px] hover:bg-indigo-700 active:scale-[0.98] transition-all">Save changes</button>
    </div>
  );
};

/* ---------- Support ---------- */
const SupportView = ({ chats }) => (
  <div className="bg-white rounded-2xl border border-gray-200 h-[600px] flex flex-col items-center justify-center text-gray-300 gap-3">
    <MessageSquare size={32} strokeWidth={1.5} />
    <p className="font-medium text-[13px]">Support chat isn't connected yet</p>
  </div>
);