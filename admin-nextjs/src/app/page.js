'use client';
import React, { useState, useEffect } from 'react';
import {
  Users, Briefcase, CheckCircle, XCircle, BarChart3,
  Settings, LogOut, MessageSquare, Image as ImageIcon,
  Tag, CreditCard, Bell, ChevronRight, Search, ShieldAlert,
  UserCheck, UserMinus, Clock, Filter, FileText, AlertTriangle,
  Trash2, Plus, Star, Wallet
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

  useEffect(() => { fetchTabData(activeTab); }, [activeTab]);

  const fetchTabData = async (tab) => {
    setLoading(true);
    setError(null);
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
    } catch (err) {
      console.error(err);
      setError("Failed to load data. Please check if backend is running.");
    }
    setLoading(false);
  };

  const sendWarning = async (uid, name) => {
    const title = prompt(`Warning Title for ${name}:`, "Account Warning");
    const message = prompt(`Warning Message for ${name}:`, "Please follow platform guidelines.");
    if(!title || !message) return;
    try {
      await adminApi.sendWarning({ uid, title, message, type: 'warning' });
      alert("Warning sent!");
    } catch(e) { alert("Failed to send notification"); }
  };

  const adjustWallet = async (uid, name) => {
    const type = confirm("Credit (+) click OK. Debit (-) click Cancel.") ? 'credit' : 'debit';
    const amount = prompt(`Amount to ${type} for ${name}:`);
    const description = prompt("Description:", "Admin adjustment");
    if(!amount) return;
    try {
      await adminApi.adjustWallet({ uid, amount, type, description });
      alert("Wallet updated!");
      fetchTabData(activeTab);
    } catch(e) { alert("Update failed"); }
  };

  const deleteUser = async (uid, name) => {
    if(!confirm(`Delete ${name} PERMANENTLY?`)) return;
    try {
      await adminApi.deleteUser(uid);
      alert("Deleted!");
      fetchTabData(activeTab);
    } catch(e) { alert("Delete failed"); }
  };

  const toggleBlock = async (uid, tab) => {
    try { await adminApi.toggleBlock(uid); fetchTabData(tab); } catch (err) { alert("Action failed"); }
  };

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900">
      <aside className="w-64 bg-slate-900 text-white flex flex-col shadow-xl shrink-0">
        <div className="p-6 border-b border-slate-800 font-bold text-xl text-indigo-400">Madadwala Admin</div>
        <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto custom-scrollbar text-sm">
          <NavItem icon={<BarChart3 size={18}/>} label="Dashboard" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Management</div>
          <NavItem icon={<Users size={18}/>} label="Customers" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={18}/>} label="Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={18}/>} label="Pending Approvals" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />
          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Finance & Jobs</div>
          <NavItem icon={<CreditCard size={18}/>} label="Withdrawals" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Clock size={18}/>} label="Live Tracking" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={18}/>} label="All Bookings" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />
          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Feedback</div>
          <NavItem icon={<MessageSquare size={18}/>} label="Support" active={activeTab==='support'} onClick={()=>setActiveTab('support')} />
          <NavItem icon={<FileText size={18}/>} label="Reports" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />
          <NavItem icon={<Star size={18}/>} label="Reviews" active={activeTab==='reviews'} onClick={()=>setActiveTab('reviews')} />
          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Config</div>
          <NavItem icon={<Tag size={18}/>} label="Categories" active={activeTab==='categories'} onClick={()=>setActiveTab('categories')} />
          <NavItem icon={<ImageIcon size={18}/>} label="Banners" active={activeTab==='banners'} onClick={()=>setActiveTab('banners')} />
          <NavItem icon={<Settings size={18}/>} label="Settings" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
      </aside>

      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8 shrink-0">
          <h2 className="font-bold text-gray-800 uppercase tracking-tight">{activeTab.replace('-', ' ')}</h2>
          {error && <div className="text-red-500 text-xs font-bold bg-red-50 px-4 py-2 rounded-lg border border-red-100">{error}</div>}
          <div className="w-10 h-10 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-bold shadow-lg">A</div>
        </header>

        <main className="flex-1 overflow-y-auto p-8 custom-scrollbar">
          {loading ? (
             <div className="flex flex-col items-center justify-center h-full space-y-4">
               <div className="w-8 h-8 border-2 border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div>
               <p className="text-gray-400 text-xs font-bold uppercase tracking-widest">Fetching Data...</p>
             </div>
          ) : (
            <div className="max-w-7xl mx-auto animate-in fade-in duration-300">
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>sendWarning(u.uid, u.name)} onWallet={(u)=>adjustWallet(u.uid, u.name)} onDelete={(u)=>deleteUser(u.uid, u.name)} onBlock={(u)=>toggleBlock(u.uid, 'customers')} title="Customers" />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>sendWarning(u.uid, u.name)} onWallet={(u)=>adjustWallet(u.uid, u.name)} onDelete={(u)=>deleteUser(u.uid, u.name)} onBlock={(u)=>toggleBlock(u.uid, 'providers-all')} title="Partners" isPartner />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>fetchTabData('providers-pending'))} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>fetchTabData('withdrawals'))} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Real-time Tracking" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="Complete Booking History" />}
              {activeTab === 'categories' && <CategoriesView categories={data.categories} refresh={()=>fetchTabData('categories')} />}
              {activeTab === 'reports' && <ReportsView reports={data.reports} />}
              {activeTab === 'reviews' && <ReviewsView reviews={data.reviews} onDelete={(id)=>adminApi.deleteReview(id).then(()=>fetchTabData('reviews'))} />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>fetchTabData('settings')} />}
              {activeTab === 'support' && <SupportView chats={data.chats} />}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

