import React from "react";
import { NavLink } from "react-router-dom";
import { BarChart3, CalendarPlus, LayoutDashboard, Ticket } from "lucide-react";

const items=[["/admin","Dashboard",LayoutDashboard],["/admin/add-shows","Add Shows",CalendarPlus],["/admin/list-shows","Shows",BarChart3],["/admin/list-bookings","Bookings",Ticket]];
export default function AdminSidebar(){return <aside className="hidden w-60 shrink-0 border-r border-white/10 bg-[#070a0f] p-4 md:block"><p className="px-3 pb-4 pt-2 text-[10px] font-semibold uppercase tracking-[.25em] text-white/25">Management</p><div className="grid gap-1">{items.map(([to,label,Icon])=><NavLink end={to==="/admin"} key={to} to={to} className={({isActive})=>`flex items-center gap-3 rounded-xl px-3 py-3 text-sm transition ${isActive?"bg-cyan-300/10 text-cyan-200":"text-white/45 hover:bg-white/[.04] hover:text-white"}`}><Icon size={17}/>{label}</NavLink>)}</div></aside>}
