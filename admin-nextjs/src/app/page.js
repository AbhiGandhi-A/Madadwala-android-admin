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
  loading: () => <div className="h-full w-full bg-slate-50 flex items-center justify-center font-semibold text-slate-300 uppercase tracking-widest text-[9px]">Initializing Cluster...</div>
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
    if (toast) { const timer = setTimeout(() => setToast(null), 2000); return () => clearTimeout(timer); }
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
        case 'settings': res = await adminApi.getSettings(); setData(p => ({...p, settings: res.data})); break;
        case 'reports': res = await adminApi.getReports(); setData(p => ({...p, reports: res.data})); break;
        case 'reviews': res = await adminApi.getAllReviews(); setData(p => ({...p, reviews: res.data})); break;
      }
    } catch (err) { setError("Data Sync Error"); }
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
      showToast("Success"); setActiveModal(null);
    } catch (e) { showToast("Error", "error"); }
  };

  return (
    <div className="flex h-screen bg-slate-50 text-slate-700 overflow-hidden text-[12px] selection:bg-indigo-100 antialiased">
      {toast && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[100] animate-in fade-in duration-150">
            <div className={`px-4 py-1.5 rounded-full shadow-lg flex items-center gap-2 border bg-white ${toast.type === 'success' ? 'border-emerald-500 text-emerald-600' : 'border-red-500 text-red-600'}`}>
                <Check size={12} strokeWidth={3} />
                <span className="font-bold uppercase text-[9px] tracking-widest">{toast.message}</span>
            </div>
        </div>
      )}

      <aside className="w-52 bg-[#020617] text-white flex flex-col shrink-0 z-20">
        <div className="h-14 flex items-center px-6 border-b border-white/5 font-extrabold text-base tracking-tight italic">MADADWALA</div>
        <nav className="flex-1 px-2 py-4 space-y-0.5 overflow-y-auto custom-scrollbar">
          <NavItem icon={<Activity size={14}/>} label="Mission Control" active={activeTab==='monitor'} onClick={()=>setActiveTab('monitor')} />
          <NavItem icon={<BarChart3 size={14}/>} label="Analytics" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <div className="pt-4 pb-1 px-3 text-[8px] font-bold text-slate-500 uppercase tracking-widest">Directory</div>
          <NavItem icon={<Users size={14}/>} label="Consumers" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={14}/>} label="Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={14}/>} label="Approvals" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />
          <div className="pt-4 pb-1 px-3 text-[8px] font-bold text-slate-500 uppercase tracking-widest">Fleet Ops</div>
          <NavItem icon={<CreditCard size={14}/>} label="Payouts" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Navigation size={14}/>} label="Live Tracking" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={14}/>} label="History" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />
          <div className="pt-4 pb-1 px-3 text-[8px] font-bold text-slate-500 uppercase tracking-widest">Governance</div>
          <NavItem icon={<FileText size={14}/>} label="Reports" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />
          <NavItem icon={<Star size={14}/>} label="Reviews" active={activeTab==='reviews'} onClick={()=>setActiveTab('reviews')} />
          <NavItem icon={<Settings size={14}/>} label="Engine" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
        <div className="p-3"><button onClick={()=>openAction('broadcast')} className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-[9px] uppercase rounded-md flex items-center justify-center gap-2">Broadcast</button></div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0 bg-white">
        <header className="h-12 bg-white border-b border-slate-100 flex items-center justify-between px-6 shrink-0 z-10">
          <h2 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest italic">{activeTab.replace('-', ' ')}</h2>
          <div className="w-7 h-7 rounded-md bg-slate-900 flex items-center justify-center text-white font-bold text-[10px]">A</div>
        </header>

        <main className="flex-1 overflow-y-auto p-0 bg-slate-50/50 custom-scrollbar">
          {loading ? (
             <div className="flex items-center justify-center h-full"><div className="w-5 h-5 border-2 border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div></div>
          ) : (
            <div className={`h-full ${activeTab !== 'monitor' ? 'p-6 max-w-[1200px] mx-auto' : ''}`}>
              {activeTab === 'monitor' && <MonitorView data={data.monitor} onRefresh={()=>fetchTabData('monitor')} selectedPartner={selectedPartner} setSelectedPartner={setSelectedPartner} />}
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('customers'); showToast("Updated");})} onDetails={(u)=>openAction('details', u)} title="Customer" />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('providers-all'); showToast("Updated");})} onDetails={(u)=>openAction('details', u)} title="Partner" />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>{fetchTabData('providers-pending'); showToast("Verified");})} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>{fetchTabData('withdrawals'); showToast("Paid");})} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Live Tracking" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="Platform Archives" />}
              {activeTab === 'reports' && <ReportsView reports={data.reports} />}
              {activeTab === 'reviews' && <ReviewsView reviews={data.reviews} onDelete={(id)=>adminApi.deleteReview(id).then(()=>{fetchTabData('reviews'); showToast("Purged");})} />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>fetchTabData('settings')} />}
            </div>
          )}
        </main>
      </div>

      {activeModal && (
          <div className="fixed inset-0 z-50 overflow-hidden flex justify-end">
              <div className="absolute inset-0 bg-slate-900/10" onClick={()=>setActiveModal(null)}></div>
              <div className="relative w-72 bg-white shadow-2xl h-full flex flex-col border-l border-slate-100">
                  <div className="px-5 py-4 border-b border-slate-50 flex items-center justify-between">
                      <h3 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">{activeModal}</h3>
                      <button onClick={()=>setActiveModal(null)} className="p-1 hover:bg-slate-50 rounded-full"><X size={16}/></button>
                  </div>
                  <div className="flex-1 overflow-y-auto p-4 space-y-6 custom-scrollbar">
                      {selectedUser && activeModal !== 'broadcast' && (
                          <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100">
                              <img src={selectedUser.profileImage || 'https://via.placeholder.com/40'} className="w-10 h-10 rounded-lg object-cover" />
                              <div><p className="font-bold text-slate-900 text-[11px] leading-tight">{selectedUser.name}</p><p className="text-[9px] text-slate-400 font-medium">{selectedUser.phoneNumber}</p></div>
                          </div>
                      )}

                      {activeModal === 'warning' && (
                          <div className="space-y-4">
                              <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="Incident Class" className="w-full p-3 bg-slate-50 border border-slate-200 rounded-lg outline-none font-bold text-[10px]" />
                              <textarea rows={6} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Debrief report..." className="w-full p-3 bg-slate-50 border border-slate-200 rounded-lg outline-none font-medium text-[10px] resize-none" />
                          </div>
                      )}

                      {activeModal === 'wallet' && (
                          <div className="space-y-6">
                              <div className="flex gap-2">
                                  <button onClick={()=>setFormState({...formState, type: 'credit'})} className={`flex-1 py-2 rounded-lg font-bold text-[9px] border ${formState.type === 'credit' ? 'bg-emerald-600 border-emerald-600 text-white' : 'bg-slate-50 text-slate-400'}`}>Credit</button>
                                  <button onClick={()=>setFormState({...formState, type: 'debit'})} className={`flex-1 py-2 rounded-lg font-bold text-[9px] border ${formState.type === 'debit' ? 'bg-red-600 border-red-600 text-white' : 'bg-slate-50 text-slate-400'}`}>Debit</button>
                              </div>
                              <input type="number" value={formState.amount} onChange={e=>setFormState({...formState, amount: e.target.value})} placeholder="₹ 0" className="w-full p-4 bg-slate-50 border border-slate-200 rounded-lg outline-none font-bold text-xl text-center" />
                          </div>
                      )}

                      {activeModal === 'details' && selectedUser && (
                          <div className="space-y-6">
                              <div className="grid grid-cols-2 gap-2 text-center text-[10px]">
                                  <div className="p-3 bg-indigo-50 rounded-xl"><p className="text-indigo-400 font-bold mb-1 uppercase tracking-widest text-[8px]">Revenue</p><p className="font-bold text-indigo-600">₹{selectedUser.totalEarnings || 0}</p></div>
                                  <div className="p-3 bg-slate-900 rounded-xl text-white"><p className="opacity-60 font-bold mb-1 uppercase tracking-widest text-[8px]">Jobs</p><p className="font-bold">{selectedUser.totalJobs || 0}</p></div>
                              </div>
                              <div className="space-y-2">
                                  <h4 className="text-[9px] font-bold text-slate-400 uppercase tracking-widest border-b pb-1">Activity Stream</h4>
                                  {selectedUser.activityLog?.slice(-10).reverse().map((log, i) => (
                                      <div key={i} className="p-2 bg-white border border-slate-100 rounded-md">
                                          <div className="flex justify-between items-center mb-0.5"><span className="text-[9px] font-bold text-indigo-600 uppercase">{log.event}</span><span className="text-[8px] text-slate-300">{new Date(log.timestamp).toLocaleDateString()}</span></div>
                                          <p className="text-[10px] text-slate-500 leading-tight">"{log.description}"</p>
                                      </div>
                                  ))}
                              </div>
                          </div>
                      )}
                  </div>
                  <div className="p-4 border-t border-slate-50">
                      <button onClick={handleAction} className={`w-full py-3 rounded-lg font-bold uppercase text-[9px] tracking-widest shadow-lg ${activeModal === 'delete' ? 'bg-red-600 text-white' : 'bg-slate-950 text-white'}`}>Confirm</button>
                  </div>
              </div>
          </div>
      )}
    </div>
  );
}

