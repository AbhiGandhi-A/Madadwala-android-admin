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
      showToast("Command Executed");
      setActiveModal(null);
    } catch (e) { showToast("Action failed", "error"); }
  };

  return (
    <div className="flex h-screen bg-[#F1F5F9] text-gray-900 overflow-hidden font-sans">
      {toast && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-[100]">
            <div className={`px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-4 border-2 bg-white ${toast.type === 'success' ? 'border-emerald-500 text-emerald-600' : 'border-red-500 text-red-600'}`}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center ${toast.type === 'success' ? 'bg-emerald-500' : 'bg-red-500'}`}><Check size={16} className="text-white" /></div>
                <span className="font-black uppercase text-xs tracking-widest italic">{toast.message}</span>
            </div>
        </div>
      )}

      <aside className="w-64 bg-[#020617] text-white flex flex-col shadow-2xl shrink-0 z-20">
        <div className="p-8 border-b border-white/5 font-black text-2xl tracking-tighter italic">MADADWALA</div>
        <nav className="flex-1 px-4 py-8 space-y-1 overflow-y-auto custom-scrollbar">
          <NavItem icon={<Activity size={18}/>} label="Mission Control" active={activeTab==='monitor'} onClick={()=>setActiveTab('monitor')} />
          <NavItem icon={<BarChart3 size={18}/>} label="Global Analytics" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <div className="pt-6 pb-2 px-2 text-[10px] font-black text-slate-500 uppercase tracking-widest">Management</div>
          <NavItem icon={<Users size={18}/>} label="Consumers" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={18}/>} label="Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={18}/>} label="Approvals" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />

          <div className="pt-6 pb-2 px-2 text-[10px] font-black text-slate-500 uppercase tracking-widest">Operations</div>
          <NavItem icon={<CreditCard size={18}/>} label="Payouts" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Navigation size={18}/>} label="Live Jobs" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={18}/>} label="Archives" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />

          <div className="pt-6 pb-2 px-2 text-[10px] font-black text-slate-500 uppercase tracking-widest">Feedback</div>
          <NavItem icon={<FileText size={18}/>} label="Violation Reports" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />
          <NavItem icon={<Star size={18}/>} label="User Reviews" active={activeTab==='reviews'} onClick={()=>setActiveTab('reviews')} />
          <NavItem icon={<Settings size={18}/>} label="Platform Rules" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
        <div className="p-4 bg-white/5 m-4 rounded-2xl">
            <button onClick={()=>openAction('broadcast')} className="w-full py-4 bg-indigo-600 hover:bg-indigo-700 text-white font-black text-[10px] uppercase tracking-widest rounded-xl transition-all flex items-center justify-center gap-2">
                <Zap size={14}/> Broadcast
            </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-20 bg-white border-b border-gray-200 flex items-center justify-between px-10 shrink-0 z-10">
          <h2 className="text-xl font-black text-slate-800 tracking-tight italic uppercase">{activeTab.replace('-', ' ')}</h2>
          <div className="w-12 h-12 rounded-2xl bg-slate-900 border-4 border-slate-50 flex items-center justify-center text-white font-black shadow-xl">A</div>
        </header>

        <main className="flex-1 overflow-y-auto p-10 custom-scrollbar bg-[#F8FAFC]">
          {loading ? (
             <div className="flex flex-col items-center justify-center h-full gap-4">
               <div className="w-10 h-10 border-4 border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div>
               <p className="text-slate-400 font-bold uppercase text-[10px] tracking-widest italic">Synchronizing...</p>
             </div>
          ) : (
            <div className="max-w-[1400px] mx-auto">
              {activeTab === 'monitor' && <MonitorView data={data.monitor} />}
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('customers'); showToast("Status Toggled");})} onDetails={(u)=>openAction('details', u)} title="Customers" />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('providers-all'); showToast("Status Toggled");})} onDetails={(u)=>openAction('details', u)} title="Partners" isPartner />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>{fetchTabData('providers-pending'); showToast("Verified");})} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>{fetchTabData('withdrawals'); showToast("Processed");})} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Real-time Tracking" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="Complete History" />}
              {activeTab === 'categories' && <CategoriesView categories={data.categories} refresh={()=>fetchTabData('categories')} />}
              {activeTab === 'reports' && <ReportsView reports={data.reports} />}
              {activeTab === 'reviews' && <ReviewsView reviews={data.reviews} onDelete={(id)=>adminApi.deleteReview(id).then(()=>{fetchTabData('reviews'); showToast("Removed");})} />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>{fetchTabData('settings'); showToast("Synced");}} />}
            </div>
          )}
        </main>
      </div>

      {activeModal && (
          <div className="fixed inset-0 z-50 overflow-hidden flex justify-end">
              <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={()=>setActiveModal(null)}></div>
              <div className="relative w-full max-w-lg bg-white shadow-[0_0_100px_rgba(0,0,0,0.3)] h-full flex flex-col">
                  <div className="px-10 py-8 border-b border-slate-50 flex items-center justify-between">
                      <h3 className="text-xl font-black text-slate-900 uppercase italic tracking-widest">{activeModal} module</h3>
                      <button onClick={()=>setActiveModal(null)} className="p-3 hover:bg-slate-50 rounded-full text-slate-400 transition-all"><X size={24}/></button>
                  </div>
                  <div className="flex-1 overflow-y-auto p-10 space-y-10 custom-scrollbar">
                      {selectedUser && activeModal !== 'broadcast' && (
                          <div className="flex items-center gap-6 p-6 bg-slate-50 rounded-3xl border border-slate-100 shadow-inner">
                              <img src={selectedUser.profileImage || 'https://via.placeholder.com/80'} className="w-16 h-16 rounded-2xl object-cover ring-4 ring-white shadow-xl" />
                              <div><p className="font-black text-slate-900 text-xl tracking-tight uppercase italic">{selectedUser.name}</p><p className="text-[11px] text-slate-400 font-bold uppercase tracking-widest mt-1">{selectedUser.phoneNumber}</p></div>
                          </div>
                      )}

                      {activeModal === 'warning' && (
                          <div className="space-y-6">
                              <div className="space-y-2">
                                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-2">VIOLATION CLASS</label>
                                  <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="e.g. Terms Violation" className="w-full p-5 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-2xl outline-none font-black text-sm tracking-widest shadow-inner transition-all" />
                              </div>
                              <div className="space-y-2">
                                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-2">INCIDENT DEBRIEF</label>
                                  <textarea rows={8} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Write detailed warning..." className="w-full p-6 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-[24px] outline-none font-bold text-slate-600 shadow-inner resize-none transition-all" />
                              </div>
                          </div>
                      )}

                      {activeModal === 'wallet' && (
                          <div className="space-y-10">
                              <div className="flex gap-4">
                                  <button onClick={()=>setFormState({...formState, type: 'credit'})} className={`flex-1 py-6 rounded-[24px] font-black uppercase text-xs tracking-widest border-2 transition-all ${formState.type === 'credit' ? 'bg-emerald-500 border-emerald-500 text-white shadow-xl shadow-emerald-200' : 'bg-slate-50 border-transparent text-slate-300'}`}>Inject (+)</button>
                                  <button onClick={()=>setFormState({...formState, type: 'debit'})} className={`flex-1 py-6 rounded-[24px] font-black uppercase text-xs tracking-widest border-2 transition-all ${formState.type === 'debit' ? 'bg-red-500 border-red-500 text-white shadow-xl shadow-red-200' : 'bg-slate-50 border-transparent text-slate-300'}`}>Extract (-)</button>
                              </div>
                              <div className="space-y-2">
                                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-2">ASSET VALUATION (₹)</label>
                                  <input type="number" value={formState.amount} onChange={e=>setFormState({...formState, amount: e.target.value})} placeholder="0.00" className="w-full p-8 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-[32px] outline-none font-black text-5xl tracking-tighter shadow-inner transition-all italic" />
                              </div>
                              <input value={formState.description} onChange={e=>setFormState({...formState, description: e.target.value})} placeholder="Internal Reason Note" className="w-full p-5 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-2xl outline-none font-bold shadow-inner transition-all" />
                          </div>
                      )}

                      {activeModal === 'broadcast' && (
                          <div className="space-y-6">
                              <div className="space-y-2">
                                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-2">TARGET AUDIENCE</label>
                                  <select value={formState.role} onChange={e=>setFormState({...formState, role: e.target.value})} className="w-full p-5 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-2xl outline-none font-black appearance-none cursor-pointer shadow-inner uppercase tracking-widest text-xs">
                                      <option value="all">Universal (All Nodes)</option>
                                      <option value="customer">Consumer Class</option>
                                      <option value="provider">Partner Fleet</option>
                                  </select>
                              </div>
                              <div className="space-y-2">
                                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-2">MISSION HEADLINE</label>
                                  <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="Urgent Broadcast" className="w-full p-5 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-2xl outline-none font-black text-sm shadow-inner transition-all" />
                              </div>
                              <div className="space-y-2">
                                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-2">DATA PACKAGE</label>
                                  <textarea rows={8} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Broadcast body message..." className="w-full p-6 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-[24px] outline-none font-bold text-slate-600 shadow-inner resize-none transition-all" />
                              </div>
                          </div>
                      )}

                      {activeModal === 'delete' && (
                          <div className="text-center space-y-10 py-10">
                              <div className="w-24 h-24 bg-red-50 rounded-full flex items-center justify-center mx-auto text-red-500 shadow-inner"><Trash2 size={48}/></div>
                              <div className="space-y-4">
                                  <h4 className="text-3xl font-black text-slate-900 uppercase tracking-tighter italic">ACCOUNT PURGE</h4>
                                  <p className="text-slate-400 font-medium leading-relaxed italic opacity-80">This will permanently incinerate all data associated with this user node. Operation is irreversible.</p>
                              </div>
                          </div>
                      )}

                      {activeModal === 'details' && selectedUser && (
                          <div className="space-y-12">
                              <div className="grid grid-cols-2 gap-8">
                                  <div className="p-8 bg-indigo-600 rounded-[35px] text-white shadow-2xl shadow-indigo-200">
                                      <p className="text-[9px] font-black uppercase tracking-widest opacity-60 mb-2">Earnings</p>
                                      <p className="text-3xl font-black italic tracking-tighter">₹{selectedUser.totalEarnings || 0}</p>
                                  </div>
                                  <div className="p-8 bg-slate-900 rounded-[35px] text-white shadow-2xl shadow-slate-200">
                                      <p className="text-[9px] font-black uppercase tracking-widest opacity-60 mb-2">Ops Count</p>
                                      <p className="text-3xl font-black italic tracking-tighter">{selectedUser.totalJobs || 0}</p>
                                  </div>
                              </div>
                              <div className="space-y-6">
                                  <h4 className="text-[11px] font-black text-slate-900 uppercase tracking-[4px] italic border-b pb-4">Activity Stream</h4>
                                  <div className="space-y-5">
                                      {selectedUser.activityLog?.slice(-10).reverse().map((log, i) => (
                                          <div key={i} className="p-5 bg-white border border-slate-100 rounded-[22px] shadow-sm flex items-start gap-4">
                                              <div className="w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 shrink-0"></div>
                                              <div><div className="flex justify-between items-center mb-1"><span className="text-[10px] font-black text-indigo-600 uppercase italic">{log.event}</span><span className="text-[9px] font-bold text-slate-300">{new Date(log.timestamp).toLocaleTimeString()}</span></div><p className="text-xs text-slate-500 font-medium italic">"{log.description}"</p></div>
                                          </div>
                                      ))}
                                      {(!selectedUser.activityLog || selectedUser.activityLog.length === 0) && <p className="text-center py-20 text-slate-300 font-black uppercase text-[10px] tracking-widest italic">Zero Active Logs</p>}
                                  </div>
                              </div>
                          </div>
                      )}
                  </div>
                  <div className="p-10 border-t border-slate-50 bg-white shadow-[0_-20px_60px_rgba(0,0,0,0.02)]">
                      {activeModal !== 'details' ? (
                          <button onClick={handleAction} className={`w-full py-6 rounded-[28px] font-black uppercase text-xs tracking-[5px] shadow-2xl transition-all hover:-translate-y-1 active:scale-95 ${activeModal === 'delete' ? 'bg-red-600 text-white shadow-red-200' : 'bg-slate-900 text-white shadow-slate-300'}`}>Confirm {activeModal}</button>
                      ) : (
                          <button onClick={()=>setActiveModal(null)} className="w-full py-6 bg-slate-50 text-slate-400 rounded-[28px] font-black uppercase text-xs tracking-[5px] hover:bg-slate-100 transition-all">Dismiss Panel</button>
                      )}
                  </div>
              </div>
          </div>
      )}
    </div>
  );
}

function NavItem({ icon, label, active, onClick, count }) {
  return (
    <button onClick={onClick} className={`w-full flex items-center justify-between px-5 py-3.5 rounded-2xl transition-colors duration-100 ${active ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}>
      <div className="flex items-center space-x-3">{icon}<span className="font-bold text-sm tracking-tight">{label}</span></div>
      {count > 0 && <span className={`px-2 py-0.5 text-[9px] font-black rounded-lg ${active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'}`}>{count}</span>}
    </button>
  );
}

