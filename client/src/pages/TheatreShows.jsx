import React from "react";
import { useNavigate,useParams } from "react-router-dom";
import { ArrowLeft, Clock3, Film, MapPin, ChevronRight } from "lucide-react";
import { getShowsByTheatreAndDate,getTheatre } from "../lib/api";
import { useLocationContext } from "../lib/LocationContext";
import { formatDate,formatTime,money } from "../lib/formatters";
import Loading from "../components/Loading";

export default function TheatreShows(){
 const {theatreId}=useParams();const navigate=useNavigate();const {selectedLocation}=useLocationContext();const today=new Date().toISOString().split("T")[0];
 const [theatre,setTheatre]=React.useState(null),[shows,setShows]=React.useState([]),[loading,setLoading]=React.useState(true);
 React.useEffect(()=>{Promise.all([getTheatre(theatreId),getShowsByTheatreAndDate(theatreId,today)]).then(([t,s])=>{setTheatre(t);setShows(Array.isArray(s)?s:[])}).catch(console.error).finally(()=>setLoading(false))},[theatreId,today]);
 if(loading)return <main className="pt-28"><Loading label="Loading today's shows..."/></main>;
 return <main className="min-h-screen px-5 pb-20 pt-32 sm:px-8 lg:px-12"><div className="mx-auto max-w-[1100px]"><button onClick={()=>navigate("/theaters")} className="mb-8 flex items-center gap-2 text-sm text-white/45 hover:text-white"><ArrowLeft size={17}/>Back to theatres</button>
 <div className="mb-10"><p className="flex items-center gap-2 text-xs uppercase tracking-[.22em] text-cyan-300"><MapPin size={14}/>{theatre?.location?.city||selectedLocation?.city}</p><h1 className="mt-2 text-4xl font-semibold">{theatre?.name||"Theatre"}</h1><p className="mt-2 text-sm text-white/40">{formatDate(today)} · Today's shows</p></div>
 {shows.length?<div className="space-y-4">{shows.map((s,i)=><button key={s.id} onClick={()=>navigate(`/show/${s.id}`)} className="group flex w-full items-center justify-between gap-5 rounded-2xl border border-white/10 bg-white/[.03] p-5 text-left transition hover:-translate-y-0.5 hover:border-cyan-300/30 hover:bg-white/[.05] animate-cine-rise" style={{animationDelay:`${i*70}ms`}}><div className="flex items-center gap-4"><span className="grid h-12 w-12 place-items-center rounded-xl bg-cyan-300/10 text-cyan-300"><Film/></span><div><h2 className="font-semibold">{s.movie?.title||"Movie"}</h2><p className="mt-1 text-xs text-white/35">{s.movie?.language||"Cinema"} · {money(s.ticketPrice)} / ticket</p></div></div><div className="flex items-center gap-3"><span className="rounded-xl border border-cyan-300/15 bg-cyan-300/10 px-4 py-2.5 text-sm font-medium text-cyan-200"><Clock3 className="mr-2 inline" size={15}/>{formatTime(s.showTime)}</span><ChevronRight size={18} className="text-white/20 group-hover:text-cyan-300"/></div></button>)}</div>:<div className="rounded-3xl border border-white/10 bg-white/[.03] p-14 text-center text-white/40">No shows are scheduled for today.</div>}
 </div></main>
}
