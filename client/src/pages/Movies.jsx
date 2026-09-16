import React from "react";
import { Search, SlidersHorizontal, X } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { getMovies } from "../lib/api";
import MovieCard from "../components/MovieCard";
import Loading from "../components/Loading";

export default function Movies(){
  const [params,setParams]=useSearchParams();
  const [movies,setMovies]=React.useState([]);
  const [loading,setLoading]=React.useState(true);
  const initial=params.get("search")||"";
  const [query,setQuery]=React.useState(initial);
  const [genre,setGenre]=React.useState("All");

  React.useEffect(()=>{getMovies().then(d=>setMovies(Array.isArray(d)?d:[])).catch(console.error).finally(()=>setLoading(false))},[]);
  const genres=["All",...new Set(movies.flatMap(m=>{
    if(m.genre) return String(m.genre).split(",").map(x=>x.trim());
    if(Array.isArray(m.genres)) return m.genres.map(x=>x.name);
    return [];
  }))];
  const filtered=movies.filter(m=>{
    const title=(m.title||"").toLowerCase().includes(query.toLowerCase());
    const g=genre==="All"||String(m.genre||"").toLowerCase().includes(genre.toLowerCase())||m.genres?.some(x=>x.name===genre);
    return title&&g;
  });
  return <main className="min-h-screen px-5 pb-20 pt-32 sm:px-8 lg:px-12">
    <div className="mx-auto max-w-[1440px]">
      <div className="animate-cine-rise">
        <p className="text-xs font-semibold uppercase tracking-[.25em] text-cyan-300">Discover</p>
        <h1 className="mt-2 text-4xl font-semibold sm:text-5xl">Movies</h1>
        <p className="mt-3 max-w-xl text-sm leading-6 text-white/40">Find your next favourite story and book the seats that feel just right.</p>
      </div>
      <div className="mt-8 flex flex-col gap-3 lg:flex-row">
        <div className="relative flex-1">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30"/>
          <input value={query} onChange={e=>{setQuery(e.target.value);setParams(e.target.value?{search:e.target.value}:{})}} placeholder="Search by movie title..." className="h-12 w-full rounded-xl border border-white/10 bg-white/[.04] pl-11 pr-11 outline-none transition focus:border-cyan-300/40 focus:bg-white/[.06]"/>
          {query&&<button onClick={()=>{setQuery("");setParams({})}} className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1.5 text-white/40 hover:text-white"><X size={16}/></button>}
        </div>
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          <SlidersHorizontal size={17} className="shrink-0 text-white/35"/>
          {genres.map(g=><button key={g} onClick={()=>setGenre(g)} className={`whitespace-nowrap rounded-full border px-4 py-2.5 text-xs transition ${genre===g?"border-cyan-300/30 bg-cyan-300/10 text-cyan-200":"border-white/10 bg-white/[.03] text-white/45 hover:text-white"}`}>{g}</button>)}
        </div>
      </div>
      {loading?<Loading label="Loading movies..."/>:<>
        <div className="mb-5 mt-10 text-sm text-white/35">{filtered.length} movie{filtered.length!==1?"s":""} found</div>
        {filtered.length?<div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6">{filtered.map((m,i)=><MovieCard key={m.id??m._id} movie={m} index={i}/>)}</div>:<div className="rounded-3xl border border-white/10 bg-white/[.03] p-16 text-center text-white/40">No movies match your search.</div>}
      </>}
    </div>
  </main>
}
