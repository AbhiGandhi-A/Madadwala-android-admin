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

  useEffect(() => { fetchTabData(activeTab); }, [activeTab]);

  const fetchTabData = async (tab) => {
    setLoading(true);
    try {
      switch(tab) {
        case 'analytics': setData(p => ({...p, analytics: (await adminApi.getAnalytics()).data})); break;
        case 'providers-pending': setData(p => ({...p, pendingProviders: (await adminApi.getPendingProviders()).data})); break;
        case 'customers': setData(p => ({...p, allUsers: (await adminApi.getAllUsers()).data})); break;
        case 'providers-all': setData(p => ({...p, allProviders: (await adminApi.getAllProviders()).data})); break;
        case 'withdrawals': setData(p => ({...p, withdrawals: (await adminApi.getPendingWithdrawals()).data})); break;
        case 'jobs': setData(p => ({...p, activeJobs: (await adminApi.getActiveJobs()).data})); break;
        case 'bookings-all': setData(p => ({...p, allBookings: (await adminApi.getAllBookings()).data})); break;
        case 'categories': setData(p => ({...p, categories: (await adminApi.getCategories()).data})); break;
        case 'offers': setData(p => ({...p, offers: (await adminApi.getOffers()).data})); break;
        case 'banners': setData(p => ({...p, banners: (await adminApi.getBanners()).data})); break;
        case 'settings': setData(p => ({...p, settings: (await adminApi.getSettings()).data})); break;
        case 'support': setData(p => ({...p, chats: (await adminApi.getSupportChats()).data})); break;
        case 'reports': setData(p => ({...p, reports: (await adminApi.getReports()).data})); break;
        case 'reviews': setData(p => ({...p, reviews: (await adminApi.getAllReviews()).data})); break;
      }
    } catch (err) { console.error(err); }
    setLoading(false);
  };

  const sendWarning = async (uid, name) => {
    const title = prompt(`Warning Title for ${name}:`, "Account Warning");
    const message = prompt(`Warning Message for ${name}:`, "We noticed unusual activity. Please follow platform guidelines.");
    if(!title || !message) return;
    try {
      await adminApi.sendWarning({ uid, title, message, type: 'warning' });
      alert("Warning sent to user's notifications.");
    } catch(e) { alert("Failed to send"); }
  };

  const adjustWallet = async (uid, name) => {
    const type = confirm("OK for Credit (+), Cancel for Debit (-)") ? 'credit' : 'debit';
    const amount = prompt(`Amount to ${type} for ${name}:`);
    const description = prompt("Description:", "Admin manual adjustment");
    if(!amount) return;
    try {
      await adminApi.adjustWallet({ uid, amount, type, description });
      alert("Wallet updated.");
      fetchTabData(activeTab);
    } catch(e) { alert("Failed"); }
  };

  const deleteUser = async (uid, name) => {
    if(!confirm(`PERMANENTLY DELETE ${name}? This cannot be undone.`)) return;
    try {
      await adminApi.deleteUser(uid);
      alert("User deleted.");
      fetchTabData(activeTab);
    } catch(e) { alert("Failed"); }
  };

  const toggleBlock = async (uid, tab) => {
    try { await adminApi.toggleBlock(uid); fetchTabData(tab); } catch (err) { alert("Failed"); }
  };

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900">
      <aside className="w-64 bg-slate-900 text-white flex flex-col shadow-xl shrink-0">
        <div className="p-6 border-b border-slate-800 font-bold text-xl bg-gradient-to-r from-indigo-400 to-cyan-400 bg-clip-text text-transparent">Madadwala Admin</div>
        <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto custom-scrollbar text-sm">
          <NavItem icon={<BarChart3 size={18}/>} label="Dashboard" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Users</div>
          <NavItem icon={<Users size={18}/>} label="Customers" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={18}/>} label="Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={18}/>} label="Requests" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />
          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Jobs & Finance</div>
          <NavItem icon={<CreditCard size={18}/>} label="Payouts" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Clock size={18}/>} label="Live Now" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={18}/>} label="History" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />
          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Feedback</div>
          <NavItem icon={<MessageSquare size={18}/>} label="Chats" active={activeTab==='support'} onClick={()=>setActiveTab('support')} />
          <NavItem icon={<FileText size={18}/>} label="Reports" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />
          <NavItem icon={<Star size={18}/>} label="Reviews" active={activeTab==='reviews'} onClick={()=>setActiveTab('reviews')} />
          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Settings</div>
          <NavItem icon={<Tag size={18}/>} label="Categories" active={activeTab==='categories'} onClick={()=>setActiveTab('categories')} />
          <NavItem icon={<ImageIcon size={18}/>} label="Banners" active={activeTab==='banners'} onClick={()=>setActiveTab('banners')} />
          <NavItem icon={<Settings size={18}/>} label="App Rules" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>
      </aside>

      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8 shrink-0 shadow-sm">
          <h2 className="font-black text-gray-800 uppercase tracking-tight">{activeTab.replace('-', ' ')}</h2>
          <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white font-black shadow-lg shadow-indigo-100">A</div>
        </header>

        <main className="flex-1 overflow-y-auto p-8 custom-scrollbar">
          {loading ? (
             <div className="flex flex-col items-center justify-center h-full space-y-4">
               <div className="w-10 h-10 border-[3px] border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div>
             </div>
          ) : (
            <div className="max-w-7xl mx-auto animate-in fade-in duration-300">
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersTable users={data.allUsers} onWarn={(u)=>sendWarning(u.uid, u.name)} onWallet={(u)=>adjustWallet(u.uid, u.name)} onDelete={(u)=>deleteUser(u.uid, u.name)} onBlock={(u)=>toggleBlock(u.uid, 'customers')} />}
              {activeTab === 'providers-all' && <UsersTable users={data.allProviders} onWarn={(u)=>sendWarning(u.uid, u.name)} onWallet={(u)=>adjustWallet(u.uid, u.name)} onDelete={(u)=>deleteUser(u.uid, u.name)} onBlock={(u)=>toggleBlock(u.uid, 'providers-all')} isPartner />}
              {activeTab === 'providers-pending' && <PendingView providers={data.pendingProviders} onApprove={(uid)=>adminApi.approveProvider(uid).then(()=>fetchTabData('providers-pending'))} />}
              {activeTab === 'withdrawals' && <WithdrawalsTable withdrawals={data.withdrawals} onHandle={(id, s)=>adminApi.updateWithdrawal(id, {status:s}).then(()=>fetchTabData('withdrawals'))} />}
              {activeTab === 'jobs' && <JobsTable jobs={data.activeJobs} title="Live Tracking" />}
              {activeTab === 'bookings-all' && <JobsTable jobs={data.allBookings} title="All Platform Bookings" />}
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

const AnalyticsView = ({ data }) => !data ? null : (
  <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
    <StatCard title="Total Customers" value={data.totalUsers} color="text-blue-600" />
    <StatCard title="Verified Partners" value={data.totalProviders} color="text-emerald-600" />
    <StatCard title="Gross Revenue" value={`₹${data.totalRevenue}`} color="text-amber-600" />
    <StatCard title="Total Jobs" value={data.totalBookings} color="text-purple-600" />
  </div>
);

const StatCard = ({ title, value, color }) => (
  <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{title}</p>
    <p className={`text-3xl font-black mt-1 ${color}`}>{value}</p>
  </div>
);

const UsersTable = ({ users, onWarn, onWallet, onDelete, onBlock, isPartner }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <table className="w-full text-left border-collapse">
      <thead className="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest">
        <tr>
          <th className="px-8 py-4">User</th>
          <th className="px-8 py-4">Financials</th>
          <th className="px-8 py-4">Status</th>
          <th className="px-8 py-4 text-right">Admin Actions</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-gray-100">
        {users.map(u => (
          <tr key={u.uid} className="hover:bg-gray-50/30 transition-colors">
            <td className="px-8 py-5">
              <div className="flex items-center space-x-4">
                <img src={u.profileImage} className="w-10 h-10 rounded-xl object-cover ring-2 ring-gray-100" />
                <div><p className="font-black text-gray-900">{u.name}</p><p className="text-xs text-gray-400">{u.phoneNumber}</p></div>
              </div>
            </td>
            <td className="px-8 py-5">
              <p className="text-sm font-black text-indigo-600">₹{u.walletBalance?.toFixed(2)}</p>
              <p className="text-[10px] font-bold text-gray-400 uppercase">Wallet Balance</p>
            </td>
            <td className="px-8 py-5">
              <span className={`px-3 py-1 rounded-full text-[10px] font-black uppercase border ${u.isBlocked ? 'bg-red-50 text-red-600 border-red-100' : 'bg-emerald-50 text-emerald-600 border-emerald-100'}`}>{u.isBlocked ? 'Blocked' : 'Active'}</span>
            </td>
            <td className="px-8 py-5 text-right space-x-2">
              <button onClick={()=>onWarn(u)} className="p-2 bg-amber-50 text-amber-600 rounded-lg shadow-sm hover:bg-amber-100" title="Send Warning"><AlertTriangle size={16}/></button>
              <button onClick={()=>onWallet(u)} className="p-2 bg-indigo-50 text-indigo-600 rounded-lg shadow-sm hover:bg-indigo-100" title="Adjust Wallet"><Wallet size={16}/></button>
              <button onClick={()=>onBlock(u)} className={`p-2 rounded-lg shadow-sm ${u.isBlocked ? 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100' : 'bg-gray-50 text-gray-600 hover:bg-gray-100'}`} title={u.isBlocked ? 'Unblock' : 'Block'}><ShieldAlert size={16}/></button>
              <button onClick={()=>onDelete(u)} className="p-2 bg-red-50 text-red-600 rounded-lg shadow-sm hover:bg-red-100" title="Delete Permanent"><Trash2 size={16}/></button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

const PendingView = ({ providers, onApprove }) => (
  <div className="grid grid-cols-1 gap-4">
    {providers.map(p => (
      <div key={p.uid} className="bg-white p-6 rounded-2xl flex items-center justify-between shadow-sm border border-gray-100">
        <div className="flex items-center space-x-6">
          <img src={p.profileImage} className="w-16 h-16 rounded-2xl object-cover" />
          <div><h4 className="font-black text-lg">{p.name}</h4><p className="text-xs font-bold text-indigo-600 uppercase tracking-tighter">{p.profession} | ID: {p.aadhaarNumber}</p></div>
        </div>
        <button onClick={()=>onApprove(p.uid)} className="px-8 py-3 bg-emerald-600 text-white font-black text-xs uppercase rounded-xl shadow-lg shadow-emerald-100">Approve Now</button>
      </div>
    ))}
  </div>
);

const WithdrawalsTable = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <table className="w-full text-left">
      <thead className="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest border-b"><tr><th className="px-8 py-4">Partner</th><th className="px-8 py-4">Amount</th><th className="px-8 py-4">Actions</th></tr></thead>
      <tbody className="divide-y divide-gray-100">
        {withdrawals.map(w => (
          <tr key={w._id}>
            <td className="px-8 py-5 font-bold">{w.providerName}</td>
            <td className="px-8 py-5 text-indigo-600 font-black italic text-xl">₹{w.amount}</td>
            <td className="px-8 py-5 space-x-2"><button onClick={()=>onHandle(w._id, 'approved')} className="px-4 py-2 bg-indigo-600 text-white text-xs font-black rounded-lg uppercase">Pay</button><button onClick={()=>onHandle(w._id, 'rejected')} className="px-4 py-2 bg-gray-100 text-gray-500 text-xs font-black rounded-lg uppercase">Decline</button></td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

const JobsTable = ({ jobs, title }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-6 border-b font-black uppercase text-xs tracking-widest bg-gray-50/30">{title}</div>
    <table className="w-full text-left">
      <thead className="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest border-b"><tr><th className="px-8 py-4">Service</th><th className="px-8 py-4">Parties</th><th className="px-8 py-4">Amount</th><th className="px-8 py-4">Status</th></tr></thead>
      <tbody className="divide-y divide-gray-100">
        {jobs.map(j => (
          <tr key={j._id} className="text-sm">
            <td className="px-8 py-4 font-bold">{j.serviceName}</td>
            <td className="px-8 py-4 text-xs font-medium text-gray-500">C: {j.customerName}<br/>P: {j.providerName}</td>
            <td className="px-8 py-4 font-black">₹{j.totalAmount}</td>
            <td className="px-8 py-4"><span className="px-2 py-0.5 bg-gray-100 rounded text-[10px] font-black uppercase">{j.status.replace('_',' ')}</span></td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

const CategoriesView = ({ categories, refresh }) => {
  const [n, setN] = useState(''); const [i, setI] = useState('');
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
      <div className="bg-white p-8 rounded-2xl border border-gray-100 h-fit">
        <h3 className="font-black mb-6 uppercase text-xs tracking-widest">New Category</h3>
        <div className="space-y-4"><input value={n} onChange={e=>setN(e.target.value)} placeholder="Name" className="w-full p-3 bg-gray-50 rounded-xl font-bold border-none outline-none"/><input value={i} onChange={e=>setI(e.target.value)} placeholder="Icon URL" className="w-full p-3 bg-gray-50 rounded-xl font-bold border-none outline-none"/><button onClick={()=>adminApi.addCategory({name:n, icon:i}).then(refresh)} className="w-full py-4 bg-indigo-600 text-white font-black rounded-xl uppercase text-xs">Save</button></div>
      </div>
      <div className="md:col-span-2 grid grid-cols-4 gap-4">
        {categories.map(c => <div key={c._id} className="p-4 bg-white rounded-2xl border border-gray-100 flex flex-col items-center"><img src={c.icon} className="w-8 h-8 mb-2"/><p className="text-[10px] font-black uppercase">{c.name}</p></div>)}
      </div>
    </div>
  );
};

const ReportsView = ({ reports }) => (
  <div className="grid grid-cols-2 gap-6">
    {reports.map(r => (
      <div key={r._id} className="bg-white p-6 rounded-2xl border-l-4 border-red-500 shadow-sm">
        <h4 className="font-black text-red-600 uppercase text-xs tracking-widest mb-2">{r.reason}</h4>
        <p className="text-sm font-medium text-gray-600 italic">"{r.description}"</p>
        <div className="mt-4 flex -space-x-2">{r.evidenceUrls?.map((u, i) => <img key={i} src={u} className="w-10 h-10 rounded-lg border-2 border-white object-cover shadow-sm"/>)}</div>
      </div>
    ))}
  </div>
);

const ReviewsView = ({ reviews, onDelete }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <table className="w-full text-left">
      <thead className="bg-gray-50/50 text-[10px] font-black text-gray-400 uppercase tracking-widest"><tr><th className="px-8 py-4">Customer</th><th className="px-8 py-4">Rating</th><th className="px-8 py-4">Comment</th><th className="px-8 py-4 text-right">Actions</th></tr></thead>
      <tbody className="divide-y divide-gray-100">
        {reviews.map(r => (
          <tr key={r._id} className="text-sm">
            <td className="px-8 py-4 font-bold">{r.customerName}</td>
            <td className="px-8 py-4 text-amber-500 font-black">★ {r.rating}</td>
            <td className="px-8 py-4 text-gray-500 italic">"{r.comment}"</td>
            <td className="px-8 py-4 text-right"><button onClick={()=>onDelete(r._id)} className="text-red-500"><Trash2 size={16}/></button></td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

const SettingsView = ({ settings, refresh }) => {
  const [c, setC] = useState(settings.commission_percentage || 15);
  return (
    <div className="bg-white p-10 rounded-3xl border border-gray-100 max-w-lg">
      <h3 className="font-black uppercase text-xs tracking-widest mb-8">Revenue Control</h3>
      <div className="flex justify-between items-center mb-8"><p className="font-bold text-gray-600">Platform Commission</p><div className="flex items-center space-x-2"><input type="number" value={c} onChange={e=>setC(e.target.value)} className="w-16 p-2 bg-gray-50 rounded-lg text-center font-black text-indigo-600 border-none outline-none"/><span className="font-black text-gray-300">%</span></div></div>
      <button onClick={()=>adminApi.updateSetting('commission_percentage', c).then(()=>alert('Updated'))} className="w-full py-4 bg-slate-900 text-white font-black rounded-xl uppercase text-xs tracking-widest">Save Settings</button>
    </div>
  );
};

const SupportView = ({ chats }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 h-[500px] flex items-center justify-center text-gray-400 font-bold uppercase tracking-widest">Support Portal - Select Chat on Left (Backend Connected)</div>
);