function NavItem({ icon, label, active, onClick, count, pulse }) {
  return (
    <button onClick={onClick} className={`w-full flex items-center justify-between px-4 py-2 rounded-xl transition-all ${active ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}>
      <div className="flex items-center space-x-3">
        {pulse && !active && <div className="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-ping absolute -ml-0.5"></div>}
        {icon}<span className="font-medium text-[11px]">{label}</span>
      </div>
      {count > 0 && <span className={`px-1 py-0.5 text-[8px] font-bold rounded ${active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'}`}>{count}</span>}
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
            <div><h3 className="text-xl font-bold text-slate-800 tracking-tight uppercase">Live Partner Tracking</h3><p className="text-slate-400 font-medium text-[9px] uppercase mt-0.5 tracking-wider">Operational Console V1.0</p></div>
            <div className="flex gap-2">
                <StatusPill color="bg-emerald-500" label="Ready" count={counts.online} />
                <StatusPill color="bg-indigo-500" label="Busy" count={counts.busy} />
            </div>
        </div>

        <div className="flex-1 flex gap-6 min-h-0">
            <div className="w-64 flex flex-col bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
                <div className="p-3 border-b border-slate-100 flex gap-1 bg-slate-50/50">
                    {['all', 'online', 'busy'].map(f => (
                        <button key={f} onClick={()=>setFilter(f)} className={`flex-1 py-1 rounded-md text-[8px] font-bold uppercase transition-all ${filter===f ? 'bg-slate-900 text-white' : 'bg-white text-slate-400 border border-slate-100'}`}>{f}</button>
                    ))}
                </div>
                <div className="flex-1 overflow-y-auto custom-scrollbar p-2 space-y-1">
                    {filteredData.map(p => (
                        <div key={p.uid} onClick={()=>setSelectedPartner(p)} className={`p-2.5 rounded-xl cursor-pointer transition-all border ${selectedPartner?.uid === p.uid ? 'bg-indigo-50 border-indigo-200 shadow-sm' : 'bg-transparent border-transparent hover:bg-slate-50'}`}>
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <img src={p.profileImage || 'https://via.placeholder.com/24'} className="w-7 h-7 rounded-lg object-cover" />
                                    <div><p className="font-bold text-slate-800 text-[10px] uppercase truncate w-20 tracking-tighter">{p.name}</p><p className="text-[8px] font-medium text-slate-400 uppercase tracking-tighter">{p.phoneNumber}</p></div>
                                </div>
                                <div className={`w-1.5 h-1.5 rounded-full ${p.status === 'busy' ? 'bg-indigo-500' : 'bg-emerald-500'}`}></div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            <div className="flex-1 bg-slate-100 rounded-2xl relative overflow-hidden border border-slate-200 shadow-sm">
                <div className="absolute inset-0 z-0"><MapComponent partners={filteredData} selectedPartner={selectedPartner} /></div>
                <button onClick={onRefresh} className="absolute top-4 right-4 p-2 bg-white shadow-md rounded-lg z-10 hover:bg-slate-50 border border-slate-100"><RefreshCw size={12}/></button>
            </div>
        </div>

        {selectedPartner && (
            <div className="bg-white p-4 rounded-2xl shadow-lg border border-slate-100 flex items-center justify-between gap-6 animate-in slide-in-from-bottom-1">
                <div className="flex items-center gap-4">
                    <img src={selectedPartner.profileImage || 'https://via.placeholder.com/60'} className="w-10 h-10 rounded-xl object-cover ring-2 ring-slate-100" />
                    <div><h4 className="font-bold text-slate-900 text-sm uppercase tracking-tight">{selectedPartner.name}</h4><p className="font-medium text-slate-400 text-[9px] uppercase tracking-widest">{selectedPartner.phoneNumber}</p></div>
                </div>
                <div className="flex gap-2"><button className="px-6 py-2 bg-indigo-50 text-indigo-600 rounded-lg font-bold text-[9px] uppercase border border-indigo-100">Focus</button><button className="px-6 py-2 bg-slate-900 text-white rounded-lg font-bold text-[9px] uppercase shadow-md">Comms</button></div>
            </div>
        )}
    </div>
  );
};

const StatusPill = ({ color, label, count }) => (
    <div className="px-3 py-1.5 bg-white border border-slate-200 rounded-full shadow-sm flex items-center gap-2.5"><div className={`w-1.5 h-1.5 rounded-full ${color}`}></div><span className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">{label}</span><span className="text-[11px] font-bold text-slate-900">{count}</span></div>
);

const AnalyticsView = ({ data }) => !data ? null : (
  <div className="grid grid-cols-1 md:grid-cols-4 gap-4 animate-in fade-in duration-300">
    <StatCard title="Consumers" value={data.totalUsers} color="text-indigo-600" />
    <StatCard title="Partners" value={data.totalProviders} color="text-emerald-600" />
    <StatCard title="Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} color="text-slate-900" />
    <StatCard title="Archive" value={data.totalBookings} color="text-blue-600" />
    <div className="col-span-full bg-white p-6 rounded-2xl border border-slate-200 shadow-sm mt-2">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-10 gap-y-6">
            {data.categories?.map((cat, i) => (
                <div key={i}>
                    <div className="flex justify-between items-end mb-1.5"><p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{cat.name}</p><p className="text-sm font-bold text-slate-800">{Math.round(cat.ratio * 100)}%</p></div>
                    <div className="w-full bg-slate-100 rounded-full h-1 overflow-hidden"><div className="bg-indigo-500 h-full rounded-full transition-all duration-1000 shadow-sm" style={{ width: `${cat.ratio * 100}%` }}></div></div>
                </div>
            ))}
        </div>
    </div>
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
    <p className="text-[8px] font-bold text-slate-400 uppercase tracking-widest mb-0.5 opacity-70">{title}</p>
    <p className={`text-xl font-bold tracking-tight ${color}`}>{value || 0}</p>
  </div>
);

const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, onDetails, title }) => (
  <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
    <div className="px-6 py-3.5 border-b border-slate-100 bg-white flex justify-between items-center"><h3 className="font-bold text-slate-800 text-[10px] uppercase tracking-widest">{title} Ledger</h3><span className="px-3 py-1 bg-slate-50 rounded-md text-[8px] font-bold text-slate-400 uppercase tracking-tighter">{users?.length || 0} secure nodes</span></div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead><tr className="bg-slate-50/50 text-[8px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-100"><th className="px-6 py-3">Identity</th><th className="px-6 py-3 text-center">Protocol</th><th className="px-6 py-3 text-right">Admin</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {!users || users.length === 0 ? (
            <tr><td colSpan="3" className="px-10 py-16 text-center text-slate-200 uppercase font-bold text-[9px] tracking-widest">Repository Null</td></tr>
          ) : (
            users.map(u => (
              <tr key={u.uid} className="hover:bg-slate-50/50 transition-colors group">
                <td className="px-6 py-3.5"><div className="flex items-center space-x-3 cursor-pointer" onClick={()=>onDetails(u)}><img src={u.profileImage || 'https://via.placeholder.com/24'} className="w-8 h-8 rounded-lg object-cover" /><div><p className="font-bold text-slate-800 text-[11px] uppercase tracking-tight italic">{u.name || 'Anonymous'}</p><p className="text-[9px] text-slate-400 font-medium tracking-tighter">{u.phoneNumber}</p></div></div></td>
                <td className="px-6 py-3.5 text-center"><span className={`px-2 py-0.5 rounded-full text-[7px] font-bold uppercase border ${u.isBlocked ? 'bg-red-50 border-red-100 text-red-500' : 'bg-emerald-50 border-emerald-100 text-emerald-500'}`}>{u.isBlocked ? 'INHIBITED' : 'ACTIVE'}</span></td>
                <td className="px-6 py-3.5 text-right space-x-1">
                  <button onClick={()=>onWarn(u)} className="p-1.5 bg-amber-50 text-amber-500 rounded-md hover:bg-amber-100"><AlertTriangle size={12}/></button>
                  <button onClick={()=>onWallet(u)} className="p-1.5 bg-indigo-50 text-indigo-500 rounded-md hover:bg-indigo-100"><Wallet size={12}/></button>
                  <button onClick={()=>onBlock(u)} className={`p-1.5 rounded-md ${u.isBlocked ? 'bg-emerald-50 text-emerald-500' : 'bg-slate-100 text-slate-500'}`}><ShieldAlert size={12}/></button>
                  <button onClick={()=>onDelete(u)} className="p-1.5 bg-red-50 text-red-400 rounded-md hover:bg-red-100"><Trash2 size={12}/></button>
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
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 animate-in fade-in duration-300">
    {providers.length === 0 ? (
       <div className="col-span-full p-20 bg-white rounded-2xl border border-dashed border-slate-200 text-center text-slate-200 font-bold uppercase text-[9px] tracking-[4px]">Status Nominal</div>
    ) : (
      providers.map(p => (
        <div key={p.uid} className="bg-white p-5 rounded-2xl border border-slate-200 relative group overflow-hidden hover:shadow-md transition-all text-center">
          <button onClick={()=>onApprove(p.uid)} className="absolute top-3 right-3 p-1.5 bg-indigo-600 text-white rounded-md shadow-md font-bold uppercase text-[7px] tracking-widest hover:bg-indigo-700 active:scale-95 transition-all">Verify</button>
          <img src={p.profileImage || 'https://via.placeholder.com/60'} className="w-14 h-14 rounded-xl object-cover ring-2 ring-slate-50 shadow-md mx-auto mb-3" />
          <h4 className="font-bold text-[11px] tracking-tight text-slate-900 uppercase italic leading-none truncate px-2">{p.name}</h4>
          <p className="text-[8px] font-bold text-indigo-500 uppercase tracking-widest mt-1.5 mb-4 opacity-60 italic">{p.profession}</p>
          <div className="p-2 bg-slate-50 rounded-lg border border-slate-100 flex justify-between items-center"><p className="text-[7px] font-bold text-slate-400 uppercase tracking-widest">ID: <span className="text-slate-700">{p.aadhaarNumber}</span></p><Eye size={12} className="text-indigo-400 cursor-pointer hover:scale-110"/></div>
        </div>
      ))
    )}
  </div>
);

const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
    <div className="px-6 py-4 border-b border-slate-100 font-bold text-slate-800 text-[9px] uppercase tracking-[3px] bg-white text-center italic opacity-60">Pending Clearance Queue</div>
    <div className="overflow-x-auto">
      <table className="w-full text-left text-[10px]">
        <thead><tr className="bg-slate-50/50 text-[8px] font-bold text-slate-400 uppercase tracking-widest"><th className="px-6 py-3">Entity</th><th className="px-6 py-3 text-right">Authorization</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {withdrawals.length === 0 ? (
            <tr><td colSpan="2" className="px-10 py-16 text-center text-slate-200 italic font-bold uppercase tracking-widest text-[8px]">Waiting for triggers...</td></tr>
          ) : (
            withdrawals.map(w => (
              <tr key={w._id} className="hover:bg-slate-50/50 transition-colors font-sans">
                <td className="px-6 py-4 font-bold text-slate-800 uppercase tracking-tighter italic">{w.providerName}</td>
                <td className="px-6 py-4 text-right space-x-3"><span className="text-indigo-600 font-bold text-base mr-4 italic tracking-tighter">₹{w.amount}</span><button onClick={()=>onHandle(w._id, 'approved')} className="px-4 py-1.5 bg-slate-900 text-white text-[8px] font-bold rounded-md uppercase tracking-widest shadow-sm hover:bg-indigo-600 transition-all">Authorize</button></td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

const JobsTable = ({ jobs, title }) => (
  <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
    <div className="px-6 py-4 border-b font-bold uppercase text-[10px] tracking-widest bg-white text-slate-800 flex justify-between items-center italic opacity-40"><span>{title}</span><div className="px-2 py-0.5 bg-indigo-50 text-indigo-600 rounded-md font-bold tracking-tighter text-[9px] italic">{jobs?.length || 0} TOTAL</div></div>
    <table className="w-full text-left text-[10px]">
      <thead><tr className="bg-slate-50/50 text-[8px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-100"><th className="px-6 py-3">Mission</th><th className="px-6 py-3">State</th><th className="px-6 py-3 text-right">Value</th></tr></thead>
      <tbody className="divide-y divide-slate-50">
        {(!jobs || jobs.length === 0) ? (
          <tr><td colSpan="3" className="px-10 py-16 text-center text-slate-200 font-bold uppercase text-[9px] tracking-widest">Null Set</td></tr>
        ) : (
          jobs.map(j => (
            <tr key={j._id} className="hover:bg-slate-50/50 transition-colors">
              <td className="px-6 py-4"><p className="font-bold text-slate-800 uppercase text-[10px] leading-tight italic">{j.serviceName}</p><p className="text-[8px] text-slate-300 mt-0.5 tracking-tighter">{new Date(j.createdAt).toLocaleDateString()}</p></td>
              <td className="px-6 py-4"><span className={`px-2 py-0.5 rounded-md text-[7px] font-bold uppercase border ${j.status === 'done' ? 'bg-emerald-50 border-emerald-100 text-emerald-600' : 'bg-indigo-50 border-indigo-100 text-indigo-600'}`}>{j.status.replace('_',' ')}</span></td>
              <td className="px-6 py-4 text-right font-bold text-xs text-slate-900 tracking-tighter italic">₹{j.totalAmount}</td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  </div>
);

const SettingsView = ({ settings, refresh }) => {
  const [c, setC] = useState(settings.commission_percentage || 15);
  return (
    <div className="bg-slate-950 p-10 rounded-[40px] border border-white/10 max-w-2xl shadow-2xl relative overflow-hidden group mx-auto">
      <div className="absolute top-0 right-0 w-[300px] h-[300px] bg-indigo-500/5 rounded-full -mr-[150px] -mt-[150px] blur-[80px] group-hover:bg-indigo-500/10 transition-all duration-1000"></div>
      <h3 className="font-bold uppercase text-[10px] tracking-[10px] mb-10 text-indigo-500 text-center italic opacity-40">System Logic Core</h3>
      <div className="flex justify-between items-center mb-10 relative z-10 text-white uppercase font-bold text-xl tracking-tighter italic">
          <div className="space-y-2"><p className="leading-none text-2xl">Revenue Retainment</p><p className="text-slate-600 text-[9px] font-semibold tracking-widest normal-case opacity-80 leading-relaxed border-l-2 border-indigo-600 pl-4 text-left">The coefficient determining platform retainment per transaction node.</p></div>
          <div className="flex items-center space-x-4 bg-white/5 p-6 rounded-3xl border border-white/10 shadow-inner group/input">
              <input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-16 h-16 text-center bg-transparent font-bold text-4xl text-indigo-500 outline-none group-hover/input:scale-110 transition-transform duration-500" />
              <span className="font-bold text-white/5 text-[50px] italic select-none leading-none">%</span>
          </div>
      </div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(()=>showToast('Logic Synced'))} className="w-full py-6 bg-indigo-600 text-white font-bold rounded-2xl uppercase text-[10px] tracking-[6px] shadow-2xl hover:bg-white hover:text-indigo-600 transition-all duration-300 relative z-10 italic">SYNCHRONIZE UNIVERSAL ENGINE</button>
    </div>
  );
};