function NavItem({ icon, label, active, onClick, count }) {
  return (
    <button onClick={onClick} className={`w-full flex items-center justify-between px-4 py-2.5 rounded-lg transition-all ${active ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'}`}>
      <div className="flex items-center space-x-3">{icon}<span className="font-bold">{label}</span></div>
      {count > 0 && <span className={`px-2 py-0.5 text-[10px] font-black rounded-md ${active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'}`}>{count}</span>}
    </button>
  );
}

const AnalyticsView = ({ data }) => !data ? <div className="text-gray-400 font-bold p-8 bg-white rounded-2xl border-2 border-dashed border-gray-100 text-center uppercase tracking-widest">No Analytics Data Available</div> : (
  <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
    <StatCard title="Total Customers" value={data.totalUsers} color="text-blue-600" />
    <StatCard title="Total Partners" value={data.totalProviders} color="text-emerald-600" />
    <StatCard title="Gross Revenue" value={`₹${data.totalRevenue}`} color="text-amber-600" />
    <StatCard title="Total Jobs" value={data.totalBookings} color="text-purple-600" />
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{title}</p>
    <p className={`text-3xl font-black mt-1 ${color}`}>{value || 0}</p>
  </div>
);

const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, title, isPartner }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-4 border-b font-bold text-sm text-gray-600 bg-gray-50/50 uppercase tracking-tight">{title} List ({users?.length || 0})</div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead className="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest">
          <tr>
            <th className="px-8 py-4">User</th>
            <th className="px-8 py-4">Wallet</th>
            <th className="px-8 py-4">Status</th>
            <th className="px-8 py-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {!users || users.length === 0 ? (
            <tr><td colSpan="4" className="px-8 py-10 text-center text-gray-400 italic">No {title.toLowerCase()} found in database.</td></tr>
          ) : (
            users.map(u => (
              <tr key={u.uid} className="hover:bg-gray-50/30 transition-colors">
                <td className="px-8 py-4">
                  <div className="flex items-center space-x-3">
                    <img src={u.profileImage || 'https://via.placeholder.com/40'} className="w-10 h-10 rounded-xl object-cover" />
                    <div><p className="font-bold text-gray-900">{u.name || 'No Name'}</p><p className="text-[10px] text-gray-400">{u.phoneNumber}</p></div>
                  </div>
                </td>
                <td className="px-8 py-4 text-sm font-black text-indigo-600">₹{u.walletBalance?.toFixed(0) || 0}</td>
                <td className="px-8 py-4">
                  <span className={`px-2 py-0.5 rounded text-[10px] font-black uppercase ${u.isBlocked ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-600'}`}>{u.isBlocked ? 'Blocked' : 'Active'}</span>
                </td>
                <td className="px-8 py-4 text-right space-x-2">
                  <button onClick={()=>onWarn(u)} className="p-2 bg-amber-50 text-amber-600 rounded-lg"><AlertTriangle size={14}/></button>
                  <button onClick={()=>onWallet(u)} className="p-2 bg-indigo-50 text-indigo-600 rounded-lg"><Wallet size={14}/></button>
                  <button onClick={()=>onBlock(u)} className="p-2 bg-gray-50 text-gray-600 rounded-lg"><ShieldAlert size={14}/></button>
                  <button onClick={()=>onDelete(u)} className="p-2 bg-red-50 text-red-600 rounded-lg"><Trash2 size={14}/></button>
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
  <div className="space-y-4">
    {providers.length === 0 ? (
       <div className="p-12 bg-white rounded-2xl border-2 border-dashed border-gray-100 text-center text-gray-400 font-bold uppercase tracking-widest">No Pending Approvals</div>
    ) : (
      providers.map(p => (
        <div key={p.uid} className="bg-white p-6 rounded-2xl flex items-center justify-between shadow-sm border border-gray-100">
          <div className="flex items-center space-x-6">
            <img src={p.profileImage || 'https://via.placeholder.com/64'} className="w-14 h-14 rounded-2xl object-cover" />
            <div><h4 className="font-bold text-lg">{p.name}</h4><p className="text-xs font-bold text-gray-400 uppercase">{p.profession} | Aadhaar: {p.aadhaarNumber}</p></div>
          </div>
          <button onClick={()=>onApprove(p.uid)} className="px-6 py-3 bg-emerald-600 text-white font-black text-xs uppercase rounded-xl">Approve</button>
        </div>
      ))
    )}
  </div>
);

const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <table className="w-full text-left">
      <thead className="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest"><tr><th className="px-8 py-4">Partner</th><th className="px-8 py-4">Amount</th><th className="px-8 py-4 text-right">Actions</th></tr></thead>
      <tbody className="divide-y divide-gray-100">
        {withdrawals.length === 0 ? (
          <tr><td colSpan="3" className="px-8 py-10 text-center text-gray-400 italic">No pending payout requests.</td></tr>
        ) : (
          withdrawals.map(w => (
            <tr key={w._id}>
              <td className="px-8 py-4 font-bold">{w.providerName}</td>
              <td className="px-8 py-4 text-indigo-600 font-black italic">₹{w.amount}</td>
              <td className="px-8 py-4 text-right"><button onClick={()=>onHandle(w._id, 'approved')} className="px-4 py-2 bg-indigo-600 text-white text-xs font-black rounded-lg uppercase">Pay Now</button></td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  </div>
);

const JobsTable = ({ jobs, title }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-4 border-b font-bold text-xs tracking-widest bg-gray-50/50 text-gray-500 uppercase">{title} ({jobs?.length || 0})</div>
    <table className="w-full text-left">
      <thead className="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest"><tr><th className="px-8 py-4">Service</th><th className="px-8 py-4">Status</th><th className="px-8 py-4 text-right">Total</th></tr></thead>
      <tbody className="divide-y divide-gray-100">
        {(!jobs || jobs.length === 0) ? (
          <tr><td colSpan="3" className="px-8 py-10 text-center text-gray-400 italic">No bookings found.</td></tr>
        ) : (
          jobs.map(j => (
            <tr key={j._id} className="text-sm">
              <td className="px-8 py-4 font-bold text-gray-700">{j.serviceName}<p className="text-[10px] font-bold text-gray-400 uppercase">{new Date(j.createdAt).toLocaleString()}</p></td>
              <td className="px-8 py-4"><span className="px-2 py-0.5 bg-gray-100 rounded text-[10px] font-black uppercase">{j.status.replace('_',' ')}</span></td>
              <td className="px-8 py-4 text-right font-black">₹{j.totalAmount}</td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  </div>
);

const CategoriesView = ({ categories, refresh }) => {
  const [n, setN] = useState(''); const [i, setI] = useState('');
  const handleAdd = () => adminApi.addCategory({name:n, icon:i}).then(()=>{setN('');setI('');refresh();}).catch(()=>alert('Error adding category'));
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
      <div className="bg-white p-8 rounded-2xl border border-gray-100 h-fit">
        <h3 className="font-black mb-6 uppercase text-[10px] tracking-widest text-gray-400">Add New Category</h3>
        <div className="space-y-4"><input value={n} onChange={e=>setN(e.target.value)} placeholder="Category Name" className="w-full p-3 bg-gray-50 rounded-xl font-bold border-none outline-none"/><input value={i} onChange={e=>setI(e.target.value)} placeholder="Icon URL" className="w-full p-3 bg-gray-50 rounded-xl font-bold border-none outline-none"/><button onClick={handleAdd} className="w-full py-4 bg-indigo-600 text-white font-black rounded-xl uppercase text-xs tracking-widest">Save Category</button></div>
      </div>
      <div className="md:col-span-2 grid grid-cols-2 sm:grid-cols-4 gap-4">
        {categories.length === 0 ? <div className="col-span-full p-12 text-center text-gray-400 font-bold">No Categories Found</div> : categories.map(c => <div key={c._id} className="p-4 bg-white rounded-2xl border border-gray-100 flex flex-col items-center"><img src={c.icon || 'https://via.placeholder.com/32'} className="w-8 h-8 mb-2"/><p className="text-[10px] font-black uppercase text-center">{c.name}</p></div>)}
      </div>
    </div>
  );
};

const ReportsView = ({ reports }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
    {reports.length === 0 ? <div className="col-span-full p-12 text-center text-gray-400 font-bold italic uppercase tracking-widest">No User Reports Found</div> : reports.map(r => (
      <div key={r._id} className="bg-white p-6 rounded-2xl border-l-4 border-red-500 shadow-sm">
        <h4 className="font-black text-red-600 uppercase text-xs tracking-widest mb-2">{r.reason}</h4>
        <p className="text-sm font-medium text-gray-600">"{r.description}"</p>
        <div className="mt-4 flex -space-x-2 overflow-hidden">{r.evidenceUrls?.map((u, i) => <img key={i} src={u} className="w-12 h-12 rounded-lg border-2 border-white object-cover"/>)}</div>
      </div>
    ))}
  </div>
);

const ReviewsView = ({ reviews, onDelete }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <table className="w-full text-left">
      <thead className="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest"><tr><th className="px-8 py-4">Customer</th><th className="px-8 py-4">Comment</th><th className="px-8 py-4 text-right">Delete</th></tr></thead>
      <tbody className="divide-y divide-gray-100">
        {reviews.length === 0 ? (
          <tr><td colSpan="3" className="px-8 py-10 text-center text-gray-400 italic">No platform reviews yet.</td></tr>
        ) : (
          reviews.map(r => (
            <tr key={r._id} className="text-sm">
              <td className="px-8 py-4 font-bold">{r.customerName}<p className="text-amber-500 text-xs">★ {r.rating}</p></td>
              <td className="px-8 py-4 text-gray-500 italic font-medium">"{r.comment}"</td>
              <td className="px-8 py-4 text-right"><button onClick={()=>onDelete(r._id)} className="text-red-400 hover:text-red-600"><Trash2 size={16}/></button></td>
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
    <div className="bg-white p-10 rounded-3xl border border-gray-100 max-w-lg shadow-sm">
      <h3 className="font-black uppercase text-xs tracking-widest mb-8 text-gray-400">Global App Settings</h3>
      <div className="flex justify-between items-center mb-8"><p className="font-bold text-gray-700">Platform Commission Fee</p><div className="flex items-center space-x-2"><input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-16 p-2 bg-gray-50 rounded-lg text-center font-black text-indigo-600 border-none outline-none"/><span className="font-black text-gray-300">%</span></div></div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(()=>alert('Commission Updated!'))} className="w-full py-4 bg-slate-900 text-white font-black rounded-xl uppercase text-xs tracking-widest">Update Settings</button>
    </div>
  );
};

const SupportView = ({ chats }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 h-[500px] flex items-center justify-center text-gray-300 font-bold uppercase tracking-widest text-center px-12">Support Portal<br/>Please connect to Firebase for real-time chat interface.</div>
);
