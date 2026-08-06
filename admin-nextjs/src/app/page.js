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
    if (toast) { const timer = setTimeout(() => setToast(null), 3000); return () => clearTimeout(timer); }
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
      showToast("Command Executed Successfully");
      setActiveModal(null);
    } catch (e) { showToast("System Execution Failure", "error"); }
  };

  return (
    <div className="flex h-screen bg-[#F1F5F9] text-gray-900 overflow-hidden font-sans">
      {toast && (
        <div className="fixed top-10 left-1/2 -translate-x-1/2 z-[100] animate-in fade-in slide-in-from-top-10 duration-500">
            <div className={`px-10 py-5 rounded-[30px] shadow-[0_30px_60px_-15px_rgba(0,0,0,0.3)] flex items-center gap-4 border-b-8 ${toast.type === 'success' ? 'bg-white border-emerald-500 text-emerald-600' : 'bg-white border-red-500 text-red-600'}`}>
                <div className={`w-10 h-10 rounded-full flex items-center justify-center ${toast.type === 'success' ? 'bg-emerald-500' : 'bg-red-500'}`}><Check size={20} className="text-white" /></div>
                <span className="font-black uppercase text-sm tracking-[2px] italic">{toast.message}</span>
            </div>
        </div>
      )}

      <aside className="w-72 bg-[#020617] text-white flex flex-col shadow-2xl shrink-0 z-20">
        <div className="p-10 border-b border-white/5 font-black text-2xl tracking-tighter flex items-center gap-3">
            <div className="w-10 h-10 bg-indigo-500 rounded-2xl flex items-center justify-center shadow-lg shadow-indigo-500/50">M</div>
            MADADWALA
        </div>
        <nav className="flex-1 px-5 py-10 space-y-2 overflow-y-auto custom-scrollbar">
          <NavItem icon={<Activity size={18}/>} label="Mission Control" active={activeTab==='monitor'} onClick={()=>setActiveTab('monitor')} pulse />
          <NavItem icon={<BarChart3 size={18}/>} label="Global Analytics" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <div className="pt-8 pb-3 px-3 text-[10px] font-black text-slate-500 uppercase tracking-[4px]">Human Resources</div>
          <NavItem icon={<Users size={18}/>} label="Consumer Base" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={18}/>} label="Partner Network" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={18}/>} label="Verification Desk" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />

          <div className="pt-8 pb-3 px-3 text-[10px] font-black text-slate-500 uppercase tracking-[4px]">Logistics & Cash</div>
          <NavItem icon={<CreditCard size={18}/>} label="Financial Payouts" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Navigation size={18}/>} label="Live Operations" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={18}/>} label="Service History" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />

          <div className="pt-8 pb-3 px-3 text-[10px] font-black text-slate-500 uppercase tracking-[4px]">System Integrity</div>
          <NavItem icon={<FileText size={18}/>} label="Violation Reports" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />
          <NavItem icon={<Star size={18}/>} label="User Sentiment" active={activeTab==='reviews'} onClick={()=>setActiveTab('reviews')} />
          <NavItem icon={<Settings size={18}/>} label="Platform Engine" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
        <div className="p-6">
            <button onClick={()=>openAction('broadcast')} className="w-full py-5 bg-indigo-600 hover:bg-white hover:text-indigo-600 text-white font-black text-[10px] uppercase tracking-[4px] rounded-3xl transition-all flex items-center justify-center gap-3 shadow-2xl shadow-indigo-500/20">
                <Zap size={16}/> Push Broadcast
            </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0 bg-[#F8FAFC]">
        <header className="h-24 bg-white border-b border-slate-100 flex items-center justify-between px-12 shrink-0 z-10">
          <h2 className="text-2xl font-black text-slate-900 tracking-tighter italic uppercase">{activeTab.replace('-', ' ')}</h2>
          <div className="flex items-center gap-6">
              {error && <div className="text-red-500 text-[10px] font-black bg-red-50 px-5 py-2.5 rounded-full border border-red-100 flex items-center gap-2 animate-pulse uppercase tracking-widest">{error}</div>}
              <div className="flex items-center gap-3 pr-6 border-r border-slate-100">
                  <div className="text-right">
                      <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Admin Authorization</p>
                      <p className="text-xs font-bold text-slate-900">ABHI GANDHI</p>
                  </div>
                  <div className="w-14 h-14 rounded-[22px] bg-slate-900 border-[6px] border-slate-50 flex items-center justify-center text-white font-black shadow-2xl">AG</div>
              </div>
              <button className="p-4 hover:bg-slate-50 rounded-full text-slate-400 transition-colors"><Bell size={24}/></button>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-12 custom-scrollbar">
          {loading ? (
             <div className="flex flex-col items-center justify-center h-full gap-6">
               <div className="w-20 h-20 border-8 border-indigo-50 border-t-indigo-600 rounded-[35px] animate-spin shadow-2xl shadow-indigo-100"></div>
               <p className="text-slate-400 font-black uppercase text-[10px] tracking-[6px] animate-pulse">Synchronizing Core...</p>
             </div>
          ) : (
            <div className="max-w-[1600px] mx-auto animate-in fade-in slide-in-from-bottom-8 duration-700">
              {activeTab === 'monitor' && <MonitorView data={data.monitor} />}
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('customers'); showToast("Status Toggled");})} onDetails={(u)=>openAction('details', u)} title="Customers" />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>{fetchTabData('providers-all'); showToast("Status Toggled");})} onDetails={(u)=>openAction('details', u)} title="Partners" isPartner />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>{fetchTabData('providers-pending'); showToast("Partner Verified");})} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>{fetchTabData('withdrawals'); showToast("Payout Processed");})} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Mission Real-time Log" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="Historical Archives" />}
              {activeTab === 'reports' && <ReportsView reports={data.reports} />}
              {activeTab === 'reviews' && <ReviewsView reviews={data.reviews} onDelete={(id)=>adminApi.deleteReview(id).then(()=>{fetchTabData('reviews'); showToast("Data Purged");})} />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>{fetchTabData('settings'); showToast("Engine Synced");}} />}
            </div>
          )}
        </main>
      </div>

      {activeModal && (
          <div className="fixed inset-0 z-50 overflow-hidden">
              <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-md transition-opacity" onClick={()=>setActiveModal(null)}></div>
              <div className="absolute inset-y-0 right-0 max-w-xl w-full bg-white shadow-[0_0_100px_rgba(0,0,0,0.5)] transform transition-transform duration-500 animate-in slide-in-from-right ease-out">
                  <div className="h-full flex flex-col">
                      <div className="px-12 py-10 border-b border-slate-50 flex items-center justify-between bg-white">
                          <h3 className="text-2xl font-black text-slate-900 uppercase tracking-tighter italic">
                              {activeModal.toUpperCase()} MODULE
                          </h3>
                          <button onClick={()=>setActiveModal(null)} className="p-4 hover:bg-slate-100 rounded-full text-slate-400 transition-all active:scale-90"><X size={28}/></button>
                      </div>
                      <div className="flex-1 overflow-y-auto p-12 space-y-12 custom-scrollbar">
                          {selectedUser && activeModal !== 'broadcast' && (
                              <div className="flex items-center gap-6 p-8 bg-[#F8FAFC] rounded-[40px] border-2 border-white shadow-xl shadow-slate-200/50">
                                  <img src={selectedUser.profileImage || 'https://via.placeholder.com/80'} className="w-20 h-20 rounded-[30px] object-cover ring-8 ring-white shadow-2xl" />
                                  <div>
                                      <p className="font-black text-slate-900 text-2xl tracking-tighter uppercase">{selectedUser.name}</p>
                                      <p className="text-[11px] text-indigo-500 font-black uppercase tracking-[3px] mt-1">{selectedUser.phoneNumber}</p>
                                  </div>
                              </div>
                          )}

                          {activeModal === 'warning' && (
                              <div className="space-y-10 animate-in fade-in slide-in-from-bottom-4 duration-500">
                                  <div className="space-y-3">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-[4px] ml-2">VIOLATION CLASS</label>
                                      <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="e.g. BEHAVIORAL_MISCONDUCT" className="w-full p-6 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-[28px] outline-none font-black text-sm transition-all shadow-inner tracking-widest" />
                                  </div>
                                  <div className="space-y-3">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-[4px] ml-2">INCIDENT DEBRIEF</label>
                                      <textarea rows={8} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Input full incident details for user notification..." className="w-full p-8 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-[40px] outline-none font-bold text-slate-600 transition-all resize-none shadow-inner leading-relaxed" />
                                  </div>
                              </div>
                          )}

                          {activeModal === 'wallet' && (
                              <div className="space-y-10 animate-in fade-in slide-in-from-bottom-4 duration-500">
                                  <div className="flex gap-6">
                                      <button onClick={()=>setFormState({...formState, type: 'credit'})} className={`flex-1 py-7 rounded-[35px] font-black uppercase text-xs tracking-[4px] border-4 transition-all ${formState.type === 'credit' ? 'bg-emerald-500 border-emerald-100 text-white shadow-2xl shadow-emerald-500/30' : 'bg-slate-50 border-transparent text-slate-300'}`}>INJECT (+)</button>
                                      <button onClick={()=>setFormState({...formState, type: 'debit'})} className={`flex-1 py-7 rounded-[35px] font-black uppercase text-xs tracking-[4px] border-4 transition-all ${formState.type === 'debit' ? 'bg-red-50 border-red-100 text-white shadow-2xl shadow-red-500/30' : 'bg-slate-50 border-transparent text-slate-300'}`}>EXTRACT (-)</button>
                                  </div>
                                  <div className="space-y-3">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-[4px] ml-2">ASSET VALUATION (INR)</label>
                                      <div className="relative">
                                          <span className="absolute left-8 top-1/2 -translate-y-1/2 text-4xl font-black text-slate-200">₹</span>
                                          <input type="number" value={formState.amount} onChange={e=>setFormState({...formState, amount: e.target.value})} placeholder="0.00" className="w-full p-10 pl-16 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-[45px] outline-none font-black text-6xl transition-all shadow-inner tracking-tighter italic" />
                                      </div>
                                  </div>
                              </div>
                          )}

                          {activeModal === 'broadcast' && (
                              <div className="space-y-6">
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">Target Audience</label>
                                      <select value={formState.role} onChange={e=>setFormState({...formState, role: e.target.value})} className="w-full p-5 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-2xl outline-none font-black transition-all appearance-none cursor-pointer shadow-inner">
                                          <option value="all">Every Account on Platform</option>
                                          <option value="customer">Customers Only</option>
                                          <option value="provider">Partners Only</option>
                                      </select>
                                  </div>
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">Push Title</label>
                                      <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="Urgent Announcement" className="w-full p-5 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-2xl outline-none font-bold transition-all shadow-inner" />
                                  </div>
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">Notification Body</label>
                                      <textarea rows={6} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Write the message that will pop up on user phones..." className="w-full p-5 bg-slate-50 border-2 border-transparent focus:border-indigo-500 focus:bg-white rounded-2xl outline-none font-bold transition-all resize-none shadow-inner" />
                                  </div>
                              </div>
                          )}

                          {activeModal === 'delete' && (
                              <div className="text-center space-y-8 py-10">
                                  <div className="w-24 h-24 bg-red-50 rounded-full flex items-center justify-center mx-auto text-red-500 animate-pulse"><Trash2 size={48}/></div>
                                  <div>
                                      <h4 className="text-2xl font-black text-slate-800 uppercase tracking-tighter">Security Protocol Required</h4>
                                      <p className="text-slate-400 font-medium leading-relaxed italic mt-4 px-6">This action will trigger a permanent cascade deletion of all user data. This is irreversible.</p>
                                  </div>
                              </div>
                          )}

                          {activeModal === 'details' && selectedUser && (
                              <div className="space-y-12 animate-in fade-in slide-in-from-bottom-10 duration-700">
                                  <div className="grid grid-cols-2 gap-8">
                                      <div className="p-10 bg-indigo-600 rounded-[45px] text-white shadow-2xl shadow-indigo-200">
                                          <p className="text-[9px] font-black uppercase tracking-[4px] opacity-60 mb-2 text-white/80">Net Worth</p>
                                          <p className="text-4xl font-black italic tracking-tighter">₹{selectedUser.totalEarnings || 0}</p>
                                      </div>
                                      <div className="p-10 bg-slate-900 rounded-[45px] text-white shadow-2xl shadow-slate-200">
                                          <p className="text-[9px] font-black uppercase tracking-[4px] opacity-60 mb-2 text-white/80">Ops Count</p>
                                          <p className="text-4xl font-black italic tracking-tighter">{selectedUser.totalJobs || 0}</p>
                                      </div>
                                  </div>
                                  <div className="space-y-8">
                                      <h4 className="text-[11px] font-black text-slate-900 uppercase tracking-[6px] italic flex items-center gap-3">
                                          <div className="w-2 h-2 rounded-full bg-indigo-600 animate-ping"></div> Activity Stream
                                      </h4>
                                      <div className="space-y-6 relative before:absolute before:left-[11px] before:top-0 before:bottom-0 before:w-1 before:bg-slate-100 before:rounded-full">
                                          {selectedUser.activityLog?.slice(-10).reverse().map((log, i) => (
                                              <div key={i} className="pl-10 relative group">
                                                  <div className="absolute left-0 top-1 w-6 h-6 rounded-full bg-white border-4 border-indigo-500 group-hover:scale-125 transition-transform z-10 shadow-lg shadow-indigo-100"></div>
                                                  <div className="p-6 bg-white border border-slate-100 rounded-[28px] shadow-sm hover:shadow-xl transition-all hover:-translate-y-1">
                                                      <div className="flex justify-between items-center mb-2">
                                                          <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest italic">{log.event}</span>
                                                          <span className="text-[9px] font-bold text-slate-300">{new Date(log.timestamp).toLocaleTimeString()}</span>
                                                      </div>
                                                      <p className="text-sm text-slate-500 font-bold leading-relaxed opacity-80">"{log.description}"</p>
                                                  </div>
                                              </div>
                                          ))}
                                      </div>
                                  </div>
                              </div>
                          )}
                      </div>

                      <div className="p-10 border-t border-slate-50 bg-white">
                          <button onClick={handleAction} className={`w-full py-8 rounded-[35px] font-black uppercase text-sm tracking-[6px] shadow-[0_20px_50px_rgba(0,0,0,0.1)] transition-all hover:-translate-y-2 active:scale-95 ${activeModal === 'delete' ? 'bg-red-600 text-white' : 'bg-slate-900 text-white'}`}>
                              CONFIRM {activeModal.toUpperCase()}
                          </button>
                      </div>
                  </div>
              </div>
          </div>
      )}
    </div>
  );
}

