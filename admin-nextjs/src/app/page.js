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

// Dynamic import for Leaflet map to avoid SSR issues in Next.js
const MapComponent = dynamic(() => import('@/components/MapComponent'), {
  ssr: false,
  loading: () => <div className="h-full w-full bg-slate-50 flex items-center justify-center font-semibold text-slate-300 uppercase tracking-widest text-[10px]">Initializing Operational Grid...</div>
});

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
        case 'monitor':
          res = await adminApi.getMonitor();
          setData(p => ({...p, monitor: res.data}));
          if(res.data.length > 0 && !selectedPartner) setSelectedPartner(res.data[0]);
          break;
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
    } catch (err) { setError("Data Synchronization Error"); }
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
      showToast("Operation Success"); setActiveModal(null);
    } catch (e) { showToast("Operation Failed", "error"); }
  };

  return (
    <div className="flex h-screen bg-slate-100 text-slate-800 overflow-hidden font-sans text-[12px] antialiased">
      {toast && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-[100] animate-in fade-in duration-200">
            <div className={`px-4 py-2 rounded-xl shadow-xl flex items-center gap-2 border bg-white ${toast.type === 'success' ? 'border-emerald-500 text-emerald-600' : 'border-red-500 text-red-600'}`}>
                <Check size={14} />
                <span className="font-bold uppercase text-[10px] tracking-wider">{toast.message}</span>
            </div>
        </div>
      )}

      <aside className="w-56 bg-[#020617] text-white flex flex-col shrink-0 z-20 shadow-2xl">
        <div className="h-16 flex items-center px-6 border-b border-white/5 font-extrabold text-xl tracking-tight">MADADWALA</div>
        <nav className="flex-1 px-2 py-4 space-y-0.5 overflow-y-auto custom-scrollbar">
          <NavItem icon={<Activity size={16}/>} label="Mission Control" active={activeTab==='monitor'} onClick={()=>setActiveTab('monitor')} />
          <NavItem icon={<BarChart3 size={16}/>} label="Global Analytics" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <div className="pt-5 pb-1.5 px-3 text-[9px] font-bold text-slate-500 uppercase tracking-[2px]">Core Resources</div>
          <NavItem icon={<Users size={16}/>} label="Consumers" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={16}/>} label="Verified Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={16}/>} label="Approval Queue" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />

          <div className="pt-5 pb-1.5 px-3 text-[9px] font-bold text-slate-500 uppercase tracking-[2px]">Operations</div>
          <NavItem icon={<CreditCard size={16}/>} label="Financial Payouts" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Navigation size={16}/>} label="Active Live Jobs" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={16}/>} label="Service History" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />

          <div className="pt-5 pb-1.5 px-3 text-[9px] font-bold text-slate-500 uppercase tracking-[2px]">Governance</div>
          <NavItem icon={<FileText size={16}/>} label="User Reports" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />
          <NavItem icon={<Star size={16}/>} label="Feedback Data" active={activeTab==='reviews'} onClick={()=>setActiveTab('reviews')} />
          <NavItem icon={<MessageSquare size={16}/>} label="Support Bridge" active={activeTab==='support'} onClick={()=>setActiveTab('support')} />

          <div className="pt-5 pb-1.5 px-3 text-[9px] font-bold text-slate-500 uppercase tracking-[2px]">Engine Config</div>
          <NavItem icon={<Tag size={16}/>} label="Categories" active={activeTab==='categories'} onClick={()=>setActiveTab('categories')} />
          <NavItem icon={<ImageIcon size={16}/>} label="Home Banners" active={activeTab==='banners'} onClick={()=>setActiveTab('banners')} />
          <NavItem icon={<Settings size={16}/>} label="Platform Rules" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
        <div className="p-4"><button onClick={()=>openAction('broadcast')} className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-[10px] uppercase rounded-lg shadow-lg flex items-center justify-center gap-2"><Send size={14}/> Universal Broadcast</button></div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0 bg-white shadow-inner">
        <header className="h-14 bg-white border-b border-slate-100 flex items-center justify-between px-8 shrink-0 z-10">
          <h2 className="text-[11px] font-bold text-slate-400 uppercase tracking-[4px]">{activeTab.replace('-', ' ')}</h2>
          <div className="flex items-center gap-4">
              {error && <div className="text-red-500 text-[9px] font-bold bg-red-50 px-3 py-1 rounded border border-red-100 uppercase animate-pulse">{error}</div>}
              <div className="w-8 h-8 rounded-lg bg-slate-950 flex items-center justify-center text-white font-bold text-[11px] shadow-lg">AG</div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-0 bg-slate-50/30">
          {loading ? (
             <div className="flex flex-col items-center justify-center h-full gap-3">
               <div className="w-6 h-6 border-2 border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div>
               <p className="text-slate-400 font-bold uppercase text-[9px] tracking-widest">Processing Data...</p>
             </div>
          ) : (
            <div className={`h-full ${activeTab !== 'monitor' ? 'p-8 max-w-[1400px] mx-auto' : ''}`}>
              {activeTab === 'monitor' && <MonitorView data={data.monitor} onRefresh={()=>fetchTabData('monitor')} selectedPartner={selectedPartner} setSelectedPartner={setSelectedPartner} />}
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('customers'); showToast("Status Changed");})} onDetails={(u)=>openAction('details', u)} title="Customers" />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('providers-all'); showToast("Status Changed");})} onDetails={(u)=>openAction('details', u)} title="Partners" isPartner />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>{fetchTabData('providers-pending'); showToast("Credentials Verified");})} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>{fetchTabData('withdrawals'); showToast("Transfer Confirmed");})} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Live Tracking" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="Platform Archives" />}
              {activeTab === 'categories' && <CategoriesView categories={data.categories} refresh={()=>fetchTabData('categories')} />}
              {activeTab === 'offers' && <OffersView offers={data.offers} refresh={()=>fetchTabData('offers')} />}
              {activeTab === 'banners' && <BannersView banners={data.banners} refresh={()=>fetchTabData('banners')} />}
              {activeTab === 'reports' && <ReportsView reports={data.reports} />}
              {activeTab === 'reviews' && <ReviewsView reviews={data.reviews} onDelete={(id)=>adminApi.deleteReview(id).then(()=>{fetchTabData('reviews'); showToast("Purged");})} />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>{fetchTabData('settings'); showToast("Core Synced");}} />}
              {activeTab === 'support' && <SupportView chats={data.chats} />}
            </div>
          )}
        </main>
      </div>

      {/* Action Slide Panel */}
      {activeModal && (
          <div className="fixed inset-0 z-50 overflow-hidden flex justify-end">
              <div className="absolute inset-0 bg-slate-950/20 backdrop-blur-[2px]" onClick={()=>setActiveModal(null)}></div>
              <div className="relative w-80 bg-white shadow-2xl h-full flex flex-col border-l border-slate-100 animate-in slide-in-from-right duration-200">
                  <div className="px-6 py-5 border-b border-slate-50 flex items-center justify-between">
                      <h3 className="font-bold text-slate-900 text-sm uppercase tracking-widest">{activeModal}</h3>
                      <button onClick={()=>setActiveModal(null)} className="p-1.5 hover:bg-slate-50 rounded-full text-slate-400"><X size={18}/></button>
                  </div>
                  <div className="flex-1 overflow-y-auto p-5 space-y-6 custom-scrollbar">
                      {selectedUser && activeModal !== 'broadcast' && (
                          <div className="flex items-center gap-3 p-4 bg-slate-50 rounded-xl border border-slate-100">
                              <img src={selectedUser.profileImage || 'https://via.placeholder.com/40'} className="w-10 h-10 rounded-lg object-cover shadow-sm" />
                              <div><p className="font-bold text-slate-900 text-xs">{selectedUser.name || 'Anonymous'}</p><p className="text-[10px] text-slate-400 font-medium">{selectedUser.phoneNumber}</p></div>
                          </div>
                      )}

                      {activeModal === 'warning' && (
                          <div className="space-y-4">
                              <label className="text-[9px] font-bold text-slate-400 uppercase tracking-widest ml-1">Class</label>
                              <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="e.g. Behavioral Misconduct" className="w-full p-3 bg-slate-50 border border-slate-200 rounded-lg outline-none font-bold text-[11px] focus:border-indigo-500" />
                              <label className="text-[9px] font-bold text-slate-400 uppercase tracking-widest ml-1">Debrief</label>
                              <textarea rows={6} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Incident report..." className="w-full p-3 bg-slate-50 border border-slate-200 rounded-lg outline-none font-medium text-[11px] resize-none focus:border-indigo-500" />
                          </div>
                      )}

                      {activeModal === 'wallet' && (
                          <div className="space-y-6">
                              <div className="flex gap-2">
                                  <button onClick={()=>setFormState({...formState, type: 'credit'})} className={`flex-1 py-2.5 rounded-lg font-bold text-[10px] border transition-all ${formState.type === 'credit' ? 'bg-emerald-600 border-emerald-600 text-white shadow-md' : 'bg-slate-50 border-transparent text-slate-400'}`}>Inject (+)</button>
                                  <button onClick={()=>setFormState({...formState, type: 'debit'})} className={`flex-1 py-2.5 rounded-lg font-bold text-[10px] border transition-all ${formState.type === 'debit' ? 'bg-red-600 border-red-600 text-white shadow-md' : 'bg-slate-50 border-transparent text-slate-400'}`}>Extract (-)</button>
                              </div>
                              <div className="space-y-2">
                                  <label className="text-[9px] font-bold text-slate-400 uppercase tracking-widest ml-1">Asset Value</label>
                                  <input type="number" value={formState.amount} onChange={e=>setFormState({...formState, amount: e.target.value})} placeholder="₹ 0.00" className="w-full p-4 bg-slate-50 border border-slate-200 rounded-xl outline-none font-bold text-2xl text-center" />
                              </div>
                          </div>
                      )}

                      {activeModal === 'broadcast' && (
                          <div className="space-y-4">
                              <label className="text-[9px] font-bold text-slate-400 uppercase tracking-widest ml-1">Node Target</label>
                              <select value={formState.role} onChange={e=>setFormState({...formState, role: e.target.value})} className="w-full p-3 bg-slate-50 border border-slate-200 rounded-lg outline-none font-bold text-[11px]">
                                  <option value="all">Universal (All Nodes)</option>
                                  <option value="customer">Consumer Class</option>
                                  <option value="provider">Partner Fleet</option>
                              </select>
                              <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="Announcement Headline" className="w-full p-3 bg-slate-50 border border-slate-200 rounded-lg outline-none font-bold text-[11px]" />
                              <textarea rows={6} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Mission body..." className="w-full p-3 bg-slate-50 border border-slate-200 rounded-lg outline-none font-medium text-[11px] resize-none" />
                          </div>
                      )}

                      {activeModal === 'details' && selectedUser && (
                          <div className="space-y-8">
                              <div className="grid grid-cols-2 gap-3 text-center text-[10px]">
                                  <div className="p-4 bg-indigo-50 rounded-2xl border border-indigo-100 shadow-sm"><p className="text-indigo-400 font-bold mb-0.5 uppercase tracking-widest text-[8px]">Revenue</p><p className="font-bold text-sm text-indigo-600 tracking-tight">₹{selectedUser.totalEarnings || 0}</p></div>
                                  <div className="p-4 bg-slate-900 rounded-2xl text-white shadow-md shadow-slate-200"><p className="opacity-60 font-bold mb-0.5 uppercase tracking-widest text-[8px]">Jobs</p><p className="font-bold text-sm">{selectedUser.totalJobs || 0}</p></div>
                              </div>
                              <div className="space-y-4">
                                  <h4 className="text-[9px] font-bold text-slate-400 uppercase tracking-[3px] border-b pb-2">Activity Pulse</h4>
                                  <div className="space-y-3">
                                      {selectedUser.activityLog?.slice(-8).reverse().map((log, i) => (
                                          <div key={i} className="p-3 bg-white border border-slate-100 rounded-xl shadow-sm">
                                              <div className="flex justify-between items-center mb-1"><span className="text-[10px] font-bold text-indigo-600 uppercase tracking-tighter">{log.event}</span><span className="text-[8px] text-slate-300">{new Date(log.timestamp).toLocaleDateString()}</span></div>
                                              <p className="text-[10px] text-slate-500 font-medium leading-tight">"{log.description}"</p>
                                          </div>
                                      ))}
                                  </div>
                              </div>
                          </div>
                      )}
                  </div>
                  <div className="p-6 border-t border-slate-50 bg-white">
                      {activeModal !== 'details' ? (
                          <button onClick={handleAction} className={`w-full py-4 rounded-xl font-bold uppercase text-[10px] tracking-[4px] shadow-xl transition-all ${activeModal === 'delete' ? 'bg-red-600 text-white shadow-red-200' : 'bg-slate-950 text-white shadow-slate-200'}`}>Execute command</button>
                      ) : (
                          <button onClick={()=>setActiveModal(null)} className="w-full py-4 bg-slate-100 text-slate-500 rounded-xl font-bold uppercase text-[10px] tracking-[2px] hover:bg-slate-200 transition-colors">Dismiss panel</button>
                      )}
                  </div>
              </div>
          </div>
      )}
    </div>
  );
}

