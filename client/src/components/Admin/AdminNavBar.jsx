import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { LogOut, ShieldCheck, Ticket } from "lucide-react";
import { assets } from "../../assets/assets";
import { useAuth } from "../../context/AuthContext";

export default function AdminNavBar(){
 const {user,logout}=useAuth();const navigate=useNavigate();
 return <header className="flex h-16 items-center justify-between border-b border-white/10 bg-[#080b10] px-5 sm:px-8"><Link to="/admin" className="flex items-center gap-3"><img src={assets.logo} className="h-9 w-auto" alt="SRH CinePass"/><span className="hidden rounded-full bg-cyan-300/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-cyan-300 sm:block">Admin</span></Link><div className="flex items-center gap-4"><div className="hidden text-right sm:block"><p className="text-sm font-medium">{user?.name||"Administrator"}</p><p className="text-[10px] uppercase tracking-wider text-cyan-300/70"><ShieldCheck className="mr-1 inline" size={11}/>{user?.role||"ADMIN"}</p></div><button onClick={()=>{logout();navigate("/")}} className="grid h-9 w-9 place-items-center rounded-full border border-white/10 text-white/50 hover:bg-red-500/10 hover:text-red-300"><LogOut size={17}/></button></div></header>
}