function NavItem({ icon, label, active, onClick, count, pulse }) {
  return (
    <button onClick={onClick} className={`w-full flex items-center justify-between px-5 py-3.5 rounded-2xl transition-all duration-500 ${active ? 'bg-indigo-600 text-white shadow-[0_10px_30px_rgba(79,70,229,0.3)]' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}>
      <div className="flex items-center space-x-3">
        {pulse && !active ? (
            <div className="relative">
                <div className="absolute inset-0 bg-indigo-500 rounded-full animate-ping opacity-25"></div>
                {icon}
            </div>
        ) : icon}
        <span className="font-bold text-sm tracking-tight">{label}</span>
      </div>
      {count > 0 && <span className={`px-2 py-0.5 text-[9px] font-black rounded-lg ${active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'}`}>{count}</span>}
    </button>
  );
}

const MonitorView = ({ data }) => (
    <div className="space-y-12">
        <div className="flex justify-between items-end">
            <div>
                <h3 className="text-3xl font-black text-slate-900 tracking-tighter uppercase italic">Mission Control</h3>
                <p className="text-slate-400 font-black text-[10px] uppercase tracking-[5px] mt-2">Live Fleet Operations & Real-time Status</p>
            </div>
            <div className="flex gap-4">
                <StatusPill color="bg-emerald-500" label="ONLINE" count={data.filter(p=>p.status==='online').length} />
                <StatusPill color="bg-indigo-500" label="BUSY" count={data.filter(p=>p.status==='busy').length} />
                <StatusPill color="bg-slate-300" label="OFFLINE" count={data.filter(p=>p.status==='offline').length} />
            </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
            <div className="lg:col-span-3 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                {data.map(p => (
                    <div key={p.uid} className="bg-white p-8 rounded-[45px] border border-slate-100 shadow-sm relative overflow-hidden group hover:shadow-2xl transition-all duration-700">
                        <div className={`absolute top-0 right-0 px-6 py-2 text-white font-black text-[9px] uppercase tracking-widest rounded-bl-[25px] ${p.status === 'online' ? 'bg-emerald-500' : p.status === 'busy' ? 'bg-indigo-500' : 'bg-slate-300'}`}>
                            {p.status}
                        </div>
                        <div className="flex items-center gap-5 mb-8">
                            <div className="relative">
                                <img src={p.profileImage || 'https://via.placeholder.com/80'} className="w-16 h-16 rounded-[25px] object-cover ring-4 ring-slate-50 shadow-xl" />
                                <div className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-full border-4 border-white ${p.status === 'online' ? 'bg-emerald-500' : p.status === 'busy' ? 'bg-indigo-500' : 'bg-slate-300'}`}></div>
                            </div>
                            <div>
                                <h4 className="font-black text-slate-900 uppercase tracking-tight">{p.name}</h4>
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{p.phoneNumber}</p>
                            </div>
                        </div>

                        {p.status === 'busy' ? (
                            <div className="p-5 bg-indigo-50 rounded-[30px] border border-indigo-100 shadow-inner">
                                <div className="flex items-center gap-2 mb-2">
                                    <Zap size={14} className="text-indigo-600 fill-indigo-600" />
                                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest">Active Task</span>
                                </div>
                                <p className="font-black text-slate-800 text-sm truncate">{p.currentTask?.service}</p>
                                <div className="flex justify-between items-center mt-4">
                                    <span className="text-[9px] font-black text-indigo-400 uppercase tracking-widest">{p.currentTask?.status.replace('_', ' ')}</span>
                                    <button className="text-[9px] font-black text-indigo-600 underline tracking-widest">TRACK OPS</button>
                                </div>
                            </div>
                        ) : (
                            <div className="p-5 bg-slate-50 rounded-[30px] border border-slate-100 flex items-center justify-center italic text-slate-300 text-xs font-bold uppercase tracking-widest">
                                {p.status === 'online' ? 'Awaiting Assignment' : 'System Disconnected'}
                            </div>
                        )}

                        <div className="mt-8 pt-6 border-t border-slate-50 flex items-center justify-between">
                            <div className="flex items-center gap-2 text-slate-400">
                                <MapPin size={12}/>
                                <span className="text-[10px] font-black uppercase tracking-widest">{p.lat ? `${p.lat.toFixed(2)}, ${p.lng.toFixed(2)}` : 'LOC_UNKNOWN'}</span>
                            </div>
                            <button className="w-8 h-8 rounded-full bg-slate-50 flex items-center justify-center text-slate-300 hover:bg-slate-900 hover:text-white transition-all"><Info size={14}/></button>
                        </div>
                    </div>
                ))}
            </div>

            <div className="space-y-8">
                <div className="bg-slate-900 p-10 rounded-[50px] text-white shadow-2xl relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-32 h-32 bg-indigo-500/10 rounded-full blur-3xl"></div>
                    <h3 className="font-black uppercase text-[10px] tracking-[4px] text-indigo-400 mb-8 flex items-center gap-2">
                        <Activity size={14}/> Live Feed
                    </h3>
                    <div className="space-y-8">
                        {[1,2,3,4].map(i => (
                            <div key={i} className="flex gap-4 animate-in fade-in slide-in-from-right-4 duration-1000" style={{animationDelay: `${i*200}ms`}}>
                                <div className="w-1 h-8 bg-indigo-500/30 rounded-full mt-1"></div>
                                <div>
                                    <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">04:2{i} PM</p>
                                    <p className="text-xs font-bold text-white mt-1 leading-relaxed opacity-90 italic">Event <span className="text-indigo-400 underline">#{1000+i}</span> processed via node_alpha.</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    </div>
);

const StatusPill = ({ color, label, count }) => (
    <div className="px-6 py-3 bg-white border border-slate-100 rounded-full shadow-sm flex items-center gap-3">
        <div className={`w-2 h-2 rounded-full ${color} animate-pulse`}></div>
        <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{label}</span>
        <span className="text-sm font-black text-slate-900">{count}</span>
    </div>
);

const AnalyticsView = ({ data }) => !data ? null : (
  <div className="space-y-12">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
        <StatCard title="Total Consumers" value={data.totalUsers} color="text-indigo-600" />
        <StatCard title="Active Partners" value={data.totalProviders} color="text-emerald-600" />
        <StatCard title="Total Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} color="text-slate-900" />
        <StatCard title="Job Orders" value={data.totalBookings} color="text-blue-600" />
      </div>
      <div className="bg-white p-12 rounded-[50px] border border-slate-100 shadow-sm">
          <h3 className="font-black text-slate-900 uppercase tracking-[8px] italic text-xs mb-12">Resource Allocation Matrix</h3>
          <div className="space-y-12">
              {data.categories?.map((cat, i) => (
                  <div key={i}>
                      <div className="flex justify-between items-end mb-4">
                          <div>
                              <p className="text-[10px] font-black text-slate-400 uppercase tracking-[4px]">{cat.name}</p>
                              <p className="text-2xl font-black text-slate-900 tracking-tighter italic">Sector Analysis</p>
                          </div>
                          <p className="text-3xl font-black text-indigo-600 italic tracking-tighter">{Math.round(cat.ratio * 100)}%</p>
                      </div>
                      <div className="w-full bg-slate-50 rounded-full h-8 overflow-hidden p-2 shadow-inner border border-slate-100">
                          <div className="bg-gradient-to-r from-indigo-500 to-indigo-400 h-full rounded-full transition-all duration-1000 shadow-xl" style={{ width: `${cat.ratio * 100}%` }}></div>
                      </div>
                  </div>
              ))}
          </div>
      </div>
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-10 rounded-[40px] border border-slate-100 shadow-sm group hover:shadow-2xl transition-all duration-700 relative overflow-hidden">
    <div className="absolute bottom-0 right-0 w-24 h-24 bg-slate-50 rounded-tl-[40px] -mr-4 -mb-4 opacity-50"></div>
    <p className="text-[10px] font-black text-slate-400 uppercase tracking-[4px] mb-3">{title}</p>
    <p className={`text-5xl font-black tracking-tighter ${color} relative z-10`}>{value || 0}</p>
  </div>
);

const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, onDetails, title, isPartner }) => (
  <div className="bg-white rounded-[40px] border border-slate-100 shadow-sm overflow-hidden">
    <div className="px-12 py-8 border-b border-slate-50 bg-white flex justify-between items-center">
        <h3 className="font-black text-slate-800 text-xs uppercase tracking-[6px] italic">{title} Ecosystem</h3>
        <div className="px-6 py-2.5 bg-slate-900 rounded-full text-[9px] font-black text-white uppercase tracking-[2px] shadow-xl shadow-slate-200">{users?.length || 0} RECORDS</div>
    </div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead>
          <tr className="bg-slate-50/50 text-[9px] font-black text-slate-400 uppercase tracking-[3px]">
            <th className="px-12 py-6">Identity</th>
            <th className="px-12 py-6">Wallet</th>
            <th className="px-12 py-6 text-center">Status</th>
            <th className="px-12 py-6 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-50">
          {!users || users.length === 0 ? (
            <tr><td colSpan="4" className="px-10 py-32 text-center text-slate-200 italic font-black uppercase tracking-[5px]">Empty Registry</td></tr>
          ) : (
            users.map(u => (
              <tr key={u.uid} className="hover:bg-slate-50/50 transition-all group">
                <td className="px-12 py-8">
                  <div className="flex items-center space-x-5 cursor-pointer" onClick={()=>onDetails(u)}>
                    <img src={u.profileImage || 'https://via.placeholder.com/64'} className="w-14 h-14 rounded-2xl object-cover border-4 border-white shadow-xl group-hover:scale-110 transition-all duration-500" />
                    <div><p className="font-black text-slate-800 group-hover:text-indigo-600 transition-colors text-lg tracking-tight">{u.name || 'ANONYMOUS'}</p><p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-0.5">{u.phoneNumber}</p></div>
                  </div>
                </td>
                <td className="px-12 py-8">
                    <div className="p-4 bg-indigo-50/50 rounded-[20px] border border-indigo-100/50 w-fit shadow-sm">
                        <p className="text-lg font-black text-indigo-600 italic tracking-tight">₹{u.walletBalance?.toFixed(0) || 0}</p>
                    </div>
                </td>
                <td className="px-12 py-8 text-center">
                  <span className={`px-5 py-2 rounded-full text-[9px] font-black uppercase border-2 transition-all shadow-md ${u.isBlocked ? 'bg-red-50 border-red-100 text-red-500 shadow-red-100' : 'bg-emerald-50 border-emerald-100 text-emerald-500 shadow-emerald-100'}`}>{u.isBlocked ? 'SUSPENDED' : 'AUTHORIZED'}</span>
                </td>
                <td className="px-12 py-8 text-right space-x-3">
                  <button onClick={()=>onWarn(u)} className="p-4 bg-amber-50 text-amber-500 rounded-2xl hover:bg-amber-500 hover:text-white transition-all shadow-sm hover:shadow-xl hover:shadow-amber-200" title="Issue Warning"><AlertTriangle size={18}/></button>
                  <button onClick={()=>onWallet(u)} className="p-4 bg-indigo-50 text-indigo-500 rounded-2xl hover:bg-indigo-600 hover:text-white transition-all shadow-sm hover:shadow-xl hover:shadow-indigo-200" title="Wallet Control"><Wallet size={18}/></button>
                  <button onClick={()=>onBlock(u)} className={`p-4 rounded-2xl transition-all shadow-sm ${u.isBlocked ? 'bg-emerald-50 text-emerald-500 hover:bg-emerald-500 hover:text-white hover:shadow-xl hover:shadow-emerald-200' : 'bg-slate-100 text-slate-500 hover:bg-slate-900 hover:text-white hover:shadow-xl hover:shadow-slate-300'}`} title={u.isBlocked ? 'Restore' : 'Block'}><ShieldAlert size={18}/></button>
                  <button onClick={()=>onDelete(u)} className="p-4 bg-red-50 text-red-400 rounded-2xl hover:bg-red-600 hover:text-white transition-all shadow-sm hover:shadow-xl hover:shadow-red-200" title="Delete Account"><Trash2 size={18}/></button>
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
  <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
    {providers.length === 0 ? (
       <div className="col-span-full p-40 bg-white rounded-[60px] border-4 border-dashed border-slate-50 text-center text-slate-200 font-black uppercase tracking-[15px]">Zero Inbound Requests</div>
    ) : (
      providers.map(p => (
        <div key={p.uid} className="bg-white p-12 rounded-[50px] shadow-sm border border-slate-100 relative group overflow-hidden hover:shadow-2xl transition-all duration-700">
          <div className="absolute top-0 right-0 p-10 flex flex-col gap-3">
              <button onClick={()=>onApprove(p.uid)} className="p-5 bg-indigo-600 text-white rounded-[24px] shadow-2xl shadow-indigo-200 hover:scale-110 active:scale-95 transition-all font-black uppercase text-[10px] tracking-[2px]">Validate Credentials</button>
          </div>
          <div className="flex flex-col items-center text-center space-y-8">
            <img src={p.profileImage || 'https://via.placeholder.com/120'} className="w-32 h-32 rounded-[40px] object-cover ring-[12px] ring-slate-50 shadow-2xl transition-all duration-700" />
            <div>
                <h4 className="font-black text-3xl tracking-tighter text-slate-800 uppercase">{p.name}</h4>
                <p className="text-[11px] font-black text-indigo-500 uppercase tracking-[4px] mt-3 italic">{p.profession || 'SPECIALIST'}</p>
            </div>
            <div className="w-full grid grid-cols-2 gap-4">
                <div className="p-5 bg-slate-50 rounded-[24px] border border-slate-100 shadow-inner">
                    <p className="text-[9px] font-black text-slate-300 uppercase tracking-widest mb-1">GOVT ID (AADHAAR)</p>
                    <p className="text-sm font-black text-slate-700 tracking-widest">{p.aadhaarNumber}</p>
                </div>
                <div className="p-5 bg-slate-50 rounded-[24px] border border-slate-100 flex items-center justify-center cursor-pointer hover:bg-indigo-600 hover:text-white transition-all duration-500 group/eye shadow-inner">
                    <Eye size={24} className="text-indigo-400 group-hover/eye:text-white transition-colors"/>
                </div>
            </div>
          </div>
        </div>
      ))
    )}
  </div>
);

const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-[40px] border border-slate-100 shadow-sm overflow-hidden">
    <div className="p-12 border-b border-slate-50 flex justify-between items-center bg-white font-black text-slate-800 text-xs uppercase tracking-[8px]">Partner Asset Liquidation</div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead className="bg-slate-50/50 text-[9px] font-black text-slate-400 uppercase tracking-[4px]">
          <tr><th className="px-12 py-8">Account Holder</th><th className="px-12 py-8">Asset Valuation</th><th className="px-12 py-8 text-right">Confirmation</th></tr>
        </thead>
        <tbody className="divide-y divide-slate-50">
          {withdrawals.length === 0 ? (
            <tr><td colSpan="3" className="px-10 py-32 text-center text-slate-200 italic font-black uppercase tracking-[5px]">Zero Pending Payouts</td></tr>
          ) : (
            withdrawals.map(w => (
              <tr key={w._id} className="hover:bg-slate-50/50 transition-all">
                <td className="px-12 py-10 font-black text-slate-800 uppercase tracking-tighter text-xl">{w.providerName}</td>
                <td className="px-12 py-10 text-indigo-600 font-black text-4xl italic tracking-tighter">₹{w.amount}</td>
                <td className="px-12 py-10 text-right space-x-4">
                    <button onClick={()=>onHandle(w._id, 'approved')} className="px-10 py-4 bg-slate-900 text-white text-[10px] font-black rounded-[20px] uppercase tracking-[3px] shadow-2xl hover:bg-indigo-600 transition-all duration-500">Initialize Transfer</button>
                    <button onClick={()=>onHandle(w._id, 'rejected')} className="px-10 py-4 bg-red-50 text-red-400 text-[10px] font-black rounded-[20px] uppercase tracking-[3px] hover:bg-red-500 hover:text-white transition-all">Deny File</button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

const JobsTable = ({ jobs, title }) => (
  <div className="bg-white rounded-[40px] border border-slate-100 shadow-sm overflow-hidden">
    <div className="px-12 py-8 border-b font-black uppercase text-[10px] tracking-[6px] bg-white text-slate-800 flex justify-between items-center">
        <span>{title}</span>
        <div className="px-5 py-2 bg-indigo-50 text-indigo-600 rounded-full font-black italic tracking-widest">{jobs?.length || 0} TOTAL OPS</div>
    </div>
    <table className="w-full text-left border-collapse">
      <thead className="bg-slate-50/50 text-[9px] font-black text-slate-400 uppercase tracking-[4px] border-b border-slate-50">
          <tr><th className="px-12 py-7">Mission Configuration</th><th className="px-12 py-7">Field Deployment</th><th className="px-12 py-7 text-right">Resource Credit</th></tr>
      </thead>
      <tbody className="divide-y divide-slate-50">
        {(!jobs || jobs.length === 0) ? (
          <tr><td colSpan="3" className="px-10 py-40 text-center text-slate-200 italic font-black uppercase tracking-[10px]">Registry Empty</td></tr>
        ) : (
          jobs.map(j => (
            <tr key={j._id} className="hover:bg-slate-50/50 transition-all duration-500">
              <td className="px-12 py-10">
                  <p className="font-black text-slate-800 uppercase tracking-tighter text-2xl">{j.serviceName}</p>
                  <p className="text-[10px] font-black text-slate-300 uppercase tracking-[3px] mt-2 italic">{new Date(j.createdAt).toLocaleString()}</p>
              </td>
              <td className="px-12 py-10">
                  <span className={`px-6 py-2.5 rounded-full text-[10px] font-black uppercase border-2 transition-all shadow-lg ${j.status === 'done' ? 'bg-emerald-50 border-emerald-100 text-emerald-500 shadow-emerald-50' : 'bg-indigo-50 border-indigo-100 text-indigo-500 shadow-indigo-50'}`}>{j.status.replace('_',' ')}</span>
              </td>
              <td className="px-12 py-10 text-right font-black text-3xl italic tracking-tighter text-slate-900">₹{j.totalAmount}</td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  </div>
);

const CategoriesView = ({ categories, refresh }) => {
  const [n, setN] = useState(''); const [i, setI] = useState('');
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
      <div className="bg-[#0F172A] p-12 rounded-[50px] shadow-2xl h-fit border border-white/5 relative overflow-hidden">
        <h3 className="font-black mb-10 uppercase text-[10px] tracking-[5px] text-indigo-400 italic">Initialize Module</h3>
        <div className="space-y-8">
            <input value={n} onChange={e=>setN(e.target.value)} placeholder="VECTOR NAME" className="w-full p-5 bg-white/5 border-2 border-white/10 rounded-[24px] font-black text-white outline-none focus:border-indigo-500 uppercase tracking-widest text-xs" />
            <input value={i} onChange={e=>setI(e.target.value)} placeholder="ASSET URI" className="w-full p-5 bg-white/5 border-2 border-white/10 rounded-[24px] font-bold text-white outline-none focus:border-indigo-500 text-xs" />
            <button onClick={()=>adminApi.addCategory({name:n, icon:i}).then(refresh)} className="w-full py-6 bg-indigo-600 text-white font-black rounded-[28px] uppercase text-xs tracking-[5px] shadow-2xl shadow-indigo-900/50 hover:bg-white hover:text-indigo-600 transition-all">Initialize Deploy</button>
        </div>
      </div>
      <div className="lg:col-span-2 grid grid-cols-2 md:grid-cols-3 gap-8">
        {categories.map(c => (
            <div key={c._id} className="bg-white p-10 rounded-[45px] border border-slate-100 shadow-sm flex flex-col items-center hover:shadow-2xl hover:scale-[1.03] transition-all duration-700 group relative overflow-hidden">
                <div className="w-24 h-24 bg-slate-50 rounded-[35px] flex items-center justify-center mb-8 shadow-inner group-hover:rotate-12 transition-transform duration-500">
                    <img src={c.icon || 'https://via.placeholder.com/80'} className="w-16 h-16 object-contain drop-shadow-2xl" />
                </div>
                <p className="font-black text-slate-800 uppercase tracking-[3px] text-xs text-center leading-relaxed">{c.name}</p>
            </div>
        ))}
      </div>
    </div>
  );
};

const ReportsView = ({ reports }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
    {reports.length === 0 ? <div className="col-span-full p-60 bg-white rounded-[60px] border-4 border-dashed border-slate-50 text-center text-slate-100 font-black uppercase tracking-[30px]">STATUS NOMINAL</div> : reports.map(r => (
      <div key={r._id} className="bg-white p-12 rounded-[50px] border-l-[16px] border-red-500 shadow-2xl relative overflow-hidden group">
        <div className="absolute top-0 right-0 px-8 py-3 bg-red-600 text-white text-[10px] font-black uppercase rounded-bl-[30px] tracking-[4px] shadow-2xl">{r.status}</div>
        <h4 className="font-black text-slate-800 uppercase text-2xl tracking-tighter mb-6">{r.reason}</h4>
        <div className="bg-slate-50 p-6 rounded-[30px] shadow-inner mb-10 font-bold text-slate-500 italic opacity-90 text-sm leading-relaxed">"{r.description}"</div>
        <div className="flex gap-4 overflow-x-auto pb-6 custom-scrollbar scroll-smooth">
            {r.evidenceUrls?.map((u, i) => (
                <img key={i} src={u} className="w-24 h-24 rounded-[32px] border-8 border-white object-cover shadow-2xl hover:scale-110 hover:rotate-6 transition-all flex-shrink-0" />
            ))}
        </div>
      </div>
    ))}
  </div>
);

const ReviewsView = ({ reviews, onDelete }) => (
  <div className="bg-white rounded-[50px] border border-slate-100 shadow-sm overflow-hidden text-left">
    <div className="px-12 py-10 border-b border-slate-50 bg-white font-black text-slate-800 text-xs uppercase tracking-[10px]">Sentiment Intelligence</div>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead><tr className="bg-slate-50/50 text-[9px] font-black text-slate-400 uppercase tracking-[5px]">
            <th className="px-12 py-8">Reviewer</th><th className="px-12 py-8 text-left">Analysis</th><th className="px-12 py-8 text-right">Delete</th>
        </tr></thead>
        <tbody className="divide-y divide-slate-50">
          {reviews.map(r => (
            <tr key={r._id} className="hover:bg-slate-50/50 transition-all duration-500">
              <td className="px-12 py-10 font-black text-slate-800 uppercase tracking-tighter text-xl">{r.customerName}
                  <div className="flex gap-1 mt-3">{[...Array(5)].map((_, i) => <Star key={i} size={14} className={i < r.rating ? 'text-amber-400 fill-amber-400' : 'text-slate-200'} />)}</div>
              </td>
              <td className="px-12 py-10"><div className="p-6 bg-slate-50 rounded-[30px] border border-slate-100 shadow-inner italic font-bold text-slate-500">"{r.comment}"</div></td>
              <td className="px-12 py-10 text-right"><button onClick={()=>onDelete(r._id)} className="p-6 bg-red-50 text-red-400 rounded-[30px] hover:bg-red-500 hover:text-white transition-all shadow-xl active:scale-90"><Trash2 size={24}/></button></td>
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
    <div className="bg-[#0F172A] p-16 rounded-[60px] border-8 border-white/5 max-w-3xl shadow-[0_60px_150px_rgba(0,0,0,0.5)] relative overflow-hidden group">
      <h3 className="font-black uppercase text-[10px] tracking-[10px] mb-16 text-indigo-400 italic italic">Central Logic Control</h3>
      <div className="flex justify-between items-center mb-16 relative z-10 text-white uppercase font-black text-3xl tracking-tighter">
          <div><p>Revenue Cut %</p><p className="text-slate-500 text-sm mt-3 font-medium italic normal-case tracking-normal">Platform fee per successful job completion.</p></div>
          <div className="flex items-center space-x-6 bg-white/5 p-8 rounded-[40px] border-2 border-white/10">
              <input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-32 h-32 text-center bg-transparent font-black text-6xl text-indigo-400 outline-none" />
              <span className="font-black text-white/5 text-8xl italic select-none">%</span>
          </div>
      </div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(()=>alert('Engine Synced'))} className="w-full py-8 bg-indigo-600 text-white font-black rounded-[35px] uppercase text-sm tracking-[8px] shadow-[0_20px_50px_rgba(79,70,229,0.5)] hover:bg-white hover:text-indigo-600 transition-all duration-500">UPDATE GLOBAL ENGINE</button>
    </div>
  );
};
