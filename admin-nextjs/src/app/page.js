'use client';
import React, { useState, useEffect } from 'react';
import {
  Users, Briefcase, CheckCircle, XCircle, BarChart3,
  Settings, LogOut, MessageSquare, Image as ImageIcon,
  Tag, CreditCard, Bell, ChevronRight, Search, ShieldAlert,
  UserCheck, UserMinus, Clock, Filter, FileText, AlertTriangle,
  Trash2, Plus, Star, Wallet, Send, X, MoreVertical, Eye
} from 'lucide-react';
import { adminApi } from '@/lib/api';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('analytics');
  const [data, setData] = useState({
    analytics: null, pendingProviders: [], withdrawals: [],
    activeJobs: [], categories: [], offers: [], banners: [],
    settings: {}, chats: [], allUsers: [], allProviders: [],
    allBookings: [], reports: [], reviews: []
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // UI State
  const [activeModal, setActiveModal] = useState(null); // 'warning', 'wallet', 'broadcast', 'delete', 'details'
  const [selectedUser, setSelectedUser] = useState(null);
  const [formState, setFormState] = useState({ title: '', message: '', amount: '', type: 'credit', description: '', role: 'all' });

  useEffect(() => { fetchTabData(activeTab); }, [activeTab]);

  const fetchTabData = async (tab) => {
    setLoading(true); setError(null);
    try {
      let res;
      switch(tab) {
        case 'analytics': res = await adminApi.getAnalytics(); setData(p => ({...p, analytics: res.data})); break;
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
        case 'support': res = await adminApi.getSupportChats(); setData(p => ({...p, chats: res.data})); break;
        case 'reports': res = await adminApi.getReports(); setData(p => ({...p, reports: res.data})); break;
        case 'reviews': res = await adminApi.getAllReviews(); setData(p => ({...p, reviews: res.data})); break;
      }
    } catch (err) { setError("Backend Connection Error"); }
    setLoading(false);
  };

  const openAction = (type, user = null) => {
    setSelectedUser(user);
    setActiveModal(type);
    setFormState({ title: '', message: '', amount: '', type: 'credit', description: '', role: 'all' });
  };

  const handleAction = async () => {
    try {
      if (activeModal === 'warning') {
        await adminApi.sendWarning({ uid: selectedUser.uid, title: formState.title, message: formState.message, type: 'warning' });
        alert("Warning sent!");
      } else if (activeModal === 'wallet') {
        await adminApi.adjustWallet({ uid: selectedUser.uid, amount: formState.amount, type: formState.type, description: formState.description });
        alert("Wallet updated!");
        fetchTabData(activeTab);
      } else if (activeModal === 'broadcast') {
        await adminApi.broadcast({ role: formState.role, title: formState.title, message: formState.message });
        alert("Broadcast sent successfully!");
      } else if (activeModal === 'delete') {
        await adminApi.deleteUser(selectedUser.uid);
        alert("User deleted permanently.");
        fetchTabData(activeTab);
      }
      setActiveModal(null);
    } catch (e) { alert("Action failed"); }
  };

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900 overflow-hidden font-sans">
      {/* Sidebar */}
      <aside className="w-64 bg-[#0F172A] text-white flex flex-col shadow-2xl shrink-0 z-20">
        <div className="p-8 border-b border-white/5 font-black text-xl tracking-tighter flex items-center gap-2">
            <div className="w-8 h-8 bg-indigo-500 rounded-lg flex items-center justify-center">M</div>
            Madadwala
        </div>
        <nav className="flex-1 px-4 py-8 space-y-1 overflow-y-auto custom-scrollbar">
          <NavItem icon={<BarChart3 size={18}/>} label="Dashboard" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <div className="pt-6 pb-2 px-2 text-[10px] font-black text-slate-500 uppercase tracking-[2px]">Core Management</div>
          <NavItem icon={<Users size={18}/>} label="Customer Directory" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={18}/>} label="Verified Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={18}/>} label="Approval Queue" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />

          <div className="pt-6 pb-2 px-2 text-[10px] font-black text-slate-500 uppercase tracking-[2px]">Operations</div>
          <NavItem icon={<CreditCard size={18}/>} label="Payout Requests" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Clock size={18}/>} label="Live Jobs Map" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={18}/>} label="Transaction Logs" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />
          <NavItem icon={<Star size={18}/>} label="User Reviews" active={activeTab==='reviews'} onClick={()=>setActiveTab('reviews')} />
          <NavItem icon={<FileText size={18}/>} label="Reports & Issues" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />

          <div className="pt-6 pb-2 px-2 text-[10px] font-black text-slate-500 uppercase tracking-[2px]">Control Panel</div>
          <NavItem icon={<Tag size={18}/>} label="Service Categories" active={activeTab==='categories'} onClick={()=>setActiveTab('categories')} />
          <NavItem icon={<ImageIcon size={18}/>} label="Promotion Banners" active={activeTab==='banners'} onClick={()=>setActiveTab('banners')} />
          <NavItem icon={<Settings size={18}/>} label="System Settings" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
        <div className="p-4 bg-white/5 m-4 rounded-2xl">
            <button onClick={()=>openAction('broadcast')} className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs uppercase tracking-widest rounded-xl transition-all flex items-center justify-center gap-2">
                <Send size={14}/> Broadcast
            </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-20 bg-white border-b border-gray-200 flex items-center justify-between px-10 shrink-0 z-10">
          <h2 className="text-xl font-black text-slate-800 tracking-tight">{activeTab.replace('-', ' ').toUpperCase()}</h2>
          <div className="flex items-center gap-4">
              {error && <div className="text-red-500 text-xs font-black bg-red-50 px-4 py-2 rounded-full border border-red-100 flex items-center gap-2"><XCircle size={14}/> {error}</div>}
              <div className="w-12 h-12 rounded-2xl bg-slate-900 border-4 border-slate-100 flex items-center justify-center text-white font-black shadow-xl shadow-slate-200">A</div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-10 custom-scrollbar bg-[#F8FAFC]">
          {loading ? (
             <div className="flex flex-col items-center justify-center h-full gap-4">
               <div className="w-12 h-12 border-4 border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div>
               <p className="text-slate-400 font-bold uppercase text-[10px] tracking-[3px]">Syncing Data...</p>
             </div>
          ) : (
            <div className="max-w-[1400px] mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500">
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>fetchTabData('customers'))} onDetails={(u)=>openAction('details', u)} title="Customers" />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>openAction('warning', u)} onWallet={(u)=>openAction('wallet', u)} onDelete={(u)=>openAction('delete', u)} onBlock={(u)=>adminApi.toggleBlock(u.uid).then(()=>fetchTabData('providers-all'))} onDetails={(u)=>openAction('details', u)} title="Partners" isPartner />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>fetchTabData('providers-pending'))} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>fetchTabData('withdrawals'))} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Real-time Tracking" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="Complete Booking History" />}
              {activeTab === 'categories' && <CategoriesView categories={data.categories} refresh={()=>fetchTabData('categories')} />}
              {activeTab === 'reports' && <ReportsView reports={data.reports} />}
              {activeTab === 'reviews' && <ReviewsView reviews={data.reviews} onDelete={(id)=>adminApi.deleteReview(id).then(()=>fetchTabData('reviews'))} />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>fetchTabData('settings')} />}
            </div>
          )}
        </main>
      </div>

      {/* Slide-over Panel (Proper UI for Warning/Wallet/Details) */}
      {activeModal && (
          <div className="fixed inset-0 z-50 overflow-hidden">
              <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity" onClick={()=>setActiveModal(null)}></div>
              <div className="absolute inset-y-0 right-0 max-w-lg w-full bg-white shadow-2xl transform transition-transform duration-500 animate-in slide-in-from-right">
                  <div className="h-full flex flex-col">
                      <div className="px-8 py-6 border-b border-slate-100 flex items-center justify-between">
                          <h3 className="text-xl font-black text-slate-800 uppercase tracking-tight">
                              {activeModal === 'warning' && "Issue Account Warning"}
                              {activeModal === 'wallet' && "Adjust Wallet Balance"}
                              {activeModal === 'broadcast' && "Platform Broadcast"}
                              {activeModal === 'delete' && "Permanent Deletion"}
                              {activeModal === 'details' && "User Profile Detail"}
                          </h3>
                          <button onClick={()=>setActiveModal(null)} className="p-2 hover:bg-slate-50 rounded-full text-slate-400"><X size={20}/></button>
                      </div>

                      <div className="flex-1 overflow-y-auto p-8 space-y-8">
                          {selectedUser && activeModal !== 'broadcast' && (
                              <div className="flex items-center gap-4 p-4 bg-slate-50 rounded-2xl border border-slate-100">
                                  <img src={selectedUser.profileImage} className="w-12 h-12 rounded-xl object-cover ring-4 ring-white" />
                                  <div>
                                      <p className="font-black text-slate-800">{selectedUser.name}</p>
                                      <p className="text-xs text-slate-400 font-bold uppercase tracking-widest">{selectedUser.phoneNumber}</p>
                                  </div>
                              </div>
                          )}

                          {activeModal === 'warning' && (
                              <div className="space-y-6">
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Warning Title</label>
                                      <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="e.g. Terms Violation" className="w-full p-4 bg-slate-50 border-2 border-transparent focus:border-indigo-500 rounded-2xl outline-none font-bold transition-all" />
                                  </div>
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Warning Message</label>
                                      <textarea rows={5} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Write detailed warning here..." className="w-full p-4 bg-slate-50 border-2 border-transparent focus:border-indigo-500 rounded-2xl outline-none font-bold transition-all resize-none" />
                                  </div>
                              </div>
                          )}

                          {activeModal === 'wallet' && (
                              <div className="space-y-6">
                                  <div className="flex gap-4">
                                      <button onClick={()=>setFormState({...formState, type: 'credit'})} className={`flex-1 py-4 rounded-2xl font-black uppercase text-xs tracking-widest border-2 transition-all ${formState.type === 'credit' ? 'bg-emerald-500 border-emerald-500 text-white shadow-lg shadow-emerald-100' : 'bg-slate-50 border-transparent text-slate-400'}`}>Credit (+)</button>
                                      <button onClick={()=>setFormState({...formState, type: 'debit'})} className={`flex-1 py-4 rounded-2xl font-black uppercase text-xs tracking-widest border-2 transition-all ${formState.type === 'debit' ? 'bg-red-500 border-red-500 text-white shadow-lg shadow-red-100' : 'bg-slate-50 border-transparent text-slate-400'}`}>Debit (-)</button>
                                  </div>
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Amount (₹)</label>
                                      <input type="number" value={formState.amount} onChange={e=>setFormState({...formState, amount: e.target.value})} placeholder="0.00" className="w-full p-4 bg-slate-50 border-2 border-transparent focus:border-indigo-500 rounded-2xl outline-none font-black text-2xl transition-all" />
                                  </div>
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Adjustment Reason</label>
                                      <input value={formState.description} onChange={e=>setFormState({...formState, description: e.target.value})} placeholder="e.g. Booking refund or correction" className="w-full p-4 bg-slate-50 border-2 border-transparent focus:border-indigo-500 rounded-2xl outline-none font-bold transition-all" />
                                  </div>
                              </div>
                          )}

                          {activeModal === 'broadcast' && (
                              <div className="space-y-6">
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Target Audience</label>
                                      <select value={formState.role} onChange={e=>setFormState({...formState, role: e.target.value})} className="w-full p-4 bg-slate-50 border-2 border-transparent focus:border-indigo-500 rounded-2xl outline-none font-bold">
                                          <option value="all">Everyone (Customers + Partners)</option>
                                          <option value="customer">Customers Only</option>
                                          <option value="provider">Partners Only</option>
                                      </select>
                                  </div>
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Notification Title</label>
                                      <input value={formState.title} onChange={e=>setFormState({...formState, title: e.target.value})} placeholder="Headline message" className="w-full p-4 bg-slate-50 border-2 border-transparent focus:border-indigo-500 rounded-2xl outline-none font-bold transition-all" />
                                  </div>
                                  <div className="space-y-2">
                                      <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Message Content</label>
                                      <textarea rows={6} value={formState.message} onChange={e=>setFormState({...formState, message: e.target.value})} placeholder="Tell your users something important..." className="w-full p-4 bg-slate-50 border-2 border-transparent focus:border-indigo-500 rounded-2xl outline-none font-bold transition-all resize-none" />
                                  </div>
                              </div>
                          )}

                          {activeModal === 'delete' && (
                              <div className="text-center space-y-6">
                                  <div className="w-20 h-20 bg-red-50 rounded-full flex items-center justify-center mx-auto text-red-500"><Trash2 size={40}/></div>
                                  <h4 className="text-2xl font-black text-slate-800 uppercase tracking-tighter">Are you absolutely sure?</h4>
                                  <p className="text-slate-400 font-medium leading-relaxed italic">This will permanently delete the user, their wallet, bookings, and all history. This action cannot be reversed.</p>
                              </div>
                          )}

                          {activeModal === 'details' && selectedUser && (
                              <div className="space-y-8">
                                  <div className="grid grid-cols-2 gap-4">
                                      <div className="p-4 bg-indigo-50 rounded-2xl">
                                          <p className="text-[10px] font-black text-indigo-400 uppercase tracking-widest">Total Earnings</p>
                                          <p className="text-2xl font-black text-indigo-600">₹{selectedUser.totalEarnings || 0}</p>
                                      </div>
                                      <div className="p-4 bg-emerald-50 rounded-2xl">
                                          <p className="text-[10px] font-black text-emerald-400 uppercase tracking-widest">Total Jobs</p>
                                          <p className="text-2xl font-black text-emerald-600">{selectedUser.totalJobs || 0}</p>
                                      </div>
                                  </div>
                                  <div className="space-y-4">
                                      <h4 className="text-[10px] font-black text-slate-400 uppercase tracking-[3px]">Recent Activity Log</h4>
                                      <div className="space-y-3">
                                          {selectedUser.activityLog?.slice(-5).reverse().map((log, i) => (
                                              <div key={i} className="p-4 bg-white border border-slate-100 rounded-xl shadow-sm">
                                                  <div className="flex justify-between items-center mb-1">
                                                      <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest">{log.event}</span>
                                                      <span className="text-[9px] font-bold text-slate-300">{new Date(log.timestamp).toLocaleDateString()}</span>
                                                  </div>
                                                  <p className="text-xs text-slate-500 font-medium italic">"{log.description}"</p>
                                              </div>
                                          ))}
                                          {(!selectedUser.activityLog || selectedUser.activityLog.length === 0) && <p className="text-center py-10 text-slate-300 italic text-sm">No activity logs recorded.</p>}
                                      </div>
                                  </div>
                              </div>
                          )}
                      </div>

                      <div className="p-8 border-t border-slate-100 bg-slate-50/50">
                          {activeModal !== 'details' ? (
                              <button onClick={handleAction} className={`w-full py-5 rounded-2xl font-black uppercase text-sm tracking-[3px] shadow-2xl transition-all ${activeModal === 'delete' ? 'bg-red-600 text-white shadow-red-200' : 'bg-slate-900 text-white shadow-slate-200 hover:bg-slate-800'}`}>
                                  Confirm {activeModal}
                              </button>
                          ) : (
                              <button onClick={()=>setActiveModal(null)} className="w-full py-5 bg-white border-2 border-slate-100 rounded-2xl font-black uppercase text-sm tracking-[3px] text-slate-400">Close Panel</button>
                          )}
                      </div>
                  </div>
              </div>
          </div>
      )}
    </div>
  );
}