function NavItem({ icon, label, active, onClick, count, pulse }) {
  return (
    <button onClick={onClick} className={`w-full flex items-center justify-between px-4 py-2.5 rounded-xl transition-all ${active ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}>
      <div className="flex items-center space-x-3">
        {pulse && !active && <div className="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-ping absolute -ml-0.5"></div>}
        {icon}<span className="font-semibold text-[13px]">{label}</span>
      </div>
      {count > 0 && <span className={`px-1.5 py-0.5 text-[9px] font-bold rounded ${active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'}`}>{count}</span>}
    </button>
  );
}

const MonitorView = ({ data, onRefresh, selectedPartner, setSelectedPartner }) => {
  const [filter, setFilter] = useState('all');
  const filteredData = data.filter(p => filter === 'all' || p.status === filter);
  const counts = { total: data.length, online: data.filter(p => p.status === 'online').length, busy: data.filter(p => p.status === 'busy').length };

  return (
    <div className="p-6 space-y-6 h-full flex flex-col font-sans">
        <div className="flex justify-between items-end">
            <div><h3 className="text-2xl font-bold text-slate-900 flex items-center gap-3">Live Partner Tracking<span className="flex items-center gap-1.5 text-[9px] font-bold text-emerald-500 uppercase tracking-widest bg-emerald-50 px-2.5 py-1 rounded-full"><div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></div> Real-time</span></h3><p className="text-slate-400 font-medium text-[9px] uppercase mt-0.5 tracking-wider">Mission Control V1.0</p></div>
            <div className="flex gap-2">
                <StatusPill color="bg-emerald-500" label="Available" count={counts.online} />
                <StatusPill color="bg-indigo-500" label="Operational" count={counts.busy} />
            </div>
        </div>

        <div className="flex-1 flex gap-6 min-h-0">
            <div className="w-72 flex flex-col bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
                <div className="p-3 border-b border-slate-100 flex gap-1 bg-slate-50/50">
                    {['all', 'online', 'busy'].map(f => (
                        <button key={f} onClick={()=>setFilter(f)} className={`flex-1 py-1.5 rounded-lg text-[8px] font-bold uppercase transition-all ${filter===f ? 'bg-slate-900 text-white shadow-md' : 'bg-white text-slate-400 border border-slate-200'}`}>{f}</button>
                    ))}
                </div>
                <div className="flex-1 overflow-y-auto custom-scrollbar p-2 space-y-1">
                    {filteredData.map(p => (
                        <div key={p.uid} onClick={()=>setSelectedPartner(p)} className={`p-3 rounded-xl cursor-pointer transition-all border ${selectedPartner?.uid === p.uid ? 'bg-indigo-50 border-indigo-200 ring-2 ring-indigo-50' : 'bg-transparent border-transparent hover:bg-slate-50'}`}>
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <img src={p.profileImage || 'https://via.placeholder.com/24'} className="w-8 h-8 rounded-lg object-cover shadow-sm border border-white" />
                                    <div><p className="font-bold text-slate-800 text-[11px] uppercase truncate w-24 tracking-tighter">{p.name}</p><p className="text-[8px] font-bold text-slate-400 uppercase tracking-tighter">{p.phoneNumber}</p></div>
                                </div>
                                <div className={`w-1.5 h-1.5 rounded-full ${p.status === 'busy' ? 'bg-indigo-500' : 'bg-emerald-500 shadow-[0_0_5px_rgba(16,185,129,0.5)]'}`}></div>
                            </div>
                        </div>
                    ))}
                </div>
                <div className="p-2 bg-slate-50 border-t border-slate-100 text-[8px] font-bold text-slate-400 text-center uppercase tracking-widest">{filteredData.length} Nodes Active</div>
            </div>

            <div className="flex-1 bg-white rounded-3xl relative overflow-hidden border border-slate-200 shadow-xl shadow-slate-100">
                <div className="absolute inset-0 z-0"><MapComponent partners={filteredData} selectedPartner={selectedPartner} /></div>
                <div className="absolute top-4 right-4 flex flex-col gap-2 z-10">
                    <button onClick={onRefresh} className="px-4 py-2 bg-white shadow-lg rounded-xl flex items-center gap-2 font-bold text-[9px] uppercase border border-slate-100 transition-all hover:bg-slate-50"><RefreshCw size={12}/> Refresh</button>
                </div>
                <div className="absolute bottom-4 right-4 z-10"><button className="p-3 bg-slate-950 shadow-2xl rounded-xl text-white hover:bg-indigo-600 transition-colors shadow-indigo-200"><Navigation size={18}/></button></div>
                <div className="absolute bottom-4 left-4 p-3 bg-white/90 backdrop-blur-md rounded-2xl shadow-xl flex gap-6 border border-white/50 z-10">
                    <Legend color="bg-emerald-500" label="Online" /><Legend color="bg-indigo-600" label="Busy" /><Legend color="bg-slate-300" label="Offline" />
                </div>
            </div>
        </div>

        {selectedPartner && (
            <div className="bg-white p-6 rounded-[32px] shadow-2xl border border-slate-100 flex items-center justify-between gap-10 animate-in slide-in-from-bottom-2 duration-300">
                <div className="flex items-center gap-6 flex-shrink-0">
                    <img src={selectedPartner.profileImage || 'https://via.placeholder.com/80'} className="w-16 h-16 rounded-2xl object-cover shadow-2xl ring-4 ring-slate-50" />
                    <div><div className="flex items-center gap-3"><h4 className="font-bold text-slate-900 text-lg uppercase tracking-tight">{selectedPartner.name}</h4><span className="px-3 py-1 bg-emerald-50 text-emerald-600 rounded-lg text-[8px] font-bold uppercase tracking-widest">{selectedPartner.status}</span></div><p className="font-bold text-slate-400 text-xs mt-1">{selectedPartner.phoneNumber}</p></div>
                </div>
                <div className="h-12 w-px bg-slate-100"></div>
                <div><p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest mb-1.5 opacity-50 italic">Coordination Link</p><p className="font-bold text-slate-800 text-xs tracking-widest font-mono">{selectedPartner.lat?.toFixed(5)}, {selectedPartner.lng?.toFixed(5)}</p><p className="text-[8px] font-bold text-emerald-500 mt-1 uppercase">Active Link Verified</p></div>
                <div className="flex flex-col gap-1.5"><button className="px-10 py-2.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-600 rounded-xl font-bold text-[10px] uppercase tracking-widest transition-all border border-indigo-100">Center Node</button><button className="px-10 py-2.5 bg-slate-950 hover:bg-slate-800 text-white rounded-xl font-bold text-[10px] uppercase tracking-widest shadow-xl transition-all">Direct Comms</button></div>
            </div>
        )}
    </div>
  );
};

const StatusPill = ({ color, label, count }) => (
    <div className="px-4 py-2 bg-white border border-slate-200 rounded-full shadow-sm flex items-center gap-3"><div className={`w-1.5 h-1.5 rounded-full ${color}`}></div><span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{label}</span><span className="text-xs font-bold text-slate-900">{count}</span></div>
);

const AnalyticsView = ({ data }) => !data ? null : (
  <div className="grid grid-cols-1 md:grid-cols-4 gap-4 animate-in fade-in duration-300">
    <StatCard title="Consumers" value={data.totalUsers} color="text-indigo-600" />
    <StatCard title="Market Partners" value={data.totalProviders} color="text-emerald-600" />
    <StatCard title="Gross Platform Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} color="text-slate-950" />
    <StatCard title="Transaction Archive" value={data.totalBookings} color="text-blue-600" />
    <div className="col-span-full bg-white p-10 rounded-[32px] border border-slate-200 shadow-sm mt-4">
        <h3 className="font-bold text-slate-800 uppercase tracking-[4px] text-[10px] mb-12 text-center opacity-40 italic">Global Resource Matrix</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-20 gap-y-10">
            {data.categories?.map((cat, i) => (
                <div key={i}>
                    <div className="flex justify-between items-end mb-2.5"><p className="text-[10px] font-bold text-slate-500 uppercase tracking-[2px]">{cat.name}</p><p className="text-xl font-bold text-slate-900">{Math.round(cat.ratio * 100)}%</p></div>
                    <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden"><div className="bg-indigo-500 h-full rounded-full transition-all duration-1000 shadow-sm" style={{ width: `${cat.ratio * 100}%` }}></div></div>
                </div>
            ))}
        </div>
    </div>
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-xl transition-all group">
    <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest mb-1.5 opacity-60 group-hover:text-indigo-500 transition-colors">{title}</p>
    <p className={`text-2xl font-bold tracking-tighter ${color}`}>{value || 0}</p>
  </div>
);

const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, onDetails, title }) => (
  <div className="bg-white rounded-[32px] border border-slate-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
    <div className="px-8 py-5 border-b border-slate-100 bg-white flex justify-between items-center"><h3 className="font-bold text-slate-800 text-[11px] uppercase tracking-widest italic">{title} Registry</h3><span className="px-4 py-1 bg-slate-50 rounded-full text-[9px] font-bold text-slate-400 uppercase tracking-tighter">{users?.length || 0} Registered Units</span></div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead><tr className="bg-slate-50/50 text-[9px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-100"><th className="px-8 py-5">Core Identity</th><th className="px-8 py-5 text-center">Status</th><th className="px-8 py-5 text-right">System Console</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {!users || users.length === 0 ? (
            <tr><td colSpan="3" className="px-10 py-32 text-center text-slate-200 uppercase font-bold text-[10px] tracking-widest">Null Response</td></tr>
          ) : (
            users.map(u => (
              <tr key={u.uid} className="hover:bg-slate-50/50 transition-colors group">
                <td className="px-8 py-5"><div className="flex items-center space-x-4 cursor-pointer" onClick={()=>onDetails(u)}><img src={u.profileImage || 'https://via.placeholder.com/32'} className="w-10 h-10 rounded-xl object-cover border-2 border-white shadow-md group-hover:scale-110 transition-transform" /><div><p className="font-bold text-slate-800 text-xs uppercase tracking-tight italic">{u.name || 'ANONYMOUS'}</p><p className="text-[9px] text-slate-400 font-medium tracking-tighter">{u.phoneNumber}</p></div></div></td>
                <td className="px-8 py-5 text-center"><span className={`px-4 py-1.5 rounded-full text-[8px] font-bold uppercase border-2 transition-all ${u.isBlocked ? 'bg-red-50 border-red-200 text-red-500 shadow-red-50' : 'bg-emerald-50 border-emerald-200 text-emerald-600 shadow-emerald-50'}`}>{u.isBlocked ? 'SUSPENDED' : 'AUTHORIZED'}</span></td>
                <td className="px-8 py-5 text-right space-x-2">
                  <button onClick={()=>onWarn(u)} className="p-3 bg-amber-50 text-amber-500 rounded-xl hover:bg-amber-500 hover:text-white transition-all shadow-sm" title="Warning"><AlertTriangle size={16}/></button>
                  <button onClick={()=>onWallet(u)} className="p-3 bg-indigo-50 text-indigo-500 rounded-xl hover:bg-indigo-600 hover:text-white transition-all shadow-sm" title="Wallet"><Wallet size={16}/></button>
                  <button onClick={()=>onBlock(u)} className={`p-3 rounded-xl transition-all shadow-sm ${u.isBlocked ? 'bg-emerald-50 text-emerald-500 hover:bg-emerald-500 hover:text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-950 hover:text-white'}`} title="Lock"><ShieldAlert size={16}/></button>
                  <button onClick={()=>onDelete(u)} className="p-3 bg-red-50 text-red-400 rounded-xl hover:bg-red-600 hover:text-white transition-all shadow-sm" title="Purge"><Trash2 size={16}/></button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

const PendingView = ({ providers, onApprove }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 animate-in fade-in duration-300">
    {providers.length === 0 ? (
       <div className="col-span-full p-40 bg-white rounded-[40px] border-4 border-dashed border-slate-100 text-center text-slate-200 font-bold uppercase text-[10px] tracking-[8px]">Registry Nominal</div>
    ) : (
      providers.map(p => (
        <div key={p.uid} className="bg-white p-8 rounded-[35px] border border-slate-200 relative group overflow-hidden hover:shadow-2xl transition-all text-center">
          <button onClick={()=>onApprove(p.uid)} className="absolute top-4 right-4 p-2 bg-indigo-600 text-white rounded-xl shadow-lg font-bold uppercase text-[8px] tracking-widest hover:bg-indigo-700 active:scale-95 transition-all">Verify Node</button>
          <img src={p.profileImage || 'https://via.placeholder.com/100'} className="w-20 h-20 rounded-3xl object-cover ring-8 ring-slate-50 shadow-2xl mx-auto mb-6 transition-transform group-hover:rotate-6" />
          <h4 className="font-bold text-sm tracking-tight text-slate-900 uppercase italic leading-none truncate px-4">{p.name}</h4>
          <p className="text-[10px] font-bold text-indigo-500 uppercase tracking-widest mt-2 mb-8 opacity-60 italic">{p.profession}</p>
          <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 shadow-inner flex justify-between items-center"><p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">ID: <span className="text-slate-700 font-black">{p.aadhaarNumber}</span></p><Eye size={18} className="text-indigo-400 cursor-pointer hover:scale-125 transition-all"/></div>
        </div>
      ))
    )}
  </div>
);

const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-[32px] border border-slate-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
    <div className="px-10 py-6 border-b border-slate-100 font-bold text-slate-800 text-[10px] uppercase tracking-[6px] bg-white text-center italic opacity-30">Pending Financial Settlements</div>
    <div className="overflow-x-auto">
      <table className="w-full text-left text-[11px]">
        <thead><tr className="bg-slate-50/50 text-[9px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-50"><th className="px-10 py-5">Target Entity</th><th className="px-10 py-5 text-right">Liquidation Control</th></tr></thead>
        <tbody className="divide-y divide-slate-50 font-sans">
          {withdrawals.length === 0 ? (
            <tr><td colSpan="2" className="px-10 py-32 text-center text-slate-200 italic font-bold uppercase tracking-widest text-[9px]">Awaiting Requests...</td></tr>
          ) : (
            withdrawals.map(w => (
              <tr key={w._id} className="hover:bg-slate-50/50 transition-colors">
                <td className="px-10 py-10 font-bold text-slate-900 uppercase tracking-tighter text-2xl italic leading-none">{w.providerName}</td>
                <td className="px-10 py-10 text-right space-x-6"><span className="text-indigo-600 font-bold text-4xl mr-10 italic tracking-tighter shadow-indigo-100">₹{w.amount}</span><button onClick={()=>onHandle(w._id, 'approved')} className="px-10 py-4 bg-slate-950 text-white text-[9px] font-bold rounded-[22px] uppercase tracking-[4px] shadow-2xl hover:bg-indigo-600 active:scale-95 transition-all">Release Assets</button></td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

const JobsTable = ({ jobs, title }) => (
  <div className="bg-white rounded-[32px] border border-slate-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
    <div className="px-10 py-6 border-b font-bold uppercase text-[10px] tracking-[8px] bg-white text-slate-800 flex justify-between items-center italic opacity-40"><span>{title}</span><div className="px-4 py-1.5 bg-indigo-50 text-indigo-600 rounded-full font-bold italic tracking-tighter text-[10px]">{jobs?.length || 0} TOTAL OPS</div></div>
    <table className="w-full text-left border-collapse">
      <thead><tr className="bg-slate-50/50 text-[9px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-100"><th className="px-10 py-6">Mission Debrief</th><th className="px-10 py-6">Deployment State</th><th className="px-10 py-6 text-right">Resource Valuation</th></tr></thead>
      <tbody className="divide-y divide-slate-50">
        {(!jobs || jobs.length === 0) ? (
          <tr><td colSpan="3" className="px-10 py-40 text-center text-slate-100 font-bold uppercase text-[10px] tracking-[20px]">Archive Empty</td></tr>
        ) : (
          jobs.map(j => (
            <tr key={j._id} className="hover:bg-slate-50/50 transition-colors group">
              <td className="px-10 py-8"><p className="font-bold text-slate-800 uppercase tracking-tighter text-2xl group-hover:text-indigo-600 transition-colors italic leading-none">{j.serviceName}</p><p className="text-[9px] font-semibold text-slate-300 mt-2 tracking-widest">{new Date(j.createdAt).toLocaleDateString()}</p></td>
              <td className="px-10 py-8"><span className={`px-6 py-2.5 rounded-full text-[9px] font-bold uppercase border-2 transition-all shadow-lg italic ${j.status === 'done' ? 'bg-emerald-50 border-emerald-100 text-emerald-600 shadow-emerald-50' : 'bg-indigo-50 border-indigo-100 text-indigo-600 shadow-indigo-50'}`}>{j.status.replace('_',' ')}</span></td>
              <td className="px-10 py-8 text-right font-bold text-4xl text-slate-950 tracking-tighter italic opacity-80 group-hover:opacity-100 transition-opacity group-hover:scale-105">₹{j.totalAmount}</td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  </div>
);

const CategoriesView = ({ categories, refresh }) => {
  const [n, setN] = useState(''); const [i, setI] = useState('');
  const handleAdd = () => adminApi.addCategory({name:n, icon:i}).then(()=>{setN('');setI('');refresh();}).catch(()=>showToast('Error', 'error'));
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-12 animate-in fade-in duration-300">
      <div className="bg-[#020617] p-12 rounded-[50px] shadow-2xl h-fit border border-white/5 relative overflow-hidden text-center">
        <div className="absolute top-0 left-0 w-full h-1 bg-indigo-600"></div>
        <h3 className="font-bold mb-12 uppercase text-[9px] tracking-[6px] text-indigo-500 italic">Initialize Core Module</h3>
        <div className="space-y-8">
            <input value={n} onChange={e=>setN(e.target.value)} placeholder="VECTOR NAME" className="w-full p-5 bg-white/5 border-2 border-white/10 rounded-[28px] font-black text-white outline-none focus:border-indigo-600 uppercase tracking-widest text-xs shadow-inner" />
            <input value={i} onChange={e=>setI(e.target.value)} placeholder="URI ASSET" className="w-full p-5 bg-white/5 border-2 border-white/10 rounded-[28px] font-bold text-white outline-none focus:border-indigo-600 text-xs shadow-inner" />
            <button onClick={handleAdd} className="w-full py-8 bg-indigo-600 text-white font-bold rounded-[35px] uppercase text-xs tracking-[8px] shadow-[0_20px_50px_rgba(79,70,229,0.3)] hover:bg-white hover:text-indigo-600 active:scale-95 transition-all">Initialize Deploy</button>
        </div>
      </div>
      <div className="lg:col-span-2 grid grid-cols-2 md:grid-cols-3 gap-8">
        {categories.map(c => (
            <div key={c._id} className="bg-white p-10 rounded-[50px] border border-slate-200 shadow-sm flex flex-col items-center transition-all hover:shadow-2xl hover:scale-105 group relative overflow-hidden">
                <div className="w-24 h-24 bg-slate-50 rounded-[40px] flex items-center justify-center mb-8 shadow-inner transition-transform group-hover:rotate-12 duration-500"><img src={c.icon || 'https://via.placeholder.com/80'} className="w-16 h-16 object-contain drop-shadow-2xl" /></div>
                <p className="font-bold text-slate-800 uppercase tracking-[4px] text-[11px] text-center leading-relaxed italic">{c.name}</p>
                <button onClick={()=>adminApi.deleteCategory(c._id).then(refresh)} className="absolute top-6 right-6 p-3 bg-red-50 text-red-400 rounded-full opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-500 hover:text-white shadow-xl"><Trash2 size={16}/></button>
            </div>
        ))}
      </div>
    </div>
  );
};

const ReportsView = ({ reports }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-10 animate-in fade-in duration-300">
    {reports.length === 0 ? <div className="col-span-full p-60 bg-white rounded-[70px] border-4 border-dashed border-slate-100 text-center text-slate-100 font-bold uppercase text-3xl tracking-[30px] italic">STATUS NOMINAL</div> : reports.map(r => (
      <div key={r._id} className="bg-white p-12 rounded-[60px] border-l-[20px] border-red-500 shadow-2xl relative overflow-hidden group hover:shadow-red-50 transition-all duration-500">
        <div className="absolute top-0 right-0 px-8 py-3 bg-red-600 text-white text-[9px] font-bold uppercase rounded-bl-[35px] tracking-[4px] shadow-2xl">{r.status}</div>
        <h4 className="font-bold text-slate-900 uppercase text-2xl tracking-tighter mb-8 italic">Incident: {r.reason}</h4>
        <div className="bg-slate-50 p-8 rounded-[40px] shadow-inner mb-12 border border-slate-100 font-bold text-slate-500 italic opacity-90 text-sm leading-relaxed">"{r.description}"</div>
        <div className="flex gap-4 overflow-x-auto pb-6 custom-scrollbar scroll-smooth">
            {r.evidenceUrls?.map((u, i) => (
                <img key={i} src={u} className="w-28 h-28 rounded-[45px] border-[10px] border-white object-cover shadow-2xl transition-all hover:scale-110 hover:rotate-6 flex-shrink-0 cursor-zoom-in" />
            ))}
        </div>
      </div>
    ))}
  </div>
);

const ReviewsView = ({ reviews, onDelete }) => (
  <div className="bg-white rounded-[60px] border border-slate-100 shadow-sm overflow-hidden text-left animate-in fade-in duration-300">
    <div className="px-12 py-12 border-b border-slate-50 bg-white font-bold text-slate-800 text-[10px] uppercase tracking-[15px] text-center opacity-30 italic">Intelligence Sentiment Registry</div>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead><tr className="bg-slate-50/50 text-[9px] font-bold text-slate-400 uppercase tracking-[6px] border-b border-slate-50"><th className="px-12 py-8">Review Authority</th><th className="px-12 py-8 text-left">Analytical Debrief</th><th className="px-12 py-8 text-right">System Control</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {reviews.map(r => (
            <tr key={r._id} className="hover:bg-slate-50/50 transition-all group">
              <td className="px-12 py-12 font-bold text-slate-900 uppercase tracking-tighter text-4xl italic leading-none">{r.customerName}
                  <div className="flex gap-1.5 mt-6">
                      {[...Array(5)].map((_, i) => <Star key={i} size={18} className={i < r.rating ? 'text-amber-400 fill-amber-400 drop-shadow-2xl' : 'text-slate-100'} />)}
                  </div>
              </td>
              <td className="px-12 py-12"><div className="p-10 bg-slate-50 rounded-[45px] border border-slate-200 shadow-inner italic font-bold text-slate-500 text-xl leading-relaxed group-hover:bg-white transition-colors duration-300">"{r.comment}"</div></td>
              <td className="px-12 py-12 text-right"><button onClick={()=>onDelete(r._id)} className="p-8 bg-red-50 text-red-400 rounded-[45px] hover:bg-red-600 hover:text-white transition-all shadow-xl active:scale-90 hover:rotate-12 duration-300"><Trash2 size={32}/></button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
);

const SettingsView = ({ settings, refresh }) => {
  const [c, setC] = useState(settings.commission_percentage || 15);
  return (
    <div className="bg-[#020617] p-20 rounded-[80px] border-[12px] border-white/5 max-w-4xl shadow-[0_80px_200px_-50px_rgba(0,0,0,1)] relative overflow-hidden group animate-in fade-in duration-500 mx-auto">
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-500/5 rounded-full -mr-[250px] -mt-[250px] blur-[150px] group-hover:bg-indigo-500/10 transition-all duration-1000"></div>
      <h3 className="font-bold uppercase text-[11px] tracking-[15px] mb-20 text-indigo-500 italic text-center">Global Logic Core</h3>
      <div className="flex justify-between items-center mb-20 relative z-10 text-white uppercase font-bold text-5xl tracking-tighter italic">
          <div className="space-y-6"><p className="leading-none tracking-tight">Revenue Friction</p><p className="text-slate-600 text-sm font-bold tracking-[4px] normal-case opacity-80 leading-relaxed border-l-4 border-indigo-600 pl-8 text-left">The coefficient determining platform retainment unit factor.</p></div>
          <div className="flex items-center space-x-8 bg-white/5 p-12 rounded-[60px] border-2 border-white/10 shadow-[0_30px_100px_rgba(0,0,0,0.4)] hover:border-indigo-600/30 transition-all duration-500 group/input">
              <input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-40 h-40 text-center bg-transparent font-bold text-8xl text-indigo-500 outline-none placeholder-indigo-900 group-hover/input:scale-110 transition-transform duration-500" />
              <span className="font-bold text-white/5 text-[140px] italic select-none leading-none opacity-10">%</span>
          </div>
      </div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(()=>showToast('Engine Synced'))} className="w-full py-10 bg-indigo-600 text-white font-bold rounded-[45px] uppercase text-sm tracking-[12px] shadow-[0_40px_100px_rgba(79,70,229,0.4)] hover:bg-white hover:text-indigo-600 hover:-translate-y-4 active:scale-95 transition-all duration-500 relative z-10 italic">SYNCHRONIZE GLOBAL RULES</button>
    </div>
  );
};

const SupportView = ({ chats }) => (
  <div className="bg-white rounded-[70px] border border-slate-100 h-[700px] flex items-center justify-center text-slate-100 font-bold uppercase tracking-[40px] text-center px-12 leading-relaxed animate-pulse italic">GRID_BRIDGE_OFFLINE</div>
);
