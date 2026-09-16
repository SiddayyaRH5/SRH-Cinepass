import React from "react";
import { CalendarDays, Clock3, Film } from "lucide-react";
import { getShows } from "../../lib/api";
import { formatDate,formatTime,money } from "../../lib/formatters";
import Title from "../../components/Admin/Title";

export default function ListShows(){
 const [shows,setShows]=React.useState([]),[loading,setLoading]=React.useState(true);
 React.useEffect(()=>{getShows().then(d=>setShows(Array.isArray(d)?d:[])).catch(console.error).finally(()=>setLoading(false))},[]);
 return <><Title text1="Management" text2="Shows"/><div className="mt-8 overflow-hidden rounded-2xl border border-white/10 bg-white/[.03]">{loading?<div className="p-10 text-center text-white/40">Loading shows...</div>:shows.length?<div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="border-b border-white/10 bg-white/[.03] text-xs uppercase tracking-wider text-white/30"><tr><th className="px-5 py-4">Movie</th><th className="px-5 py-4">Theatre</th><th className="px-5 py-4">Date</th><th className="px-5 py-4">Time</th><th className="px-5 py-4">Price</th></tr></thead><tbody>{shows.map(s=><tr key={s.id} className="border-b border-white/[.06] hover:bg-white/[.025]"><td className="px-5 py-4"><div className="flex items-center gap-3"><span className="grid h-9 w-9 place-items-center rounded-lg bg-cyan-300/10 text-cyan-300"><Film size={16}/></span>{s.movie?.title||"Movie"}</div></td><td className="px-5 py-4 text-white/60">{s.theatre?.name||"—"}</td><td className="px-5 py-4 text-white/55"><CalendarDays className="mr-1 inline" size={14}/>{formatDate(s.showDate)}</td><td className="px-5 py-4 text-white/55"><Clock3 className="mr-1 inline" size={14}/>{formatTime(s.showTime)}</td><td className="px-5 py-4 text-cyan-200">{money(s.ticketPrice)}</td></tr>)}</tbody></table></div>:<div className="p-12 text-center text-white/40">No shows found.</div>}</div></>
}
