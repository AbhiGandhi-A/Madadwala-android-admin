'use client';
import React, { useState, useEffect } from 'react';
import {
  Users, Briefcase, CheckCircle, XCircle, BarChart3,
  Settings, LogOut, MessageSquare, Image as ImageIcon,
  Tag, CreditCard, Bell, ChevronRight, Search, ShieldAlert,
  UserCheck, UserMinus, Clock, Filter, FileText, AlertTriangle,
  Trash2, Plus, Star, Wallet, Send, X, MoreVertical, Eye, Check,
  Activity, Zap, MapPin, Navigation, Info
} from 'lucide-react';
import { adminApi } from '@/lib/api';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('analytics');
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
        case 'monitor': res = await adminApi.getMonitor(); setData(p => ({...p, monitor: res.data})); break;
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
      showToast("Done");
      setActiveModal(null);
    } catch (e) { showToast("Failed", "error"); }
  };

  return (
    <div className="flex h-screen bg-[#F1F5F9] text-gray-900 overflow-hidden font-sans">
      {toast && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-[100]">
            <div className={`px-6 py-3 rounded-xl shadow-lg flex items-center gap-3 border ${toast.type === 'success' ? 'bg-emerald-50 border-emerald-500 text-emerald-600' : 'bg-red-50 border-red-500 text-red-600'}`}>
                <Check size={18} />
                <span className="font-bold text-sm uppercase tracking-wider">{toast.message}</span>
            </div>
        </div>
      )}

      <aside className="w-64 bg-[#020617] text-white flex flex-col shrink-0 z-20">
        <div className="p-6 border-b border-white/5 font-black text-xl tracking-tight">MADADWALA</div>
        <nav className="flex-1 px-3 py-6 space-y-1 overflow-y-auto">
          <NavItem icon={<Activity size={18}/>} label="Mission Control" active={activeTab==='monitor'} onClick={()=>setActiveTab('monitor')} />
          <NavItem icon={<BarChart3 size={18}/>} label="Analytics" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <div className="pt-4 pb-2 px-3 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Management</div>
          <NavItem icon={<Users size={18}/>} label="Customers" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={18}/>} label="Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={18}/>} label="Approvals" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />
          <div className="pt-4 pb-2 px-3 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Jobs</div>
          <NavItem icon={<CreditCard size={18}/>} label="Payouts" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Navigation size={18}/>} label="Live Map" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={18}/>} label="History" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />
          <div className="pt-4 pb-2 px-3 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Settings</div>
          <NavItem icon={<Settings size={18}/>} label="Engine" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
        <div className="p-4"><button onClick={()=>openAction('broadcast')} className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs uppercase rounded-lg transition-colors flex items-center justify-center gap-2 underline decoration-2 underline-offset-4">Broadcast</button></div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0 bg-[#F8FAFC]">
        <header className="h-16 bg-white border-b border-slate-100 flex items-center justify-between px-8 shrink-0 z-10">
          <h2 className="text-lg font-bold text-slate-900 uppercase">{activeTab.replace('-', ' ')}</h2>
          <div className="w-8 h-8 rounded-lg bg-slate-900 flex items-center justify-center text-white font-bold">A</div>
        </header>

        <main className="flex-1 overflow-y-auto p-8">
          {loading ? (
             <div className="flex items-center justify-center h-full"><div className="w-8 h-8 border-4 border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div></div>
          ) : (
            <div className="max-w-[1400px] mx-auto">
              {activeTab === 'monitor' && <MonitorView data={data.monitor} />}
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('customers'); showToast("Toggled");})} onDetails={(u)=>openAction('details', u)} title="Customers" />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('providers-all'); showToast("Toggled");})} onDetails={(u)=>openAction('details', u)} title="Partners" isPartner />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>{fetchTabData('providers-pending'); showToast("Verified");})} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>{fetchTabData('withdrawals'); showToast("Paid");})} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Real-time Log" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="Archives" />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>{fetchTabData('settings'); showToast("Synced");}} />}
            </div>
          )}
        </main>
      </div>

      {activeModal && (
          <div className="fixed inset-0 z-50 overflow-hidden flex justify-end">
              <div className="absolute inset-0 bg-slate-900/40" onClick={()=>setActiveModal(null)}></div>
              <div className="relative w-full max-w-md bg-white shadow-xl h-full flex flex-col">
                  <div className="px-6 py-4 border-b border-slate-50 flex items-center justify-between">
                      <h3 className="font-bold text-slate-900 uppercase tracking-wider">{activeModal} module</h3>
                      <button onClick={()=>setActiveModal(null)} className="p-2 hover:bg-slate-50 rounded-full"><X size={20}/></button>
                  </div>
                  <div className="flex-1 overflow-y-auto p-6 space-y-6">
                      {selectedUser && activeModal !== 'broadcast' && (
                          <div className="flex items-center gap-4 p-4 bg-slate-50 rounded-xl">
                              <img src={selectedUser.profileImage || 'https://via.placeholder.com/40'} className="w-12 h-12 rounded-lg object-cover" />
                              <div><p className="font-bold text-slate-900">{selectedUser.name}</p><p className="text-xs text-slate-400 font-bold uppercase">{selectedUser.phoneNumber}</p></div>
                          </div>
                      )}

                      {activeModal === 'warning' && (
                          <div className="space-y-4">
                              <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="Violation Category" className="w-full p-4 bg-slate-50 border border-slate-200 rounded-lg outline-none font-bold" />
                              <textarea rows={6} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Detailed Message..." className="w-full p-4 bg-slate-50 border border-slate-200 rounded-lg outline-none font-bold resize-none" />
                          </div>
                      )}

                      {activeModal === 'wallet' && (
                          <div className="space-y-6">
                              <div className="flex gap-2">
                                  <button onClick={()=>setFormState({...formState, type: 'credit'})} className={`flex-1 py-3 rounded-lg font-bold uppercase text-xs border ${formState.type === 'credit' ? 'bg-emerald-600 border-emerald-600 text-white' : 'bg-slate-50 text-slate-400'}`}>Credit (+)</button>
                                  <button onClick={()=>setFormState({...formState, type: 'debit'})} className={`flex-1 py-3 rounded-lg font-bold uppercase text-xs border ${formState.type === 'debit' ? 'bg-red-600 border-red-600 text-white' : 'bg-slate-50 text-slate-400'}`}>Debit (-)</button>
                              </div>
                              <input type="number" value={formState.amount} onChange={e=>setFormState({...formState, amount: e.target.value})} placeholder="₹ 0.00" className="w-full p-4 bg-slate-50 border border-slate-200 rounded-lg outline-none font-black text-2xl" />
                              <input value={formState.description} onChange={e=>setFormState({...formState, description: e.target.value})} placeholder="Adjustment Note" className="w-full p-4 bg-slate-50 border border-slate-200 rounded-lg outline-none font-bold" />
                          </div>
                      )}

                      {activeModal === 'details' && selectedUser && (
                          <div className="space-y-6">
                              <div className="grid grid-cols-2 gap-4">
                                  <div className="p-4 bg-indigo-50 rounded-xl"><p className="text-[10px] font-bold text-indigo-400 uppercase mb-1">Lifetime</p><p className="text-xl font-black text-indigo-600">₹{selectedUser.totalEarnings || 0}</p></div>
                                  <div className="p-4 bg-slate-900 rounded-xl text-white"><p className="text-[10px] font-bold opacity-60 uppercase mb-1">Ops</p><p className="text-xl font-black">{selectedUser.totalJobs || 0}</p></div>
                              </div>
                              <div className="space-y-4">
                                  <h4 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest border-b pb-2">Activity Stream</h4>
                                  <div className="space-y-3">
                                      {selectedUser.activityLog?.slice(-10).reverse().map((log, i) => (
                                          <div key={i} className="p-3 bg-white border border-slate-100 rounded-lg shadow-sm">
                                              <div className="flex justify-between items-center mb-1"><span className="text-[10px] font-black text-indigo-600 uppercase">{log.event}</span><span className="text-[9px] text-slate-300 font-bold">{new Date(log.timestamp).toLocaleDateString()}</span></div>
                                              <p className="text-xs text-slate-500 font-medium italic">"{log.description}"</p>
                                          </div>
                                      ))}
                                  </div>
                              </div>
                          </div>
                      )}
                  </div>
                  <div className="p-6 border-t border-slate-50 bg-white">
                      <button onClick={handleAction} className={`w-full py-4 rounded-lg font-black uppercase text-xs tracking-widest shadow-lg ${activeModal === 'delete' ? 'bg-red-600 text-white' : 'bg-slate-900 text-white'}`}>Confirm {activeModal}</button>
                  </div>
              </div>
          </div>
      )}
    </div>
  );
}

