'use client';
import React, { useState, useEffect } from 'react';
import {
  Users, Briefcase, CheckCircle, XCircle, BarChart3,
  Settings, LogOut, MessageSquare, Image as ImageIcon,
  Tag, CreditCard, Bell, ChevronRight, Search
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
    chats: []
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
        case 'providers':
          const p = await adminApi.getPendingProviders();
          setData(prev => ({ ...prev, pendingProviders: p.data }));
          break;
        case 'withdrawals':
          const w = await adminApi.getPendingWithdrawals();
          setData(prev => ({ ...prev, withdrawals: w.data }));
          break;
        case 'jobs':
          const j = await adminApi.getActiveJobs();
          setData(prev => ({ ...prev, activeJobs: j.data }));
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
      fetchTabData('providers');
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

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 text-white flex flex-col shadow-xl">
        <div className="p-6 border-b border-slate-800">
          <h1 className="text-xl font-bold bg-gradient-to-r from-indigo-400 to-cyan-400 bg-clip-text text-transparent">
            Madadwala Admin
          </h1>
        </div>

        <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
          <NavItem icon={<BarChart3 size={20}/>} label="Dashboard" active={activeTab==='analytics'} onClick={()=>setActiveTab('analytics')} />
          <NavItem icon={<Users size={20}/>} label="Verifications" active={activeTab==='providers'} onClick={()=>setActiveTab('providers')} count={data.pendingProviders.length} />
          <NavItem icon={<CreditCard size={20}/>} label="Withdrawals" active={activeTab==='withdrawals'} onClick={()=>setActiveTab('withdrawals')} count={data.withdrawals.length} />
          <NavItem icon={<Briefcase size={20}/>} label="Live Jobs" active={activeTab==='jobs'} onClick={()=>setActiveTab('jobs')} />
          <NavItem icon={<MessageSquare size={20}/>} label="Support" active={activeTab==='support'} onClick={()=>setActiveTab('support')} />
          <div className="pt-4 pb-2 px-2 text-xs font-semibold text-slate-500 uppercase tracking-wider">Content</div>
          <NavItem icon={<Tag size={20}/>} label="Categories" active={activeTab==='categories'} onClick={()=>setActiveTab('categories')} />
          <NavItem icon={<ImageIcon size={20}/>} label="Banners" active={activeTab==='banners'} onClick={()=>setActiveTab('banners')} />
          <NavItem icon={<Bell size={20}/>} label="Offers" active={activeTab==='offers'} onClick={()=>setActiveTab('offers')} />
          <div className="pt-4 pb-2 px-2 text-xs font-semibold text-slate-500 uppercase tracking-wider">System</div>
          <NavItem icon={<Settings size={20}/>} label="Settings" active={activeTab==='settings'} onClick={()=>setActiveTab('settings')} />
        </nav>

        <div className="p-4 border-t border-slate-800">
          <button className="flex items-center space-x-3 px-4 py-3 w-full rounded-lg hover:bg-red-500/10 text-red-400 transition-colors">
            <LogOut size={20} />
            <span className="font-medium">Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8 shrink-0">
          <h2 className="text-lg font-semibold text-gray-800 capitalize">
            {activeTab === 'analytics' ? 'Dashboard Overview' : activeTab}
          </h2>

          <div className="flex items-center space-x-6">
            <div className="relative group">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-indigo-500 transition-colors" size={18} />
              <input
                type="text"
                placeholder="Search..."
                className="pl-10 pr-4 py-2 bg-gray-100 border-transparent focus:bg-white focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 rounded-full text-sm transition-all outline-none w-64"
              />
            </div>
            <button className="p-2 text-gray-500 hover:bg-gray-100 rounded-full transition-colors relative">
              <Bell size={20} />
              <span className="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
            </button>
            <div className="flex items-center space-x-3 pl-4 border-l border-gray-200">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-medium text-gray-900">Admin User</p>
                <p className="text-xs text-gray-500">Super Admin</p>
              </div>
              <div className="w-10 h-10 rounded-full bg-indigo-600 flex items-center justify-center text-white font-bold shadow-lg shadow-indigo-200">
                AD
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-8">
          {loading ? (
            <div className="flex flex-col items-center justify-center h-full space-y-4">
              <div className="w-12 h-12 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
              <p className="text-gray-500 font-medium">Loading data...</p>
            </div>
          ) : (
            <div className="max-w-7xl mx-auto space-y-8 animate-in fade-in duration-500">
              {activeTab === 'analytics' && <AnalyticsView data={data.analytics} />}
              {activeTab === 'providers' && <ProvidersView providers={data.pendingProviders} onApprove={handleApproveProvider} />}
              {activeTab === 'withdrawals' && <WithdrawalsView withdrawals={data.withdrawals} onHandle={handleWithdrawal} />}
              {activeTab === 'jobs' && <JobsView jobs={data.activeJobs} />}
              {activeTab === 'categories' && <CategoriesView categories={data.categories} refresh={()=>fetchTabData('categories')} />}
              {activeTab === 'offers' && <OffersView offers={data.offers} refresh={()=>fetchTabData('offers')} />}
              {activeTab === 'banners' && <BannersView banners={data.banners} refresh={()=>fetchTabData('banners')} />}
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
    <button
      onClick={onClick}
      className={`w-full flex items-center justify-between px-4 py-3 rounded-xl transition-all duration-200 ${
        active
          ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-900/50'
          : 'text-slate-400 hover:bg-slate-800 hover:text-white'
      }`}
    >
      <div className="flex items-center space-x-3">
        <span className={active ? 'text-white' : 'text-slate-500'}>{icon}</span>
        <span className="font-medium">{label}</span>
      </div>
      {count > 0 && (
        <span className={`px-2 py-0.5 text-xs font-bold rounded-full ${
          active ? 'bg-white text-indigo-600' : 'bg-indigo-600 text-white'
        }`}>
          {count}
        </span>
      )}
    </button>
  );
}

// Sub-views
const AnalyticsView = ({ data }) => {
  if (!data) return null;
  return (
    <div className="space-y-8">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Total Customers" value={data.totalUsers} change="+12%" icon={<Users className="text-blue-600" />} />
        <StatCard title="Verified Partners" value={data.totalProviders} change="+5%" icon={<CheckCircle className="text-emerald-600" />} />
        <StatCard title="Total Bookings" value={data.totalBookings} change="+18%" icon={<Briefcase className="text-purple-600" />} />
        <StatCard title="Total Revenue" value={`₹${data.totalRevenue?.toLocaleString()}`} change="+24%" icon={<CreditCard className="text-amber-600" />} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
          <h3 className="text-lg font-bold text-gray-800 mb-6 flex items-center space-x-2">
            <BarChart3 size={20} className="text-indigo-600" />
            <span>Partner Category Distribution</span>
          </h3>
          <div className="space-y-6">
            {data.categories?.map((cat, i) => (
              <div key={i} className="group">
                <div className="flex justify-between mb-2">
                  <span className="text-sm font-semibold text-gray-700 group-hover:text-indigo-600 transition-colors">{cat.name}</span>
                  <span className="text-sm font-bold text-indigo-600">{Math.round(cat.ratio * 100)}%</span>
                </div>
                <div className="w-full bg-gray-100 rounded-full h-3 overflow-hidden">
                  <div
                    className="bg-indigo-500 h-full rounded-full transition-all duration-1000 ease-out"
                    style={{ width: `${cat.ratio * 100}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-center items-center text-center">
            <div className="w-20 h-20 bg-indigo-50 rounded-full flex items-center justify-center mb-4">
                <Briefcase size={32} className="text-indigo-600" />
            </div>
            <h3 className="text-xl font-bold text-gray-800">Growth Analysis</h3>
            <p className="text-gray-500 max-w-sm mt-2">Charts and detailed growth metrics will appear here in the production version.</p>
            <button className="mt-6 px-6 py-2 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 transition-colors shadow-lg shadow-indigo-200">
                View Full Report
            </button>
        </div>
      </div>
    </div>
  );
};

const StatCard = ({ title, value, change, icon }) => (
  <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
    <div className="flex justify-between items-start mb-4">
      <div className="p-3 bg-gray-50 rounded-xl">{icon}</div>
      <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2 py-1 rounded-lg">{change}</span>
    </div>
    <div>
      <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">{title}</p>
      <p className="text-3xl font-bold text-gray-900 mt-1">{value}</p>
    </div>
  </div>
);

const ProvidersView = ({ providers, onApprove }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-6 border-b border-gray-100 bg-gray-50/50 flex justify-between items-center">
        <h3 className="font-bold text-gray-800">Pending Verification Requests</h3>
        <span className="bg-indigo-100 text-indigo-700 text-xs font-bold px-3 py-1 rounded-full">{providers.length} Pending</span>
    </div>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead>
          <tr className="bg-gray-50/50 text-gray-400 text-xs font-bold uppercase tracking-wider">
            <th className="px-8 py-4">Partner Details</th>
            <th className="px-8 py-4">Profession</th>
            <th className="px-8 py-4">Verification Info</th>
            <th className="px-8 py-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {providers.length === 0 ? (
            <tr><td colSpan="4" className="px-8 py-12 text-center text-gray-400 font-medium italic">No pending verifications at the moment.</td></tr>
          ) : (
            providers.map((p) => (
              <tr key={p.uid} className="hover:bg-indigo-50/30 transition-colors">
                <td className="px-8 py-5">
                  <div className="flex items-center space-x-4">
                    <img src={p.profileImage || 'https://via.placeholder.com/48'} className="w-12 h-12 rounded-xl object-cover ring-2 ring-gray-100" alt="" />
                    <div>
                      <p className="font-bold text-gray-900">{p.name}</p>
                      <p className="text-sm text-gray-500">{p.phoneNumber}</p>
                    </div>
                  </div>
                </td>
                <td className="px-8 py-5">
                  <span className="px-3 py-1 bg-blue-50 text-blue-700 rounded-lg text-sm font-semibold border border-blue-100">
                    {p.profession || p.category}
                  </span>
                </td>
                <td className="px-8 py-5">
                  <div className="text-sm">
                    <p className="font-medium text-gray-700">Aadhaar: <span className="text-gray-900 font-bold">{p.aadhaarNumber}</span></p>
                    <button className="text-indigo-600 font-bold hover:underline mt-1 text-xs uppercase tracking-tighter">View Document</button>
                  </div>
                </td>
                <td className="px-8 py-5 text-right space-x-3">
                  <button onClick={() => onApprove(p.uid)} className="px-4 py-2 bg-emerald-600 text-white text-sm font-bold rounded-lg hover:bg-emerald-700 transition-colors shadow-lg shadow-emerald-100">Approve</button>
                  <button className="px-4 py-2 bg-white text-red-600 border border-red-200 text-sm font-bold rounded-lg hover:bg-red-50 transition-colors">Reject</button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

const WithdrawalsView = ({ withdrawals, onHandle }) => (
  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
    <div className="p-6 border-b border-gray-100 bg-gray-50/50 flex justify-between items-center">
        <h3 className="font-bold text-gray-800">Pending Payout Requests</h3>
        <button className="text-indigo-600 text-sm font-bold hover:underline">View History</button>
    </div>
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50 text-gray-400 font-bold uppercase text-[10px] tracking-widest">
            <th className="px-8 py-4">Recipient</th>
            <th className="px-8 py-4">Amount</th>
            <th className="px-8 py-4">Bank Details</th>
            <th className="px-8 py-4">Requested Date</th>
            <th className="px-8 py-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {withdrawals.length === 0 ? (
            <tr><td colSpan="5" className="px-8 py-12 text-center text-gray-400 font-medium">Clear! No pending payouts.</td></tr>
          ) : (
            withdrawals.map((w) => (
              <tr key={w._id} className="hover:bg-gray-50/50 transition-colors">
                <td className="px-8 py-6 font-bold text-gray-900">{w.providerName}</td>
                <td className="px-8 py-6"><span className="text-indigo-600 font-black text-lg italic">₹{w.amount}</span></td>
                <td className="px-8 py-6">
                  <div className="p-3 bg-gray-50 rounded-xl border border-gray-100 space-y-1">
                    <p className="text-xs font-bold text-gray-500 uppercase tracking-tighter">A/C: {w.accountNumber}</p>
                    <p className="text-xs font-bold text-gray-500 uppercase tracking-tighter">IFSC: {w.ifscCode}</p>
                    <p className="text-xs font-bold text-indigo-600 truncate">{w.holderName}</p>
                  </div>
                </td>
                <td className="px-8 py-6 text-gray-500 font-medium">{new Date(w.createdAt).toLocaleString()}</td>
                <td className="px-8 py-6 text-right space-x-2">
                  <button onClick={() => onHandle(w._id, 'approved')} className="px-5 py-2 bg-indigo-600 text-white font-bold rounded-lg hover:bg-indigo-700 shadow-lg shadow-indigo-100">Pay Now</button>
                  <button onClick={() => onHandle(w._id, 'rejected')} className="px-5 py-2 bg-gray-100 text-gray-600 font-bold rounded-lg hover:bg-gray-200">Decline</button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);

const JobsView = ({ jobs }) => (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {jobs.length === 0 ? (
            <div className="col-span-full py-20 text-center bg-white rounded-2xl border-2 border-dashed border-gray-200 text-gray-400 font-bold">No active jobs being performed right now.</div>
        ) : (
            jobs.map(j => (
                <div key={j._id} className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 relative overflow-hidden group hover:border-indigo-200 transition-all">
                    <div className="absolute top-0 right-0 p-3">
                        <span className={`text-[10px] font-black uppercase px-2 py-1 rounded-bl-xl ${
                            j.status === 'in_progress' ? 'bg-blue-600 text-white' : 'bg-amber-500 text-white'
                        }`}>
                            {j.status.replace('_', ' ')}
                        </span>
                    </div>
                    <div className="mb-4">
                        <h4 className="font-black text-gray-900 line-clamp-1">{j.serviceName}</h4>
                        <p className="text-xs font-bold text-gray-400 mt-1 uppercase tracking-tighter">ID: #{j._id.slice(-8)}</p>
                    </div>
                    <div className="space-y-4">
                        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase">Customer</p>
                                <p className="text-sm font-bold text-gray-800">{j.customerName}</p>
                            </div>
                            <ChevronRight size={16} className="text-gray-300" />
                            <div className="text-right">
                                <p className="text-[10px] font-bold text-gray-400 uppercase">Partner</p>
                                <p className="text-sm font-bold text-gray-800">{j.providerName}</p>
                            </div>
                        </div>
                        <div className="flex justify-between items-center pt-2">
                            <p className="text-xl font-black text-indigo-600">₹{j.totalAmount}</p>
                            <button className="text-xs font-bold text-indigo-500 hover:underline">Track on Map</button>
                        </div>
                    </div>
                </div>
            ))
        )}
    </div>
);

const CategoriesView = ({ categories, refresh }) => {
    const [name, setName] = useState('');
    const [icon, setIcon] = useState('');

    const handleAdd = async (e) => {
        e.preventDefault();
        try {
            await adminApi.addCategory({ name, icon });
            setName(''); setIcon('');
            refresh();
        } catch(e) { alert("Failed to add category"); }
    };

    const handleDelete = async (id) => {
        if(!confirm("Delete category?")) return;
        try {
            await adminApi.deleteCategory(id);
            refresh();
        } catch(e) { alert("Delete failed"); }
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 h-fit">
                <h3 className="font-bold text-gray-800 mb-6 flex items-center space-x-2">
                    <Tag size={20} className="text-indigo-600" />
                    <span>Create New Category</span>
                </h3>
                <form onSubmit={handleAdd} className="space-y-4">
                    <div>
                        <label className="text-xs font-bold text-gray-400 uppercase block mb-1">Category Name</label>
                        <input value={name} onChange={e=>setName(e.target.value)} required className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-indigo-500 font-medium" placeholder="e.g. Electrician" />
                    </div>
                    <div>
                        <label className="text-xs font-bold text-gray-400 uppercase block mb-1">Icon URL / Name</label>
                        <input value={icon} onChange={e=>setIcon(e.target.value)} required className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg outline-none focus:border-indigo-500 font-medium" placeholder="https://..." />
                    </div>
                    <button className="w-full py-3 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 transition-all shadow-lg shadow-indigo-100">Add Category</button>
                </form>
            </div>

            <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="p-6 border-b border-gray-100 bg-gray-50/50 font-bold text-gray-800">Existing Categories</div>
                <div className="p-6 grid grid-cols-2 sm:grid-cols-3 gap-4">
                    {categories.map(c => (
                        <div key={c._id} className="p-4 bg-gray-50 rounded-2xl border border-gray-100 flex flex-col items-center group relative">
                            <div className="w-12 h-12 bg-white rounded-xl shadow-sm mb-3 flex items-center justify-center overflow-hidden">
                                <img src={c.icon || 'https://via.placeholder.com/40'} className="w-8 h-8 object-contain" alt="" />
                            </div>
                            <span className="font-bold text-gray-800 text-sm text-center">{c.name}</span>
                            <button onClick={()=>handleDelete(c._id)} className="absolute -top-2 -right-2 bg-white text-red-500 p-1.5 rounded-full shadow-md opacity-0 group-hover:opacity-100 transition-opacity border border-red-50 hover:bg-red-50">
                                <XCircle size={16} />
                            </button>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

const OffersView = ({ offers, refresh }) => {
    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h3 className="text-xl font-bold text-gray-800">Promotions & Offers</h3>
                <button className="px-6 py-2 bg-indigo-600 text-white font-bold rounded-lg shadow-lg shadow-indigo-100">+ Create Offer</button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {offers.map(o => (
                    <div key={o._id} className="bg-white p-6 rounded-2xl border-2 border-indigo-50 shadow-sm hover:border-indigo-200 transition-all cursor-pointer">
                        <div className="flex justify-between items-start mb-4">
                            <div className="px-3 py-1 bg-indigo-600 text-white text-xs font-black rounded-lg">{o.code}</div>
                            <span className="text-emerald-600 font-black text-2xl">{o.discount}% OFF</span>
                        </div>
                        <h4 className="font-bold text-gray-900 text-lg">{o.title}</h4>
                        <p className="text-gray-500 text-sm mt-1 mb-4">{o.description}</p>
                        <div className="flex justify-between items-center text-xs font-bold text-gray-400">
                            <span>Expires: {new Date(o.expiryDate).toLocaleDateString()}</span>
                            <div className="flex space-x-2">
                                <button className="text-indigo-600">Edit</button>
                                <button className="text-red-500">Delete</button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

const BannersView = ({ banners, refresh }) => (
    <div className="space-y-6">
        <div className="flex justify-between items-center">
            <h3 className="text-xl font-bold text-gray-800">App Home Banners</h3>
            <button className="px-6 py-2 bg-indigo-600 text-white font-bold rounded-lg shadow-lg shadow-indigo-100">+ Add Banner</button>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {banners.map(b => (
                <div key={b._id} className="bg-white rounded-2xl border border-gray-100 overflow-hidden shadow-sm group">
                    <div className="h-48 bg-gray-100 relative">
                        <img src={b.image} className="w-full h-full object-cover" alt="" />
                        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center space-x-4">
                            <button className="bg-white text-gray-900 px-4 py-2 rounded-lg font-bold text-sm">Edit</button>
                            <button className="bg-red-600 text-white px-4 py-2 rounded-lg font-bold text-sm">Remove</button>
                        </div>
                    </div>
                    <div className="p-4">
                        <h4 className="font-bold text-gray-900">{b.title}</h4>
                        <p className="text-sm text-gray-500">{b.subtitle}</p>
                    </div>
                </div>
            ))}
        </div>
    </div>
);

const SettingsView = ({ settings, refresh }) => {
    const [comm, setComm] = useState(settings.commission_percentage || 15);

    const handleSave = async () => {
        try {
            await adminApi.updateSetting('commission_percentage', comm);
            alert("Settings saved!");
            refresh();
        } catch(e) { alert("Save failed"); }
    };

    return (
        <div className="max-w-2xl bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="p-8 space-y-8">
                <h3 className="text-xl font-bold text-gray-900 border-b pb-4">Revenue & Commission</h3>
                <div className="flex items-center justify-between">
                    <div>
                        <p className="font-bold text-gray-800">Platform Commission (%)</p>
                        <p className="text-sm text-gray-500">Percentage deducted from partner earnings for every job.</p>
                    </div>
                    <div className="flex items-center space-x-4">
                        <input
                            type="number"
                            value={comm}
                            onChange={e=>setComm(e.target.value)}
                            className="w-20 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-center font-bold text-indigo-600 focus:border-indigo-500 outline-none"
                        />
                        <span className="text-xl font-bold text-gray-300">%</span>
                    </div>
                </div>

                <div className="bg-indigo-50 p-6 rounded-2xl border border-indigo-100 flex items-start space-x-4">
                    <div className="p-2 bg-white rounded-xl text-indigo-600 shadow-sm"><Settings size={20}/></div>
                    <div>
                        <p className="text-sm font-bold text-indigo-900 uppercase">Impact Analysis</p>
                        <p className="text-xs text-indigo-600 mt-1">Increasing commission will increase revenue but may decrease partner satisfaction. Partners currently retain {100-comm}% of their earnings.</p>
                    </div>
                </div>

                <button onClick={handleSave} className="w-full py-4 bg-indigo-600 text-white font-black rounded-xl hover:bg-indigo-700 transition-all shadow-xl shadow-indigo-100">
                    Apply & Save Global Settings
                </button>
            </div>
        </div>
    );
};

const SupportView = ({ chats }) => (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex flex-col h-[600px]">
        <div className="p-6 border-b border-gray-100 bg-gray-50/50 font-bold text-gray-800 flex justify-between">
            <span>Customer Support Tickets</span>
            <span className="text-indigo-600">{chats.length} Active Chats</span>
        </div>
        <div className="flex-1 flex min-h-0">
            <div className="w-80 border-r border-gray-100 overflow-y-auto">
                {chats.map(chat => (
                    <div key={chat.userUid} className="p-4 border-b border-gray-50 hover:bg-indigo-50/30 cursor-pointer transition-colors relative">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center font-bold text-gray-500">{chat.userName[0]}</div>
                            <div className="flex-1 min-w-0">
                                <p className="font-bold text-gray-900 truncate">{chat.userName}</p>
                                <p className="text-xs text-gray-500 truncate italic">"{chat.lastMessage}"</p>
                            </div>
                            {chat.unreadCount > 0 && <div className="w-5 h-5 bg-indigo-600 rounded-full flex items-center justify-center text-[10px] text-white font-bold">{chat.unreadCount}</div>}
                        </div>
                        <p className="text-[10px] text-gray-400 mt-2 text-right">{new Date(chat.lastTimestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</p>
                    </div>
                ))}
            </div>
            <div className="flex-1 bg-gray-50 flex flex-col items-center justify-center text-center p-12">
                <div className="w-16 h-16 bg-white rounded-2xl shadow-sm flex items-center justify-center mb-4 text-gray-300">
                    <MessageSquare size={32} />
                </div>
                <h4 className="font-bold text-gray-800">Select a conversation</h4>
                <p className="text-sm text-gray-500 mt-1 max-w-xs">Click on a user from the left panel to view message history and respond.</p>
            </div>
        </div>
    </div>
);
