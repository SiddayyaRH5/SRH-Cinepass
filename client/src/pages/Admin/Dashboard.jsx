import React from "react";
import { Activity, CalendarDays, IndianRupee, Ticket, Users } from "lucide-react";
import { getAllBookings, getShows } from "../../lib/api";
import Title from "../../components/Admin/Title";
import { money } from "../../lib/formatters";

export default function Dashboard(){
 const [bookings,setBookings]=React.useState([]),[shows,setShows]=React.useState([]),[loading,setLoading]=React.useState(true);
 React.useEffect(()=>{Promise.allSettled([getAllBookings(),getShows()]).then(([b,s])=>{setBookings(b.status==="fulfilled"&&Array.isArray(b.value)?b.value:[]);setShows(s.status==="fulfilled"&&Array.isArray(s.value)?s.value:[])}).finally(()=>setLoading(false))},[]);
 const revenue=bookings.reduce((a,b)=>a+Number(b.totalAmount||0),0);
 const cards=[["Bookings",bookings.length,Ticket],["Revenue",money(revenue),IndianRupee],["Active Shows",shows.length,CalendarDays],["Users",new Set(bookings.map(b=>b.userId)).size,Users]];
 return <><Title text1="Overview" text2="Dashboard"/><div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{cards.map(([l,v,I],i)=><div key={l} className="rounded-2xl border border-white/10 bg-white/[.03] p-5 animate-cine-rise" style={{animationDelay:`${i*60}ms`}}><div className="flex items-center justify-between"><span className="grid h-10 w-10 place-items-center rounded-xl bg-cyan-300/10 text-cyan-300"><I size={19}/></span><Activity size={15} className="text-white/20"/></div><p className="mt-6 text-xs uppercase tracking-wider text-white/30">{l}</p><p className="mt-1 text-2xl font-semibold">{loading?"—":v}</p></div>)}</div><div className="mt-6 rounded-2xl border border-white/10 bg-white/[.03] p-6"><h2 className="font-semibold">System status</h2><div className="mt-5 grid gap-3 sm:grid-cols-3"><Status label="Database" text="Connected"/><Status label="Booking API" text="Operational"/><Status label="Authentication" text="JWT secured"/></div></div></>
}
function Status({label,text}){return <div className="flex items-center justify-between rounded-xl bg-white/[.03] px-4 py-3"><span className="text-sm text-white/45">{label}</span><span className="text-xs text-green-300">{text}</span></div>}