function NavItem({ icon, label, active, onClick, count }) {
  return (
    <button onClick={onClick} className={`w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors duration-100 ${active ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}>
      <div className="flex items-center space-x-3">{icon}<span className="font-bold text-sm">{label}</span></div>
      {count > 0 && <span className={`px-2 py-0.5 text-[9px] font-black rounded-md ${active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'}`}>{count}</span>}
    </button>
  );
}

const MonitorView = ({ data }) => (
    <div className="space-y-8">
        <div className="flex justify-between items-end">
            <div><h3 className="text-xl font-black text-slate-900 uppercase">Mission Control</h3><p className="text-slate-400 font-bold text-[10px] uppercase tracking-widest mt-1">Live Fleet Status</p></div>
            <div className="flex gap-2">
                <StatusPill color="bg-emerald-500" label="Online" count={data.filter(p=>p.status==='online').length} />
                <StatusPill color="bg-indigo-500" label="Busy" count={data.filter(p=>p.status==='busy').length} />
            </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {data.map(p => (
                <div key={p.uid} className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm relative overflow-hidden group">
                    <div className={`absolute top-0 right-0 px-3 py-1 text-white font-bold text-[8px] uppercase tracking-widest rounded-bl-lg ${p.status === 'online' ? 'bg-emerald-500' : p.status === 'busy' ? 'bg-indigo-500' : 'bg-slate-300'}`}>{p.status}</div>
                    <div className="flex items-center gap-4 mb-4">
                        <img src={p.profileImage || 'https://via.placeholder.com/40'} className="w-12 h-12 rounded-xl object-cover ring-2 ring-slate-50 shadow-md" />
                        <div><h4 className="font-bold text-slate-900 uppercase text-xs">{p.name}</h4><p className="text-[9px] font-bold text-slate-400">{p.phoneNumber}</p></div>
                    </div>
                    {p.status === 'busy' ? (
                        <div className="p-3 bg-indigo-50 rounded-xl border border-indigo-100">
                            <p className="font-bold text-slate-800 text-[10px] truncate uppercase">{p.currentTask?.service}</p>
                            <div className="flex justify-between items-center mt-2"><span className="text-[8px] font-black text-indigo-400 uppercase tracking-widest">{p.currentTask?.status.replace('_', ' ')}</span></div>
                        </div>
                    ) : <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center italic text-slate-300 text-[10px] font-bold uppercase">{p.status === 'online' ? 'Waiting' : 'Offline'}</div>}
                </div>
            ))}
        </div>
    </div>
);

const StatusPill = ({ color, label, count }) => (
    <div className="px-4 py-2 bg-white border border-slate-100 rounded-full shadow-sm flex items-center gap-3"><div className={`w-1.5 h-1.5 rounded-full ${color}`}></div><span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider">{label}</span><span className="text-xs font-bold text-slate-900">{count}</span></div>
);

const AnalyticsView = ({ data }) => !data ? null : (
  <div className="space-y-8">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <StatCard title="Customers" value={data.totalUsers} color="text-indigo-600" />
        <StatCard title="Partners" value={data.totalProviders} color="text-emerald-600" />
        <StatCard title="Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} color="text-slate-900" />
        <StatCard title="Jobs" value={data.totalBookings} color="text-blue-600" />
      </div>
      <div className="bg-white p-8 rounded-3xl border border-slate-100 shadow-sm">
          <h3 className="font-bold text-slate-800 text-xs uppercase tracking-widest mb-8 italic">Category Matrix</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-6">
              {data.categories?.map((cat, i) => (
                  <div key={i}>
                      <div className="flex justify-between items-center mb-2"><span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{cat.name}</span><span className="text-xs font-black text-indigo-600 italic">{Math.round(cat.ratio * 100)}%</span></div>
                      <div className="w-full bg-slate-50 rounded-full h-1.5 overflow-hidden border border-slate-100"><div className="bg-indigo-500 h-full rounded-full transition-all duration-1000 shadow-sm" style={{ width: `${cat.ratio * 100}%` }}></div></div>
                  </div>
              ))}
          </div>
      </div>
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm transition-shadow hover:shadow-lg">
    <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest mb-1">{title}</p>
    <p className={`text-2xl font-black tracking-tight ${color}`}>{value || 0}</p>
  </div>
);

const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, onDetails, title, isPartner }) => (
  <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
    <div className="px-6 py-4 border-b border-slate-50 bg-white flex justify-between items-center"><h3 className="font-bold text-slate-800 text-xs uppercase tracking-widest italic">{title} Registry</h3><span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{users?.length || 0} Records</span></div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead><tr className="bg-slate-50/50 text-[9px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-100"><th className="px-6 py-4">Identity</th><th className="px-6 py-4 text-center">Status</th><th className="px-6 py-4 text-right">Console</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {!users || users.length === 0 ? (
            <tr><td colSpan="3" className="px-10 py-20 text-center text-slate-300 italic font-bold uppercase tracking-widest text-xs">Empty Database</td></tr>
          ) : (
            users.map(u => (
              <tr key={u.uid} className="hover:bg-slate-50/50 transition-colors">
                <td className="px-6 py-4"><div className="flex items-center space-x-3 cursor-pointer" onClick={()=>onDetails(u)}><img src={u.profileImage || 'https://via.placeholder.com/40'} className="w-10 h-10 rounded-lg object-cover border border-slate-100" /><div><p className="font-bold text-slate-800 text-sm">{u.name || 'Anonymous'}</p><p className="text-[9px] text-slate-400 font-bold uppercase">{u.phoneNumber}</p></div></div></td>
                <td className="px-6 py-4 text-center"><span className={`px-3 py-1 rounded-full text-[8px] font-black uppercase border transition-all ${u.isBlocked ? 'bg-red-50 border-red-100 text-red-500' : 'bg-emerald-50 border-emerald-100 text-emerald-500'}`}>{u.isBlocked ? 'Suspended' : 'Active'}</span></td>
                <td className="px-6 py-4 text-right space-x-1">
                  <button onClick={()=>onWarn(u)} className="p-2 bg-amber-50 text-amber-500 rounded-lg hover:bg-amber-500 hover:text-white transition-colors" title="Warning"><AlertTriangle size={14}/></button>
                  <button onClick={()=>onWallet(u)} className="p-2 bg-indigo-50 text-indigo-500 rounded-lg hover:bg-indigo-600 hover:text-white transition-colors" title="Wallet"><Wallet size={14}/></button>
                  <button onClick={()=>onBlock(u)} className={`p-2 rounded-lg transition-colors ${u.isBlocked ? 'bg-emerald-50 text-emerald-500 hover:bg-emerald-500 hover:text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-900 hover:text-white'}`} title="Block"><ShieldAlert size={14}/></button>
                  <button onClick={()=>onDelete(u)} className="p-2 bg-red-50 text-red-400 rounded-lg hover:bg-red-600 hover:text-white transition-colors" title="Delete"><Trash2 size={14}/></button>
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
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
    {providers.length === 0 ? (
       <div className="col-span-full p-20 bg-white rounded-3xl border-2 border-dashed border-slate-50 text-center text-slate-200 font-black uppercase tracking-widest text-sm">No Pending Approvals</div>
    ) : (
      providers.map(p => (
        <div key={p.uid} className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm relative group overflow-hidden">
          <button onClick={()=>onApprove(p.uid)} className="absolute top-4 right-4 p-2 bg-indigo-600 text-white rounded-lg shadow-lg font-bold uppercase text-[8px] tracking-widest hover:scale-105 transition-all">Verify</button>
          <div className="flex flex-col items-center text-center space-y-4 pt-4">
            <img src={p.profileImage || 'https://via.placeholder.com/60'} className="w-20 h-20 rounded-2xl object-cover ring-4 ring-slate-50" />
            <div><h4 className="font-bold text-slate-800 uppercase text-sm">{p.name}</h4><p className="text-[9px] font-black text-indigo-500 uppercase tracking-widest mt-1 italic">{p.profession}</p></div>
            <div className="w-full p-3 bg-slate-50 rounded-xl border border-slate-100 shadow-inner flex justify-between items-center"><p className="text-[8px] font-black text-slate-400 uppercase tracking-widest">Aadhaar: <span className="text-slate-700">{p.aadhaarNumber}</span></p><Eye size={14} className="text-indigo-400 cursor-pointer hover:text-indigo-600 transition-colors"/></div>
          </div>
        </div>
      ))
    )}
  </div>
);

const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
    <div className="p-6 border-b border-slate-50 font-black text-slate-800 text-xs uppercase tracking-widest bg-white">Payout Requests</div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead><tr className="bg-slate-50/50 text-[9px] font-bold text-slate-400 uppercase tracking-widest"><th className="px-6 py-4">Partner</th><th className="px-6 py-4 text-right">Liquidation</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {withdrawals.length === 0 ? (
            <tr><td colSpan="2" className="px-10 py-20 text-center text-slate-200 italic font-black uppercase tracking-widest text-xs">No Payouts</td></tr>
          ) : (
            withdrawals.map(w => (
              <tr key={w._id} className="hover:bg-slate-50/50 transition-colors">
                <td className="px-6 py-6 font-bold text-slate-800 uppercase text-xs italic tracking-tighter">{w.providerName}</td>
                <td className="px-6 py-6 text-right space-x-2"><span className="text-indigo-600 font-black italic text-lg mr-4">₹{w.amount}</span><button onClick={()=>onHandle(w._id, 'approved')} className="px-5 py-2 bg-slate-900 text-white text-[9px] font-black rounded-lg uppercase transition-all shadow-md active:scale-95">Release Funds</button></td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

const JobsTable = ({ jobs, title }) => (
  <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
    <div className="px-6 py-4 border-b font-bold uppercase text-[9px] tracking-widest bg-white text-slate-800 flex justify-between items-center"><span>{title}</span><div className="px-3 py-1 bg-indigo-50 text-indigo-600 rounded-full font-black italic tracking-widest">{jobs?.length || 0} Ops</div></div>
    <table className="w-full text-left">
      <thead><tr className="bg-slate-50/50 text-[9px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-50"><th className="px-6 py-4">Configuration</th><th className="px-6 py-4">Field Deployment</th><th className="px-6 py-4 text-right">Credit</th></tr></thead>
      <tbody className="divide-y divide-slate-50">
        {(!jobs || jobs.length === 0) ? (
          <tr><td colSpan="3" className="px-10 py-20 text-center text-slate-200 italic font-black uppercase text-xs">No Records</td></tr>
        ) : (
          jobs.map(j => (
            <tr key={j._id} className="hover:bg-slate-50/50 transition-colors">
              <td className="px-6 py-5"><p className="font-bold text-slate-800 uppercase text-xs tracking-tighter">{j.serviceName}</p><p className="text-[8px] font-bold text-slate-300 uppercase tracking-widest mt-1 italic">{new Date(j.createdAt).toLocaleDateString()}</p></td>
              <td className="px-6 py-5"><span className={`px-3 py-1 rounded-lg text-[8px] font-black uppercase border transition-all shadow-sm ${j.status === 'done' ? 'bg-emerald-50 border-emerald-100 text-emerald-500' : 'bg-indigo-50 border-indigo-100 text-indigo-500'}`}>{j.status.replace('_',' ')}</span></td>
              <td className="px-6 py-5 text-right font-black text-sm italic text-slate-900 tracking-tighter">₹{j.totalAmount}</td>
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
    <div className="bg-[#0F172A] p-10 rounded-3xl border border-white/5 max-w-xl shadow-2xl relative overflow-hidden">
      <h3 className="font-black uppercase text-[10px] tracking-widest mb-10 text-indigo-400 italic italic">Engine Logic</h3>
      <div className="flex justify-between items-center mb-10 relative z-10 text-white uppercase font-black text-xl">
          <div><p>Revenue Cut %</p><p className="text-slate-500 text-[10px] mt-2 font-medium italic normal-case tracking-normal opacity-70">Platform fee per successful task.</p></div>
          <div className="flex items-center space-x-4 bg-white/5 p-5 rounded-2xl border border-white/10"><input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-16 h-12 text-center bg-transparent font-black text-2xl text-indigo-400 outline-none" /><span className="font-black text-white/5 text-4xl italic select-none">%</span></div>
      </div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(()=>showToast('Logic Updated'))} className="w-full py-5 bg-indigo-600 text-white font-black rounded-xl uppercase text-xs tracking-widest shadow-xl hover:bg-white hover:text-indigo-600 transition-all duration-200">Synchronize Engine</button>
    </div>
  );
};