const MonitorView = ({ data }) => (
    <div className="space-y-12 animate-in fade-in duration-200">
        <div className="flex justify-between items-end">
            <div><h3 className="text-3xl font-black text-slate-900 tracking-tighter uppercase italic">Mission Control</h3><p className="text-slate-400 font-black text-[10px] uppercase tracking-[5px] mt-2">Live Fleet Operations & Monitoring</p></div>
            <div className="flex gap-4">
                <StatusPill color="bg-emerald-500" label="Online" count={data.filter(p=>p.status==='online').length} />
                <StatusPill color="bg-indigo-500" label="Busy" count={data.filter(p=>p.status==='busy').length} />
                <StatusPill color="bg-slate-300" label="Offline" count={data.filter(p=>p.status==='offline').length} />
            </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {data.map(p => (
                <div key={p.uid} className="bg-white p-8 rounded-[40px] border border-slate-100 shadow-sm relative overflow-hidden transition-shadow hover:shadow-xl group">
                    <div className={`absolute top-0 right-0 px-5 py-2 text-white font-black text-[8px] uppercase tracking-widest rounded-bl-[20px] ${p.status === 'online' ? 'bg-emerald-500' : p.status === 'busy' ? 'bg-indigo-500' : 'bg-slate-300'}`}>{p.status}</div>
                    <div className="flex items-center gap-5 mb-8">
                        <img src={p.profileImage || 'https://via.placeholder.com/80'} className="w-14 h-14 rounded-2xl object-cover ring-4 ring-slate-50 shadow-xl group-hover:scale-105 transition-all" />
                        <div><h4 className="font-black text-slate-900 uppercase tracking-tight text-sm">{p.name}</h4><p className="text-[9px] font-black text-slate-400 uppercase tracking-widest mt-0.5">{p.phoneNumber}</p></div>
                    </div>
                    {p.status === 'busy' ? (
                        <div className="p-4 bg-indigo-50 rounded-[24px] border border-indigo-100 shadow-inner">
                            <p className="font-black text-slate-800 text-[10px] truncate uppercase italic tracking-tighter">TASK: {p.currentTask?.service}</p>
                            <div className="flex justify-between items-center mt-3"><span className="text-[8px] font-black text-indigo-400 uppercase tracking-widest">{p.currentTask?.status.replace('_', ' ')}</span><button className="text-[8px] font-black text-indigo-600 underline tracking-widest">DETAILS</button></div>
                        </div>
                    ) : <div className="p-4 bg-slate-50 rounded-[24px] border border-slate-100 flex items-center justify-center italic text-slate-300 text-[10px] font-black uppercase tracking-widest">{p.status === 'online' ? 'Awaiting Data' : 'Node Offline'}</div>}
                </div>
            ))}
        </div>
    </div>
);

