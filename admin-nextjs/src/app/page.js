'use client';
import React, { useState, useEffect } from 'react';
import {
  Users, Briefcase, CheckCircle, XCircle, BarChart3,
  Settings, LogOut, MessageSquare, Image as ImageIcon,
  Tag, CreditCard, Bell, ChevronRight, Search, ShieldAlert,
  UserCheck, UserMinus, Clock, Filter, FileText
} from 'lucide-react';
import { adminApi } from '@/lib/api';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('analytics');
  const [data, setData] = useState({
    analytics: null,
    pendingProviders: [],
    withdrawals: [],
    activeJobs: [],
    categories: [],
    offers: [],
    banners: [],
    settings: {},
    chats: [],
    allUsers: [],
    allProviders: [],
    allBookings: [],
    reports: []
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchTabData(activeTab);
  }, [activeTab]);

  const fetchTabData = async (tab) => {
    setLoading(true);
    try {
      switch(tab) {
        case 'analytics':
          const a = await adminApi.getAnalytics();
          setData(prev => ({ ...prev, analytics: a.data }));
          break;
        case 'providers-pending':
          const pp = await adminApi.getPendingProviders();
          setData(prev => ({ ...prev, pendingProviders: pp.data }));
          break;
        case 'customers':
          const u = await adminApi.getAllUsers();
          setData(prev => ({ ...prev, allUsers: u.data }));
          break;
        case 'providers-all':
          const pa = await adminApi.getAllProviders();
          setData(prev => ({ ...prev, allProviders: pa.data }));
          break;
        case 'withdrawals':
          const w = await adminApi.getPendingWithdrawals();
          setData(prev => ({ ...prev, withdrawals: w.data }));
          break;
        case 'jobs':
          const j = await adminApi.getActiveJobs();
          setData(prev => ({ ...prev, activeJobs: j.data }));
          break;
        case 'bookings-all':
          const bks = await adminApi.getAllBookings();
          setData(prev => ({ ...prev, allBookings: bks.data }));
          break;
        case 'categories':
          const c = await adminApi.getCategories();
          setData(prev => ({ ...prev, categories: c.data }));
          break;
        case 'offers':
          const o = await adminApi.getOffers();
          setData(prev => ({ ...prev, offers: o.data }));
          break;
        case 'banners':
          const b = await adminApi.getBanners();
          setData(prev => ({ ...prev, banners: b.data }));
          break;
        case 'settings':
          const s = await adminApi.getSettings();
          setData(prev => ({ ...prev, settings: s.data }));
          break;
        case 'support':
          const ch = await adminApi.getSupportChats();
          setData(prev => ({ ...prev, chats: ch.data }));
          break;
        case 'reports':
          const r = await adminApi.getReports();
          setData(prev => ({ ...prev, reports: r.data }));
          break;
      }
    } catch (err) {
      console.error("Fetch error:", err);
    }
    setLoading(false);
  };

  const handleApproveProvider = async (uid) => {
    if(!confirm("Approve this partner?")) return;
    try {
      await adminApi.approveProvider(uid);
      fetchTabData('providers-pending');
    } catch (err) { alert("Error approving"); }
  };

  const handleWithdrawal = async (id, status) => {
    const reason = status === 'rejected' ? prompt("Reason for rejection?") : null;
    if (status === 'rejected' && !reason) return;
    try {
      await adminApi.updateWithdrawal(id, { status, rejectionReason: reason });
      fetchTabData('withdrawals');
    } catch (err) { alert("Action failed"); }
  };

  const toggleBlock = async (uid, tab) => {
    try {
      const res = await adminApi.toggleBlock(uid);
      alert(res.data.message);
      fetchTabData(tab);
    } catch (err) { alert("Toggle block failed"); }
  };

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 text-white flex flex-col shadow-xl shrink-0">
        <div className="p-6 border-b border-slate-800">
          <h1 className="text-xl font-bold bg-gradient-to-r from-indigo-400 to-cyan-400 bg-clip-text text-transparent">
            Madadwala Admin
          </h1>
        </div>

        <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto custom-scrollbar">
          <NavItem icon={<BarChart3 size={18}/>} label="Dashboard" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />

          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-[2px]">Management</div>
          <NavItem icon={<Users size={18}/>} label="Customers" active={activeTab==='customers'} onClick={()=>setActiveTab('customers')} />
          <NavItem icon={<UserCheck size={18}/>} label="All Partners" active={activeTab==='providers-all'} onClick={()=>setActiveTab('providers-all')} />
          <NavItem icon={<ShieldAlert size={18}/>} label="Verifications" active={activeTab==='providers-pending'} onClick={()=>setActiveTab('providers-pending')} count={data.pendingProviders.length} />

          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-[2px]">Operations</div>
          <NavItem icon={<CreditCard size={18}/>} label="Withdrawals" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Clock size={18}/>} label="Live Jobs" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<Briefcase size={18}/>} label="Booking History" active={activeTab==='bookings-all'} onClick={()=>setActiveTab('bookings-all')} />
          <NavItem icon={<MessageSquare size={18}/>} label="Support Chats" active={activeTab==='support'} onClick={()=>setActiveTab('support')} />
          <NavItem icon={<FileText size={18}/>} label="User Reports" active={activeTab==='reports'} onClick={()=>setActiveTab('reports')} />

          <div className="pt-4 pb-2 px-2 text-[10px] font-bold text-slate-500 uppercase tracking-[2px]">CMS & Settings</div>
          <NavItem icon={<Tag size={18}/>} label="Categories" active={activeTab==='categories'} onClick={()=>setActiveTab('categories')} />
          <NavItem icon={<ImageIcon size={18}/>} label="Home Banners" active={activeTab==='banners'} onClick={()=>setActiveTab('banners')} />
          <NavItem icon={<Bell size={18}/>} label="Offers/Coupons" active={activeTab==='offers'} onClick={()=>setActiveTab('offers')} />
          <NavItem icon={<Settings size={18}/>} label="App Settings" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>

        <div className="p-4 border-t border-slate-800">
          <button className="flex items-center space-x-3 px-4 py-3 w-full rounded-lg hover:bg-red-500/10 text-red-400 transition-colors">
            <LogOut size={18} />
            <span className="font-medium text-sm">Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8 shrink-0">
          <h2 className="text-lg font-bold text-gray-800 capitalize tracking-tight">
            {activeTab.replace('-', ' ')}
          </h2>

          <div className="flex items-center space-x-6">
            <div className="relative group">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-indigo-500 transition-colors" size={16} />
              <input
                type="text"
                placeholder="Search anything..."
                className="pl-10 pr-4 py-2 bg-gray-100 border-transparent focus:bg-white focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 rounded-lg text-sm transition-all outline-none w-64"
              />
            </div>
            <div className="flex items-center space-x-3 pl-4 border-l border-gray-200">
              <div className="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-black shadow-lg shadow-indigo-100">
                A
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-8 custom-scrollbar">
          {loading ? (
            <div className="flex flex-col items-center justify-center h-full space-y-4">
              <div className="w-10 h-10 border-[3px] border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div>
              <p className="text-gray-400 text-sm font-bold uppercase tracking-widest">Updating View...</p>
            </div>
          ) : (
            <div className="max-w-7xl mx-auto animate-in fade-in slide-in-from-bottom-2 duration-500">
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'customers' && <UsersView users={data.allUsers} onToggleBlock={(uid)=>toggleBlock(uid, 'customers')} type="Customer" />}
              {activeTab === 'providers-all' && <UsersView users={data.allProviders} onToggleBlock={(uid)=>toggleBlock(uid, 'providers-all')} type="Partner" />}
              {activeTab === 'providers-pending' && <PendingProvidersView providers={data.pendingProviders} onApprove={handleApproveProvider} />}
              {activeTab === 'withdrawals' && <WithdrawalsView withdrawals={data.withdrawals} onHandle={handleWithdrawal} />}
              {activeTab === 'jobs' && <JobsView jobs={data.activeJobs} title="Live Active Jobs" />}
              {activeTab === 'bookings-all' && <JobsView jobs={data.allBookings} title="Platform Booking History" />}
              {activeTab === 'categories' && <CategoriesView categories={data.categories} refresh={()=>fetchTabData('categories')} />}
              {activeTab === 'offers' && <OffersView offers={data.offers} refresh={()=>fetchTabData('offers')} />}
              {activeTab === 'banners' && <BannersView banners={data.banners} refresh={()=>fetchTabData('banners')} />}
              {activeTab === 'settings' && <SettingsView settings={data.settings} refresh={()=>fetchTabData('settings')} />}
              {activeTab === 'support' && <SupportView chats={data.chats} />}
              {activeTab === 'reports' && <ReportsView reports={data.reports} />}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

function NavItem({ icon, label, active, onClick, count }) {
  return (
    <button
      onClick={onClick}
      className={`w-full flex items-center justify-between px-4 py-2.5 rounded-lg transition-all duration-200 ${
        active
          ? 'bg-indigo-600 text-white shadow-md'
          : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
      }`}
    >
      <div className="flex items-center space-x-3">
        <span className={active ? 'text-white' : 'text-slate-500'}>{icon}</span>
        <span className="font-semibold text-sm">{label}</span>
      </div>
      {count > 0 && (
        <span className={`px-2 py-0.5 text-[10px] font-black rounded-md ${
          active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'
        }`}>
          {count}
        </span>
      )}
    </button>
  );
}

const AnalyticsView = ({ data }) => {
  if (!data) return null;
  return (
    <div className="space-y-8">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Customers" value={data.totalUsers} icon={<Users className="text-blue-600" />} />
        <StatCard title="Partners" value={data.totalProviders} icon={<UserCheck className="text-emerald-600" />} />
        <StatCard title="Bookings" value={data.totalBookings} icon={<Briefcase className="text-purple-600" />} />
        <StatCard title="Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} icon={<CreditCard className="text-amber-600" />} />
      </div>

      <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
        <h3 className="text-lg font-bold text-gray-800 mb-6">Market Share by Category</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-6">
          {data.categories?.map((cat, i) => (
            <div key={i}>
              <div className="flex justify-between mb-2">
                <span className="text-sm font-bold text-gray-600">{cat.name}</span>
                <span className="text-sm font-black text-indigo-600">{Math.round(cat.ratio * 100)}%</span>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-2">
                <div className="bg-indigo-500 h-full rounded-full" style={{ width: `${cat.ratio * 100}%` }}></div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

const StatCard = ({ title, value, icon }) => (
  <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
    <div className="p-3 bg-gray-50 rounded-xl w-fit mb-4">{icon}</div>
    <p className="text-xs font-black text-gray-400 uppercase tracking-widest">{title}</p>
    <p className="text-3xl font-black text-gray-900 mt-1">{value}</p>
  </div>
);

const UsersView = ({ users, onToggleBlock, type }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-6 border-b border-gray-100 bg-gray-50/30 flex justify-between items-center">
        <h3 className="font-black text-gray-800 uppercase tracking-tight">Registered {type}s</h3>
        <div className="flex items-center space-x-2">
            <span className="text-xs font-bold text-gray-400">Sort by:</span>
            <select className="text-xs font-bold bg-transparent outline-none">
                <option>Newest First</option>
                <option>Oldest First</option>
            </select>
        </div>
    </div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead>
          <tr className="text-gray-400 text-[10px] font-black uppercase tracking-widest bg-gray-50/50">
            <th className="px-8 py-4">User Profile</th>
            <th className="px-8 py-4">Contact Info</th>
            <th className="px-8 py-4">Join Date</th>
            <th className="px-8 py-4">Status</th>
            <th className="px-8 py-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {users.map((u) => (
            <tr key={u.uid} className="hover:bg-gray-50/50 transition-colors group">
              <td className="px-8 py-4">
                <div className="flex items-center space-x-4">
                  <img src={u.profileImage || 'https://via.placeholder.com/40'} className="w-10 h-10 rounded-xl object-cover ring-2 ring-gray-100" alt="" />
                  <div>
                    <p className="font-bold text-gray-900">{u.name || 'Anonymous'}</p>
                    <p className="text-[10px] font-bold text-gray-400 uppercase">UID: {u.uid.slice(0, 8)}...</p>
                  </div>
                </div>
              </td>
              <td className="px-8 py-4">
                <p className="text-sm font-bold text-gray-700">{u.phoneNumber}</p>
                <p className="text-xs text-gray-400">{u.email || 'No email'}</p>
              </td>
              <td className="px-8 py-4 text-xs font-bold text-gray-500">
                {new Date(u.createdAt).toLocaleDateString()}
              </td>
              <td className="px-8 py-4">
                <span className={`px-3 py-1 rounded-full text-[10px] font-black uppercase border ${
                  u.isBlocked ? 'bg-red-50 text-red-600 border-red-100' : 'bg-emerald-50 text-emerald-600 border-emerald-100'
                }`}>
                  {u.isBlocked ? 'Blocked' : 'Active'}
                </span>
              </td>
              <td className="px-8 py-4 text-right">
                <button
                  onClick={() => onToggleBlock(u.uid)}
                  className={`px-4 py-2 rounded-lg text-xs font-black uppercase transition-all ${
                    u.isBlocked ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-100' : 'bg-white text-red-600 border border-red-200 hover:bg-red-50'
                  }`}
                >
                  {u.isBlocked ? 'Unblock' : 'Block'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
);

const PendingProvidersView = ({ providers, onApprove }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-6 border-b border-gray-100 bg-gray-50/30 font-black text-gray-800 uppercase tracking-tight">Verification Requests</div>
    <div className="divide-y divide-gray-100">
      {providers.length === 0 ? (
        <div className="p-12 text-center text-gray-400 font-bold italic">No pending requests</div>
      ) : (
        providers.map(p => (
          <div key={p.uid} className="p-6 flex items-center justify-between hover:bg-gray-50/50 transition-all">
            <div className="flex items-center space-x-6">
              <img src={p.profileImage} className="w-16 h-16 rounded-2xl object-cover ring-4 ring-gray-50" />
              <div>
                <h4 className="font-black text-gray-900 text-lg">{p.name}</h4>
                <div className="flex items-center space-x-4 mt-1">
                  <span className="px-2 py-0.5 bg-indigo-50 text-indigo-600 text-[10px] font-black rounded uppercase">{p.profession}</span>
                  <span className="text-xs font-bold text-gray-400">Aadhaar: {p.aadhaarNumber}</span>
                </div>
                <div className="mt-3 flex space-x-3">
                    <button className="text-[10px] font-black text-indigo-600 uppercase border-b-2 border-indigo-100 pb-0.5">View ID Card</button>
                    <button className="text-[10px] font-black text-indigo-600 uppercase border-b-2 border-indigo-100 pb-0.5">View Selfie</button>
                </div>
              </div>
            </div>
            <div className="flex space-x-3">
              <button onClick={() => onApprove(p.uid)} className="px-6 py-2.5 bg-emerald-600 text-white font-black text-xs uppercase rounded-xl shadow-lg shadow-emerald-100 hover:bg-emerald-700 transition-all">Approve Partner</button>
              <button className="px-6 py-2.5 bg-white text-red-600 border border-red-100 font-black text-xs uppercase rounded-xl hover:bg-red-50 transition-all">Reject</button>
            </div>
          </div>
        ))
      )}
    </div>
  </div>
);

const WithdrawalsView = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-6 border-b border-gray-100 bg-gray-50/30 font-black text-gray-800 uppercase tracking-tight">Payout Requests</div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead className="bg-gray-50/50 text-gray-400 text-[10px] font-black uppercase tracking-widest">
          <tr>
            <th className="px-8 py-4">Recipient</th>
            <th className="px-8 py-4">Amount</th>
            <th className="px-8 py-4">Bank Account</th>
            <th className="px-8 py-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {withdrawals.map(w => (
            <tr key={w._id} className="hover:bg-gray-50/50 transition-colors">
              <td className="px-8 py-6 font-bold text-gray-900">{w.providerName}</td>
              <td className="px-8 py-6 text-indigo-600 font-black text-xl italic">₹{w.amount}</td>
              <td className="px-8 py-6">
                <div className="text-xs font-bold text-gray-500 uppercase leading-relaxed">
                  <p className="text-gray-900">{w.holderName}</p>
                  <p>A/C: {w.accountNumber}</p>
                  <p>IFSC: {w.ifscCode}</p>
                </div>
              </td>
              <td className="px-8 py-6 text-right space-x-2">
                <button onClick={() => onHandle(w._id, 'approved')} className="px-5 py-2.5 bg-indigo-600 text-white font-black text-xs uppercase rounded-xl shadow-lg shadow-indigo-100">Confirm Payment</button>
                <button onClick={() => onHandle(w._id, 'rejected')} className="px-5 py-2.5 bg-gray-100 text-gray-500 font-black text-xs uppercase rounded-xl hover:bg-gray-200">Decline</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
);

const JobsView = ({ jobs, title }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-6 border-b border-gray-100 bg-gray-50/30 flex justify-between items-center">
        <h3 className="font-black text-gray-800 uppercase tracking-tight">{title}</h3>
        <button className="p-2 bg-white rounded-lg border border-gray-200 text-gray-400 hover:text-indigo-600 transition-colors">
            <Filter size={16} />
        </button>
    </div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead className="bg-gray-50/50 text-gray-400 text-[10px] font-black uppercase tracking-widest">
          <tr>
            <th className="px-8 py-4">Job Info</th>
            <th className="px-8 py-4">Customer</th>
            <th className="px-8 py-4">Partner</th>
            <th className="px-8 py-4">Status</th>
            <th className="px-8 py-4 text-right">Earnings</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {jobs.map(j => (
            <tr key={j._id} className="hover:bg-gray-50/50 transition-colors">
              <td className="px-8 py-4">
                <p className="font-bold text-gray-900">{j.serviceName}</p>
                <p className="text-[10px] font-bold text-gray-400">DATE: {new Date(j.createdAt).toLocaleString()}</p>
              </td>
              <td className="px-8 py-4 font-bold text-gray-700">{j.customerName}</td>
              <td className="px-8 py-4 font-bold text-gray-700">{j.providerName}</td>
              <td className="px-8 py-4">
                <span className={`px-2 py-0.5 rounded-md text-[10px] font-black uppercase ${
                  j.status === 'done' ? 'bg-emerald-50 text-emerald-600' :
                  j.status === 'pending' ? 'bg-gray-50 text-gray-500' :
                  'bg-blue-50 text-blue-600'
                }`}>
                  {j.status.replace('_', ' ')}
                </span>
              </td>
              <td className="px-8 py-4 text-right font-black text-gray-900">₹{j.totalAmount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
);

const CategoriesView = ({ categories, refresh }) => {
    const [name, setName] = useState('');
    const [icon, setIcon] = useState('');
    const handleAdd = async (e) => {
        e.preventDefault();
        try { await adminApi.addCategory({ name, icon }); setName(''); setIcon(''); refresh(); } catch(e) { alert("Failed"); }
    };
    const handleDelete = async (id) => {
        if(!confirm("Delete?")) return;
        try { await adminApi.deleteCategory(id); refresh(); } catch(e) { alert("Failed"); }
    };
    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 h-fit">
                <h3 className="font-black text-gray-800 mb-6 uppercase tracking-tight">Create Category</h3>
                <form onSubmit={handleAdd} className="space-y-4">
                    <input value={name} onChange={e=>setName(e.target.value)} required className="w-full px-4 py-3 bg-gray-50 border border-gray-100 rounded-xl outline-none focus:border-indigo-500 font-bold text-sm" placeholder="Name" />
                    <input value={icon} onChange={e=>setIcon(e.target.value)} required className="w-full px-4 py-3 bg-gray-50 border border-gray-100 rounded-xl outline-none focus:border-indigo-500 font-bold text-sm" placeholder="Icon URL" />
                    <button className="w-full py-4 bg-indigo-600 text-white font-black uppercase text-xs rounded-xl shadow-lg shadow-indigo-100">Save Category</button>
                </form>
            </div>
            <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-gray-100 p-8 grid grid-cols-2 md:grid-cols-4 gap-4">
                {categories.map(c => (
                    <div key={c._id} className="p-6 bg-gray-50 rounded-2xl flex flex-col items-center relative group">
                        <img src={c.icon} className="w-10 h-10 object-contain mb-3" />
                        <span className="font-black text-gray-800 text-xs uppercase tracking-tighter">{c.name}</span>
                        <button onClick={()=>handleDelete(c._id)} className="absolute top-2 right-2 p-1 bg-white rounded-full text-red-500 opacity-0 group-hover:opacity-100 transition-all shadow-sm"><XCircle size={14}/></button>
                    </div>
                ))}
            </div>
        </div>
    );
};

const OffersView = ({ offers, refresh }) => (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {offers.map(o => (
            <div key={o._id} className="bg-white p-6 rounded-3xl border-2 border-dashed border-indigo-100 relative overflow-hidden">
                <div className="absolute top-0 right-0 px-4 py-1 bg-indigo-600 text-white font-black text-xs rounded-bl-2xl uppercase italic">{o.code}</div>
                <h4 className="text-3xl font-black text-indigo-600 mb-1">{o.discount}% OFF</h4>
                <p className="font-bold text-gray-900">{o.title}</p>
                <p className="text-xs text-gray-400 mt-2 italic">Valid till: {new Date(o.expiryDate).toLocaleDateString()}</p>
                <div className="mt-6 flex justify-end space-x-2">
                    <button className="p-2 bg-gray-50 text-gray-400 rounded-lg"><Settings size={14}/></button>
                    <button className="p-2 bg-red-50 text-red-400 rounded-lg"><XCircle size={14}/></button>
                </div>
            </div>
        ))}
        <button className="bg-gray-50 border-4 border-dashed border-gray-100 rounded-3xl p-6 flex flex-col items-center justify-center text-gray-300 hover:text-indigo-400 hover:border-indigo-100 transition-all">
            <Tag size={32} />
            <span className="font-black text-xs uppercase mt-2">New Coupon</span>
        </button>
    </div>
);

const BannersView = ({ banners, refresh }) => (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {banners.map(b => (
            <div key={b._id} className="bg-white rounded-2xl border border-gray-100 overflow-hidden shadow-sm group">
                <img src={b.image} className="h-48 w-full object-cover group-hover:scale-105 transition-transform duration-700" />
                <div className="p-6">
                    <h4 className="font-black text-gray-900 text-lg uppercase tracking-tight">{b.title}</h4>
                    <p className="text-gray-500 font-medium text-sm mt-1">{b.subtitle}</p>
                </div>
            </div>
        ))}
    </div>
);

const SettingsView = ({ settings, refresh }) => {
    const [comm, setComm] = useState(settings.commission_percentage || 15);
    const handleSave = async () => { try { await adminApi.updateSetting('commission_percentage', comm); alert("Saved"); refresh(); } catch(e) { alert("Error"); } };
    return (
        <div className="max-w-xl bg-white p-10 rounded-3xl shadow-sm border border-gray-100">
            <h3 className="text-2xl font-black text-gray-900 mb-8 uppercase tracking-tighter">App Logic Settings</h3>
            <div className="space-y-10">
                <div className="flex items-center justify-between">
                    <div>
                        <p className="font-black text-gray-800 uppercase text-xs tracking-widest">Commission (%)</p>
                        <p className="text-sm text-gray-400 mt-1">Global platform cut from each job.</p>
                    </div>
                    <div className="flex items-center space-x-3">
                        <input type="number" value={comm} onChange={e=>setComm(e.target.value)} className="w-16 h-12 text-center bg-gray-50 border border-gray-100 rounded-xl font-black text-indigo-600 outline-none focus:border-indigo-500" />
                        <span className="font-black text-gray-300 text-2xl italic">%</span>
                    </div>
                </div>
                <button onClick={handleSave} className="w-full py-5 bg-slate-900 text-white font-black uppercase text-xs tracking-[3px] rounded-2xl shadow-xl shadow-slate-200 hover:bg-slate-800 transition-all">Update Platform Rules</button>
            </div>
        </div>
    );
};

const SupportView = ({ chats }) => (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex h-[600px]">
        <div className="w-80 border-r border-gray-100 flex flex-col">
            <div className="p-6 border-b border-gray-100 bg-gray-50/30 font-black text-xs uppercase tracking-widest">Active Tickets</div>
            <div className="flex-1 overflow-y-auto divide-y divide-gray-50">
                {chats.map(chat => (
                    <div key={chat.userUid} className="p-5 hover:bg-indigo-50/50 cursor-pointer transition-all">
                        <div className="flex items-center justify-between">
                            <span className="font-black text-gray-900 text-sm truncate">{chat.userName}</span>
                            <span className="text-[9px] font-bold text-gray-400 uppercase">{new Date(chat.lastTimestamp).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}</span>
                        </div>
                        <p className="text-xs text-gray-400 truncate mt-1 italic">"{chat.lastMessage}"</p>
                    </div>
                ))}
            </div>
        </div>
        <div className="flex-1 bg-gray-50 flex items-center justify-center text-center p-12">
            <div className="max-w-xs">
                <div className="w-16 h-16 bg-white rounded-2xl shadow-sm flex items-center justify-center mx-auto mb-6"><MessageSquare className="text-gray-200" size={32}/></div>
                <h4 className="font-black text-gray-800 uppercase tracking-tight">Support Desk</h4>
                <p className="text-sm text-gray-500 mt-2 font-medium">Select a ticket to view conversation history and respond to the user.</p>
            </div>
        </div>
    </div>
);

const ReportsView = ({ reports }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
    {reports.map(r => (
      <div key={r._id} className="bg-white p-6 rounded-2xl border border-red-50 shadow-sm relative overflow-hidden">
        <div className="absolute top-0 right-0 px-3 py-1 bg-red-600 text-white text-[10px] font-black uppercase rounded-bl-xl">{r.status}</div>
        <h4 className="font-black text-gray-900 mb-1">{r.reason}</h4>
        <p className="text-xs text-gray-500 leading-relaxed">{r.description}</p>
        <div className="mt-6 flex items-center justify-between pt-4 border-t border-gray-50">
          <div className="flex -space-x-2">
            {r.evidenceUrls?.map((url, i) => (
              <img key={i} src={url} className="w-8 h-8 rounded-lg border-2 border-white object-cover shadow-sm hover:scale-110 transition-all cursor-pointer" />
            ))}
          </div>
          <button className="text-[10px] font-black text-indigo-600 uppercase tracking-widest hover:underline">Mark Resolved</button>
        </div>
      </div>
    ))}
  </div>
);