function NavItem({ icon, label, active, onClick, count }) {
  return (
    <button onClick={onClick} className={`w-full flex items-center justify-between px-5 py-3 rounded-2xl transition-all duration-300 ${active ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-900/40' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}>
      <div className="flex items-center space-x-3">{icon}<span className="font-bold text-sm tracking-tight">{label}</span></div>
      {count > 0 && <span className={`px-2 py-0.5 text-[9px] font-black rounded-lg ${active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'}`}>{count}</span>}
    </button>
  );
}

const AnalyticsView = ({ data }) => !data ? <div className="text-slate-300 font-black p-20 bg-white rounded-[32px] border-4 border-dashed border-slate-50 text-center uppercase tracking-[4px]">Initializing Systems...</div> : (
  <div className="space-y-10">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
        <StatCard title="Total Consumers" value={data.totalUsers} color="text-indigo-600" />
        <StatCard title="Active Partners" value={data.totalProviders} color="text-emerald-600" />
        <StatCard title="Total Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} color="text-slate-900" />
        <StatCard title="Job Orders" value={data.totalBookings} color="text-blue-600" />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 bg-white p-10 rounded-[32px] border border-slate-100 shadow-sm">
              <h3 className="font-black text-slate-800 text-lg uppercase tracking-widest mb-10">Category Share</h3>
              <div className="space-y-8">
                  {data.categories?.map((cat, i) => (
                      <div key={i} className="group">
                          <div className="flex justify-between items-center mb-3">
                              <span className="text-xs font-black text-slate-500 uppercase tracking-widest">{cat.name}</span>
                              <span className="text-sm font-black text-indigo-600 italic">{Math.round(cat.ratio * 100)}%</span>
                          </div>
                          <div className="w-full bg-slate-50 rounded-full h-3 overflow-hidden p-0.5 border border-slate-100">
                              <div className="bg-indigo-500 h-full rounded-full transition-all duration-1000 shadow-lg shadow-indigo-100" style={{ width: `${cat.ratio * 100}%` }}></div>
                          </div>
                      </div>
                  ))}
              </div>
          </div>
          <div className="bg-slate-900 p-10 rounded-[32px] text-white flex flex-col justify-center items-center text-center shadow-2xl">
              <div className="w-24 h-24 bg-white/10 rounded-full flex items-center justify-center mb-8 border border-white/5"><CheckCircle size={48} className="text-indigo-400"/></div>
              <h4 className="text-2xl font-black uppercase tracking-tighter">System Pulse</h4>
              <p className="text-slate-400 mt-4 font-medium leading-relaxed italic opacity-80">All server modules are operational. Latency 24ms. Database sync complete.</p>
          </div>
      </div>
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-8 rounded-[32px] border border-slate-100 shadow-sm group hover:border-indigo-100 transition-all duration-500">
    <p className="text-[10px] font-black text-slate-400 uppercase tracking-[3px] mb-2">{title}</p>
    <p className={`text-4xl font-black tracking-tighter ${color}`}>{value || 0}</p>
  </div>
);

const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, onDetails, title, isPartner }) => (
  <div className="bg-white rounded-[32px] border border-slate-100 shadow-sm overflow-hidden">
    <div className="px-10 py-6 border-b border-slate-50 bg-white flex justify-between items-center">
        <h3 className="font-black text-slate-800 text-sm uppercase tracking-[4px] italic">{title} Registry</h3>
        <div className="px-4 py-2 bg-slate-50 rounded-full text-[10px] font-black text-slate-400 uppercase tracking-widest">{users?.length || 0} Records</div>
    </div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead>
          <tr className="bg-slate-50/50 text-[10px] font-black text-slate-400 uppercase tracking-widest">
            <th className="px-10 py-5">Profile Identity</th>
            <th className="px-10 py-5">Wallet Asset</th>
            <th className="px-10 py-5 text-center">Security Status</th>
            <th className="px-10 py-5 text-right">System Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-50">
          {!users || users.length === 0 ? (
            <tr><td colSpan="4" className="px-10 py-20 text-center text-slate-300 italic font-bold uppercase tracking-[2px]">Empty Repository</td></tr>
          ) : (
            users.map(u => (
              <tr key={u.uid} className="hover:bg-slate-50/50 transition-all group">
                <td className="px-10 py-6">
                  <div className="flex items-center space-x-4 cursor-pointer" onClick={()=>onDetails(u)}>
                    <img src={u.profileImage || 'https://via.placeholder.com/48'} className="w-12 h-12 rounded-2xl object-cover border-4 border-white shadow-md group-hover:scale-110 transition-all" />
                    <div><p className="font-black text-slate-800 group-hover:text-indigo-600 transition-colors">{u.name || 'ANONYMOUS'}</p><p className="text-[10px] text-slate-400 font-bold uppercase tracking-tighter">{u.phoneNumber}</p></div>
                  </div>
                </td>
                <td className="px-10 py-6">
                    <div className="p-3 bg-indigo-50/50 rounded-2xl border border-indigo-100/50 w-fit">
                        <p className="text-sm font-black text-indigo-600 italic">₹{u.walletBalance?.toFixed(0) || 0}</p>
                    </div>
                </td>
                <td className="px-10 py-6 text-center">
                  <span className={`px-4 py-1.5 rounded-full text-[9px] font-black uppercase border-2 ${u.isBlocked ? 'bg-red-50 border-red-100 text-red-500 shadow-lg shadow-red-100' : 'bg-emerald-50 border-emerald-100 text-emerald-500 shadow-lg shadow-emerald-100'}`}>{u.isBlocked ? 'Inhibited' : 'Active'}</span>
                </td>
                <td className="px-10 py-6 text-right space-x-3">
                  <button onClick={()=>onWarn(u)} className="p-3 bg-amber-50 text-amber-500 rounded-xl hover:bg-amber-500 hover:text-white transition-all shadow-sm" title="Issue Warning"><AlertTriangle size={16}/></button>
                  <button onClick={()=>onWallet(u)} className="p-3 bg-indigo-50 text-indigo-500 rounded-xl hover:bg-indigo-600 hover:text-white transition-all shadow-sm" title="Manual Wallet Control"><Wallet size={16}/></button>
                  <button onClick={()=>onBlock(u)} className={`p-3 rounded-xl transition-all shadow-sm ${u.isBlocked ? 'bg-emerald-50 text-emerald-500 hover:bg-emerald-500 hover:text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-800 hover:text-white'}`} title={u.isBlocked ? 'Restore Access' : 'Restrict Access'}><ShieldAlert size={16}/></button>
                  <button onClick={()=>onDelete(u)} className="p-3 bg-red-50 text-red-400 rounded-xl hover:bg-red-600 hover:text-white transition-all shadow-sm" title="Purge Account"><Trash2 size={16}/></button>
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
  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
    {providers.length === 0 ? (
       <div className="col-span-full p-32 bg-white rounded-[40px] border-4 border-dashed border-slate-50 text-center text-slate-300 font-black uppercase tracking-[10px]">Zero Requests</div>
    ) : (
      providers.map(p => (
        <div key={p.uid} className="bg-white p-10 rounded-[40px] shadow-sm border border-slate-100 relative group overflow-hidden">
          <div className="absolute top-0 right-0 p-8 flex flex-col gap-2">
              <button onClick={()=>onApprove(p.uid)} className="p-4 bg-emerald-500 text-white rounded-2xl shadow-xl shadow-emerald-200 hover:scale-110 transition-all font-black uppercase text-[10px] tracking-widest">Verify User</button>
              <button className="p-4 bg-red-50 text-red-400 rounded-2xl font-black uppercase text-[10px] tracking-widest">Reject</button>
          </div>
          <div className="flex flex-col items-center text-center space-y-6">
            <img src={p.profileImage || 'https://via.placeholder.com/80'} className="w-24 h-24 rounded-[32px] object-cover ring-8 ring-slate-50 shadow-xl" />
            <div>
                <h4 className="font-black text-2xl tracking-tighter text-slate-800 uppercase">{p.name}</h4>
                <p className="text-[10px] font-black text-indigo-500 uppercase tracking-[3px] mt-2 italic">{p.profession || 'Specialist'}</p>
            </div>
            <div className="w-full grid grid-cols-2 gap-2">
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                    <p className="text-[8px] font-black text-slate-400 uppercase tracking-widest mb-1">Aadhaar Document</p>
                    <p className="text-xs font-black text-slate-700">{p.aadhaarNumber}</p>
                </div>
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 flex items-center justify-center cursor-pointer hover:bg-indigo-50 transition-all">
                    <Eye size={18} className="text-indigo-400"/>
                </div>
            </div>
          </div>
        </div>
      ))
    )}
  </div>
);

const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-[32px] border border-slate-100 shadow-sm overflow-hidden">
    <div className="p-10 border-b border-slate-50 flex justify-between items-center">
        <h3 className="font-black text-slate-800 uppercase tracking-[4px]">Financial Clearance</h3>
    </div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead className="bg-slate-50/50 text-[10px] font-black text-slate-400 uppercase tracking-widest"><tr><th className="px-10 py-6">Partner Identity</th><th className="px-10 py-6">Liquidation Amount</th><th className="px-10 py-6 text-right">System Confirmation</th></tr></thead>
        <tbody className="divide-y divide-slate-50">
          {withdrawals.length === 0 ? (
            <tr><td colSpan="3" className="px-10 py-20 text-center text-slate-300 italic font-black uppercase tracking-[2px]">No Active Liquidation Requests</td></tr>
          ) : (
            withdrawals.map(w => (
              <tr key={w._id} className="hover:bg-slate-50/50 transition-all">
                <td className="px-10 py-8 font-black text-slate-800 uppercase tracking-tight">{w.providerName}</td>
                <td className="px-10 py-8 text-indigo-600 font-black text-3xl italic tracking-tighter">₹{w.amount}</td>
                <td className="px-10 py-8 text-right space-x-3">
                    <button onClick={()=>onHandle(w._id, 'approved')} className="px-8 py-3 bg-slate-900 text-white text-[10px] font-black rounded-xl uppercase tracking-widest shadow-xl shadow-slate-200">Confirm Payment</button>
                    <button onClick={()=>onHandle(w._id, 'rejected')} className="px-8 py-3 bg-red-50 text-red-400 text-[10px] font-black rounded-xl uppercase tracking-widest">Decline</button>
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
  <div className="bg-white rounded-[32px] border border-slate-100 shadow-sm overflow-hidden">
    <div className="px-10 py-6 border-b font-black uppercase text-[10px] tracking-[4px] bg-white text-slate-800 flex justify-between">
        <span>{title}</span>
        <span className="text-indigo-500 italic">{jobs?.length || 0} TOTAL OPS</span>
    </div>
    <table className="w-full text-left border-collapse">
      <thead className="bg-slate-50/50 text-[10px] font-black text-slate-400 uppercase tracking-widest border-b border-slate-50"><tr><th className="px-10 py-6">Mission Details</th><th className="px-10 py-6">Operational Status</th><th className="px-10 py-6 text-right">Resource Valuation</th></tr></thead>
      <tbody className="divide-y divide-slate-50">
        {(!jobs || jobs.length === 0) ? (
          <tr><td colSpan="3" className="px-10 py-20 text-center text-slate-300 italic font-black uppercase">Historical Data Unavailable</td></tr>
        ) : (
          jobs.map(j => (
            <tr key={j._id} className="hover:bg-slate-50/50 transition-all">
              <td className="px-10 py-8">
                  <p className="font-black text-slate-800 uppercase tracking-tight text-lg">{j.serviceName}</p>
                  <p className="text-[9px] font-black text-slate-300 uppercase tracking-[2px] mt-1">{new Date(j.createdAt).toLocaleString()}</p>
              </td>
              <td className="px-10 py-8">
                  <span className={`px-4 py-1.5 rounded-xl text-[9px] font-black uppercase border-2 ${j.status === 'done' ? 'bg-emerald-50 border-emerald-100 text-emerald-500' : 'bg-indigo-50 border-indigo-100 text-indigo-500'}`}>{j.status.replace('_',' ')}</span>
              </td>
              <td className="px-10 py-8 text-right font-black text-2xl italic tracking-tighter text-slate-800">₹{j.totalAmount}</td>
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
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">
      <div className="bg-slate-900 p-10 rounded-[40px] shadow-2xl h-fit border border-white/5">
        <h3 className="font-black mb-8 uppercase text-[10px] tracking-[4px] text-indigo-400">Initialize Module</h3>
        <div className="space-y-6">
            <input value={n} onChange={e=>setN(e.target.value)} placeholder="Display Name" className="w-full p-4 bg-white/5 border border-white/10 rounded-2xl font-bold text-white outline-none focus:border-indigo-500 transition-all" />
            <input value={i} onChange={e=>setI(e.target.value)} placeholder="Asset Vector (URL)" className="w-full p-4 bg-white/5 border border-white/10 rounded-2xl font-bold text-white outline-none focus:border-indigo-500 transition-all" />
            <button onClick={()=>adminApi.addCategory({name:n, icon:i}).then(refresh)} className="w-full py-5 bg-indigo-600 text-white font-black rounded-[20px] uppercase text-xs tracking-[3px] shadow-2xl shadow-indigo-900/50 hover:bg-indigo-500 transition-all">Deploy Category</button>
        </div>
      </div>
      <div className="lg:col-span-2 grid grid-cols-2 md:grid-cols-3 gap-6">
        {categories.map(c => (
            <div key={c._id} className="bg-white p-8 rounded-[32px] border border-slate-100 shadow-sm flex flex-col items-center hover:border-indigo-200 transition-all group relative">
                <img src={c.icon || 'https://via.placeholder.com/64'} className="w-16 h-16 object-contain mb-6 drop-shadow-xl group-hover:scale-110 transition-transform" />
                <p className="font-black text-slate-800 uppercase tracking-widest text-xs">{c.name}</p>
                <button className="absolute top-4 right-4 p-2 bg-red-50 text-red-400 rounded-full opacity-0 group-hover:opacity-100 transition-opacity"><Trash2 size={12}/></button>
            </div>
        ))}
      </div>
    </div>
  );
};

const ReportsView = ({ reports }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
    {reports.length === 0 ? <div className="col-span-full p-40 bg-white rounded-[40px] border-4 border-dashed border-slate-50 text-center text-slate-200 font-black uppercase tracking-[15px]">ALL CLEAR</div> : reports.map(r => (
      <div key={r._id} className="bg-white p-10 rounded-[40px] border-l-[12px] border-red-500 shadow-2xl relative overflow-hidden group">
        <div className="absolute top-0 right-0 px-6 py-2 bg-red-600 text-white text-[10px] font-black uppercase rounded-bl-[20px] tracking-widest">{r.status}</div>
        <h4 className="font-black text-slate-800 uppercase text-lg tracking-tight mb-4">{r.reason}</h4>
        <p className="text-sm font-medium text-slate-500 leading-relaxed italic opacity-80 mb-8">"{r.description}"</p>
        <div className="flex gap-3 overflow-x-auto pb-4 custom-scrollbar">
            {r.evidenceUrls?.map((u, i) => (
                <img key={i} src={u} className="w-20 h-20 rounded-3xl border-4 border-slate-50 object-cover shadow-lg hover:scale-110 transition-all" />
            ))}
        </div>
      </div>
    ))}
  </div>
);

const ReviewsView = ({ reviews, onDelete }) => (
  <div className="bg-white rounded-[32px] border border-slate-100 shadow-sm overflow-hidden">
    <table className="w-full text-left">
      <thead className="bg-slate-50/50 text-[10px] font-black text-slate-400 uppercase tracking-widest"><tr><th className="px-10 py-6">Consumer</th><th className="px-10 py-6">Critique</th><th className="px-10 py-6 text-right">Delete</th></tr></thead>
      <tbody className="divide-y divide-slate-50">
        {reviews.map(r => (
          <tr key={r._id} className="hover:bg-slate-50/50 transition-all">
            <td className="px-10 py-8">
                <p className="font-black text-slate-800 uppercase tracking-tight">{r.customerName}</p>
                <div className="flex gap-0.5 mt-2">
                    {[...Array(5)].map((_, i) => <Star key={i} size={12} className={i < r.rating ? 'text-amber-400 fill-amber-400' : 'text-slate-200'} />)}
                </div>
            </td>
            <td className="px-10 py-8 text-slate-500 italic font-medium max-w-lg leading-relaxed">"{r.comment}"</td>
            <td className="px-10 py-8 text-right"><button onClick={()=>onDelete(r._id)} className="p-4 bg-red-50 text-red-400 rounded-2xl hover:bg-red-500 hover:text-white transition-all"><Trash2 size={16}/></button></td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

const SettingsView = ({ settings, refresh }) => {
  const [c, setC] = useState(settings.commission_percentage || 15);
  return (
    <div className="bg-slate-900 p-12 rounded-[48px] border border-white/5 max-w-2xl shadow-[0_40px_100px_rgba(0,0,0,0.4)]">
      <h3 className="font-black uppercase text-[10px] tracking-[5px] mb-12 text-indigo-400">Platform Logic Control</h3>
      <div className="flex justify-between items-center mb-12">
          <div><p className="font-black text-white uppercase tracking-tight text-xl">Revenue Share Percentage</p><p className="text-slate-500 text-sm mt-1 font-medium italic">Global commission deducted from partner settlements.</p></div>
          <div className="flex items-center space-x-4"><input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-24 h-24 text-center bg-white/5 border-2 border-white/10 rounded-[32px] font-black text-4xl text-indigo-400 outline-none focus:border-indigo-500" /><span className="font-black text-white/10 text-6xl italic">%</span></div>
      </div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(()=>alert('Logic Updated Successfully'))} className="w-full py-6 bg-indigo-600 text-white font-black rounded-[24px] uppercase text-xs tracking-[5px] shadow-2xl shadow-indigo-900/40 hover:bg-indigo-500 transition-all">Synchronize App Rules</button>
    </div>
  );
};

const SupportView = ({ chats }) => (
  <div className="bg-white rounded-[40px] border border-slate-100 h-[600px] flex items-center justify-center text-slate-200 font-black uppercase tracking-[15px] text-center px-12">Support Grid Offline</div>
);