const StatusPill = ({ color, label, count }) => (
    <div className="px-6 py-3 bg-white border border-slate-100 rounded-full shadow-sm flex items-center gap-3"><div className={`w-2 h-2 rounded-full ${color}`}></div><span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{label}</span><span className="text-sm font-black text-slate-900">{count}</span></div>
);

const AnalyticsView = ({ data }) => !data ? null : (
  <div className="space-y-12 animate-in fade-in duration-200">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
        <StatCard title="Global Consumers" value={data.totalUsers} color="text-indigo-600" />
        <StatCard title="Active Partners" value={data.totalProviders} color="text-emerald-600" />
        <StatCard title="Total Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} color="text-slate-900" />
        <StatCard title="Ops Completed" value={data.totalBookings} color="text-blue-600" />
      </div>
      <div className="bg-white p-12 rounded-[45px] border border-slate-100 shadow-sm">
          <h3 className="font-black text-slate-900 uppercase tracking-[8px] italic text-xs mb-12 opacity-40 text-center">Resource Allocation Matrix</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-16 gap-y-10">
              {data.categories?.map((cat, i) => (
                  <div key={i}>
                      <div className="flex justify-between items-end mb-3"><p className="text-[10px] font-black text-slate-400 uppercase tracking-[4px]">{cat.name}</p><p className="text-2xl font-black text-indigo-600 italic tracking-tighter">{Math.round(cat.ratio * 100)}%</p></div>
                      <div className="w-full bg-slate-50 rounded-full h-2.5 overflow-hidden border border-slate-100"><div className="bg-indigo-500 h-full rounded-full transition-all duration-1000 shadow-lg shadow-indigo-100" style={{ width: `${cat.ratio * 100}%` }}></div></div>
                  </div>
              ))}
          </div>
      </div>
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-8 rounded-[35px] border border-slate-100 shadow-sm transition-all hover:shadow-xl hover:-translate-y-1">
    <p className="text-[10px] font-black text-slate-400 uppercase tracking-[4px] mb-2">{title}</p>
    <p className={`text-4xl font-black tracking-tighter ${color}`}>{value || 0}</p>
  </div>
);

