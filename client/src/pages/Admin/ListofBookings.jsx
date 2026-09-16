import React from "react";
import { CalendarDays, Ticket, UserRound } from "lucide-react";
import { getAllBookings } from "../../lib/api";
import { formatDate,formatTime,money } from "../../lib/formatters";
import Title from "../../components/Admin/Title";

export default function ListofBookings(){
 const [bookings,setBookings]=React.useState([]),[loading,setLoading]=React.useState(true);
 React.useEffect(()=>{getAllBookings().then(d=>setBookings(Array.isArray(d)?d:[])).catch(console.error).finally(()=>setLoading(false))},[]);
 return <><Title text1="Management" text2="Bookings"/><div className="mt-8 overflow-hidden rounded-2xl border border-white/10 bg-white/[.03]">{loading?<div className="p-10 text-center text-white/40">Loading bookings...</div>:bookings.length?<div className="overflow-x-auto"><table className="w-full min-w-[820px] text-left text-sm"><thead className="border-b border-white/10 bg-white/[.03] text-xs uppercase tracking-wider text-white/30"><tr><th className="px-5 py-4">Customer</th><th className="px-5 py-4">Movie</th><th className="px-5 py-4">Show</th><th className="px-5 py-4">Seats</th><th className="px-5 py-4">Amount</th></tr></thead><tbody>{bookings.map(b=><tr key={b.id} className="border-b border-white/[.06]"><td className="px-5 py-4"><div className="flex items-center gap-2"><UserRound size={16} className="text-cyan-300"/><span>{b.userEmail||b.userId||"Customer"}</span></div></td><td className="px-5 py-4 text-white/70">{b.show?.movie?.title||"—"}</td><td className="px-5 py-4 text-white/50"><CalendarDays className="mr-1 inline" size={14}/>{formatDate(b.show?.showDate)} · {formatTime(b.show?.showTime)}</td><td className="px-5 py-4 text-cyan-200"><Ticket className="mr-1 inline" size={14}/>{(b.seatNumbers||[]).join(", ")}</td><td className="px-5 py-4 font-semibold">{money(b.totalAmount)}</td></tr>)}</tbody></table></div>:<div className="p-12 text-center text-white/40">No bookings found.</div>}</div></>
}