const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, onDetails, title, isPartner }) => (
  <div className="bg-white rounded-[40px] border border-slate-100 shadow-sm overflow-hidden animate-in fade-in duration-200">
    <div className="px-10 py-6 border-b border-slate-50 bg-white flex justify-between items-center"><h3 className="font-black text-slate-800 text-xs uppercase tracking-[6px] italic">{title} Registry</h3><span className="px-4 py-1 bg-slate-50 rounded-full text-[9px] font-black text-slate-400 uppercase tracking-[2px]">{users?.length || 0} SECURE NODES</span></div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead><tr className="bg-slate-50/50 text-[9px] font-black text-slate-400 uppercase tracking-[4px] border-b border-slate-50"><th className="px-10 py-6">Core Identity</th><th className="px-10 py-6 text-center">Protocol Status</th><th className="px-10 py-6 text-right">System Console</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {!users || users.length === 0 ? (
            <tr><td colSpan="3" className="px-10 py-32 text-center text-slate-200 italic font-black uppercase tracking-[5px] text-xs">Repository Empty</td></tr>
          ) : (
            users.map(u => (
              <tr key={u.uid} className="hover:bg-slate-50/50 transition-colors group">
                <td className="px-10 py-6"><div className="flex items-center space-x-5 cursor-pointer" onClick={()=>onDetails(u)}><img src={u.profileImage || 'https://via.placeholder.com/48'} className="w-14 h-14 rounded-2xl object-cover border-4 border-white shadow-xl transition-transform group-hover:scale-110" /><div><p className="font-black text-slate-800 group-hover:text-indigo-600 transition-colors text-lg tracking-tight italic uppercase">{u.name || 'ANONYMOUS'}</p><p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">{u.phoneNumber}</p></div></div></td>
                <td className="px-10 py-6 text-center"><span className={`px-5 py-1.5 rounded-full text-[9px] font-black uppercase border-2 transition-all shadow-md ${u.isBlocked ? 'bg-red-50 border-red-100 text-red-500 shadow-red-100' : 'bg-emerald-50 border-emerald-100 text-emerald-500 shadow-emerald-100'}`}>{u.isBlocked ? 'SUSPENDED' : 'AUTHORIZED'}</span></td>
                <td className="px-10 py-6 text-right space-x-2">
                  <button onClick={()=>onWarn(u)} className="p-3.5 bg-amber-50 text-amber-500 rounded-2xl hover:bg-amber-500 hover:text-white transition-all shadow-sm" title="Issue Warning"><AlertTriangle size={18}/></button>
                  <button onClick={()=>onWallet(u)} className="p-3.5 bg-indigo-50 text-indigo-500 rounded-2xl hover:bg-indigo-600 hover:text-white transition-all shadow-sm" title="Wallet Sync"><Wallet size={18}/></button>
                  <button onClick={()=>onBlock(u)} className={`p-3.5 rounded-2xl transition-all shadow-sm ${u.isBlocked ? 'bg-emerald-50 text-emerald-500 hover:bg-emerald-500 hover:text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-900 hover:text-white'}`} title="Block/Unblock"><ShieldAlert size={18}/></button>
                  <button onClick={()=>onDelete(u)} className="p-3.5 bg-red-50 text-red-400 rounded-2xl hover:bg-red-600 hover:text-white transition-all shadow-sm" title="Purge Node"><Trash2 size={18}/></button>
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
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 animate-in fade-in duration-200">
    {providers.length === 0 ? (
       <div className="col-span-full p-40 bg-white rounded-[50px] border-4 border-dashed border-slate-50 text-center text-slate-200 font-black uppercase tracking-[15px] text-sm italic">Status Nominal</div>
    ) : (
      providers.map(p => (
        <div key={p.uid} className="bg-white p-10 rounded-[45px] shadow-sm border border-slate-100 relative group overflow-hidden hover:shadow-2xl transition-all duration-300 text-center">
          <button onClick={()=>onApprove(p.uid)} className="absolute top-6 right-6 p-3 bg-indigo-600 text-white rounded-2xl shadow-xl font-black uppercase text-[9px] tracking-widest hover:scale-105 active:scale-95 transition-all">Verify Node</button>
          <img src={p.profileImage || 'https://via.placeholder.com/100'} className="w-24 h-24 rounded-[35px] object-cover ring-[10px] ring-slate-50 shadow-2xl mx-auto mb-8 transition-transform group-hover:rotate-6" />
          <h4 className="font-black text-2xl tracking-tighter text-slate-800 uppercase italic">{p.name}</h4>
          <p className="text-[10px] font-black text-indigo-500 uppercase tracking-[4px] mt-2 mb-8 italic opacity-60">{p.profession}</p>
          <div className="p-5 bg-slate-50 rounded-[28px] border border-slate-100 shadow-inner flex justify-between items-center"><p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">ID: <span className="text-slate-700 tracking-tighter">{p.aadhaarNumber}</span></p><Eye size={18} className="text-indigo-400 cursor-pointer hover:scale-125 transition-all"/></div>
        </div>
      ))
    )}
  </div>
);

const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-[40px] border border-slate-100 shadow-sm overflow-hidden animate-in fade-in duration-200">
    <div className="px-10 py-8 border-b border-slate-50 font-black text-slate-800 text-xs uppercase tracking-[8px] italic bg-white">Partner Liquidation Request Queue</div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead><tr className="bg-slate-50/50 text-[9px] font-black text-slate-400 uppercase tracking-[4px]"><th className="px-12 py-8">Account Identity</th><th className="px-12 py-8 text-right">Liquidation Authorization</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {withdrawals.length === 0 ? (
            <tr><td colSpan="2" className="px-10 py-32 text-center text-slate-200 italic font-black uppercase tracking-[10px] text-xs animate-pulse">Waiting for Liquidation Events</td></tr>
          ) : (
            withdrawals.map(w => (
              <tr key={w._id} className="hover:bg-slate-50/50 transition-colors">
                <td className="px-12 py-10 font-black text-slate-800 uppercase tracking-tighter text-2xl italic leading-none">{w.providerName}</td>
                <td className="px-12 py-10 text-right space-x-6"><span className="text-indigo-600 font-black italic text-3xl tracking-tighter mr-6">₹{w.amount}</span><button onClick={()=>onHandle(w._id, 'approved')} className="px-10 py-4 bg-slate-900 text-white text-[10px] font-black rounded-[22px] uppercase tracking-[4px] shadow-2xl transition-all hover:bg-indigo-600 hover:-translate-y-1 active:scale-95">Release Assets</button></td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

const JobsTable = ({ jobs, title }) => (
  <div className="bg-white rounded-[40px] border border-slate-100 shadow-sm overflow-hidden animate-in fade-in duration-200">
    <div className="px-10 py-6 border-b font-black uppercase text-[10px] tracking-[6px] bg-white text-slate-800 flex justify-between items-center italic"><span>{title}</span><div className="px-5 py-2 bg-indigo-50 text-indigo-600 rounded-full font-black italic tracking-widest">{jobs?.length || 0} TOTAL OPS</div></div>
    <table className="w-full text-left border-collapse">
      <thead><tr className="bg-slate-50/50 text-[9px] font-black text-slate-400 uppercase tracking-[4px] border-b border-slate-50"><th className="px-12 py-8">Mission Package</th><th className="px-12 py-8">Deployment State</th><th className="px-12 py-8 text-right">Asset Valuation</th></tr></thead>
      <tbody className="divide-y divide-slate-50">
        {(!jobs || jobs.length === 0) ? (
          <tr><td colSpan="3" className="px-10 py-40 text-center text-slate-100 italic font-black uppercase tracking-[20px]">Null Set</td></tr>
        ) : (
          jobs.map(j => (
            <tr key={j._id} className="hover:bg-slate-50/50 transition-colors group">
              <td className="px-12 py-10"><p className="font-black text-slate-800 uppercase tracking-tighter text-2xl italic leading-none group-hover:text-indigo-600 transition-colors">{j.serviceName}</p><p className="text-[9px] font-black text-slate-300 uppercase tracking-[3px] mt-2 italic">{new Date(j.createdAt).toLocaleString()}</p></td>
              <td className="px-12 py-10"><span className={`px-6 py-2 rounded-full text-[10px] font-black uppercase border-2 transition-all shadow-lg italic ${j.status === 'done' ? 'bg-emerald-50 border-emerald-100 text-emerald-500 shadow-emerald-50' : 'bg-indigo-50 border-indigo-100 text-indigo-500 shadow-indigo-50'}`}>{j.status.replace('_',' ')}</span></td>
              <td className="px-12 py-10 text-right font-black text-4xl italic tracking-tighter text-slate-900 opacity-80 group-hover:opacity-100">₹{j.totalAmount}</td>
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
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-12 animate-in fade-in duration-200">
      <div className="bg-[#0F172A] p-12 rounded-[50px] shadow-2xl h-fit border border-white/5 relative overflow-hidden text-center">
        <h3 className="font-black mb-12 uppercase text-[10px] tracking-[6px] text-indigo-400 italic">Initialize Core Asset</h3>
        <div className="space-y-8">
            <input value={n} onChange={e=>setN(e.target.value)} placeholder="VECTOR_NAME" className="w-full p-5 bg-white/5 border-2 border-white/10 rounded-[28px] font-black text-white outline-none focus:border-indigo-500 uppercase tracking-widest text-xs shadow-inner" />
            <input value={i} onChange={e=>setI(e.target.value)} placeholder="URI_LOCATION" className="w-full p-5 bg-white/5 border-2 border-white/10 rounded-[28px] font-bold text-white outline-none focus:border-indigo-500 text-xs shadow-inner" />
            <button onClick={handleAdd} className="w-full py-7 bg-indigo-600 text-white font-black rounded-[35px] uppercase text-xs tracking-[8px] shadow-2xl shadow-indigo-900/50 hover:bg-white hover:text-indigo-600 transition-all active:scale-95">Deploy Module</button>
        </div>
      </div>
      <div className="lg:col-span-2 grid grid-cols-2 md:grid-cols-3 gap-8">
        {categories.map(c => (
            <div key={c._id} className="bg-white p-10 rounded-[50px] border border-slate-100 shadow-sm flex flex-col items-center transition-all hover:shadow-2xl hover:scale-105 group relative overflow-hidden">
                <div className="w-24 h-24 bg-slate-50 rounded-[40px] flex items-center justify-center mb-8 shadow-inner transition-transform group-hover:rotate-12 duration-500"><img src={c.icon || 'https://via.placeholder.com/80'} className="w-16 h-16 object-contain drop-shadow-2xl" /></div>
                <p className="font-black text-slate-800 uppercase tracking-[4px] text-xs text-center leading-relaxed italic">{c.name}</p>
                <button onClick={()=>adminApi.deleteCategory(c._id).then(refresh)} className="absolute top-6 right-6 p-3 bg-red-50 text-red-400 rounded-full opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-500 hover:text-white shadow-xl"><Trash2 size={16}/></button>
            </div>
        ))}
      </div>
    </div>
  );
};

const ReportsView = ({ reports }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-10 animate-in fade-in duration-200 text-left">
    {reports.length === 0 ? <div className="col-span-full p-60 bg-white rounded-[70px] border-4 border-dashed border-slate-50 text-center text-slate-100 font-black uppercase tracking-[40px] italic">NOMINAL</div> : reports.map(r => (
      <div key={r._id} className="bg-white p-12 rounded-[60px] border-l-[20px] border-red-500 shadow-2xl relative overflow-hidden group hover:shadow-red-100 transition-all duration-500">
        <div className="absolute top-0 right-0 px-8 py-3 bg-red-600 text-white text-[10px] font-black uppercase rounded-bl-[35px] tracking-[6px] shadow-2xl">{r.status}</div>
        <h4 className="font-black text-slate-900 uppercase text-2xl tracking-tighter mb-8 italic">Incident: {r.reason}</h4>
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
  <div className="bg-white rounded-[60px] border border-slate-100 shadow-sm overflow-hidden text-left animate-in fade-in duration-200">
    <div className="px-12 py-10 border-b border-slate-50 bg-white font-black text-slate-800 text-[10px] uppercase tracking-[12px] italic text-center opacity-40">Sentiment Analysis Registry</div>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead><tr className="bg-slate-50/50 text-[9px] font-black text-slate-400 uppercase tracking-[6px] border-b border-slate-50"><th className="px-12 py-8">Reviewer Authority</th><th className="px-12 py-8 text-left">Analytical Debrief</th><th className="px-12 py-8 text-right">System Control</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {reviews.map(r => (
            <tr key={r._id} className="hover:bg-slate-50/50 transition-all group">
              <td className="px-12 py-12 font-black text-slate-900 uppercase tracking-tighter text-3xl italic leading-none">{r.customerName}
                  <div className="flex gap-1.5 mt-5">
                      {[...Array(5)].map((_, i) => <Star key={i} size={16} className={i < r.rating ? 'text-amber-400 fill-amber-400 drop-shadow-xl' : 'text-slate-100'} />)}
                  </div>
              </td>
              <td className="px-12 py-12"><div className="p-8 bg-slate-50 rounded-[45px] border border-slate-100 shadow-inner italic font-bold text-slate-500 text-lg leading-relaxed group-hover:bg-white transition-colors duration-300 shadow-[inset_0_4px_10px_rgba(0,0,0,0.02)]">"{r.comment}"</div></td>
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
    <div className="bg-[#0F172A] p-20 rounded-[80px] border-[12px] border-white/5 max-w-4xl shadow-[0_80px_200px_-50px_rgba(0,0,0,0.6)] relative overflow-hidden group animate-in fade-in duration-200 mx-auto">
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-500/5 rounded-full -mr-[250px] -mt-[250px] blur-[150px] group-hover:bg-indigo-500/10 transition-all duration-1000"></div>
      <h3 className="font-black uppercase text-[11px] tracking-[15px] mb-20 text-indigo-500 italic text-center">Central Logic Core</h3>
      <div className="flex justify-between items-center mb-20 relative z-10 text-white uppercase font-black text-5xl tracking-tighter italic">
          <div className="space-y-4"><p className="leading-none">Revenue Friction</p><p className="text-slate-600 text-sm font-black tracking-[4px] normal-case opacity-80 leading-relaxed border-l-4 border-indigo-600 pl-6">Platform retainment factor per transaction node.</p></div>
          <div className="flex items-center space-x-8 bg-white/5 p-12 rounded-[60px] border-2 border-white/10 shadow-[0_30px_100px_rgba(0,0,0,0.4)] hover:border-indigo-500/30 transition-all duration-500 group/input">
              <input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-40 h-40 text-center bg-transparent font-black text-8xl text-indigo-500 outline-none placeholder-indigo-900 group-hover/input:scale-110 transition-transform duration-500" />
              <span className="font-black text-white/5 text-[140px] italic select-none leading-none">%</span>
          </div>
      </div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(()=>showToast('Core Synced'))} className="w-full py-10 bg-indigo-600 text-white font-black rounded-[45px] uppercase text-sm tracking-[12px] shadow-[0_40px_100px_rgba(79,70,229,0.4)] hover:bg-white hover:text-indigo-600 hover:-translate-y-4 active:scale-95 transition-all duration-500 relative z-10 italic">SYNCHRONIZE UNIVERSAL ENGINE</button>
    </div>
  );
};
