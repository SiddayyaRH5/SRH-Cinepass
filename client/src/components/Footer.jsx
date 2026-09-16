import React from "react";
import { Link } from "react-router-dom";
import { ArrowUpRight, Mail, Phone, Ticket } from "lucide-react";
import { assets } from "../assets/assets";

export default function Footer() {
  return (
    <footer className="mt-24 border-t border-white/[.07] bg-[#040609]">
      <div className="mx-auto max-w-[1440px] px-5 py-14 sm:px-8 lg:px-12">
        <div className="grid gap-12 md:grid-cols-[1.6fr_1fr_1fr]">
          <div>
            <img src={assets.logo} alt="SRH CinePass" className="h-12 w-auto"/>
            <p className="mt-5 max-w-lg text-sm leading-7 text-white/45">
              Your cinema, simplified. Discover movies, choose your theatre, pick the perfect seats and carry your ticket wherever the story takes you.
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <span className="rounded-full border border-white/10 bg-white/[.03] px-3 py-1.5 text-xs text-white/45">Secure booking</span>
              <span className="rounded-full border border-white/10 bg-white/[.03] px-3 py-1.5 text-xs text-white/45">Live seat status</span>
              <span className="rounded-full border border-white/10 bg-white/[.03] px-3 py-1.5 text-xs text-white/45">Instant confirmation</span>
            </div>
          </div>
          <div>
            <p className="mb-5 text-xs font-semibold uppercase tracking-[.2em] text-cyan-300">Explore</p>
            <div className="grid gap-3 text-sm text-white/55">
              <Link className="hover:text-white" to="/">Home</Link>
              <Link className="hover:text-white" to="/movies">Movies</Link>
              <Link className="hover:text-white" to="/theaters">Theatres</Link>
              <Link className="hover:text-white" to="/favorite">Favorites</Link>
              <Link className="hover:text-white" to="/My-Bookings">My Bookings</Link>
            </div>
          </div>
          <div>
            <p className="mb-5 text-xs font-semibold uppercase tracking-[.2em] text-cyan-300">Need help?</p>
            <div className="grid gap-4 text-sm text-white/55">
              <a href="mailto:Siddusrh@gmail.com" className="flex items-center gap-3 hover:text-white"><Mail size={16}/>Siddusrh@gmail.com</a>
              <a href="tel:+918088285205" className="flex items-center gap-3 hover:text-white"><Phone size={16}/>+91 80882 85205</a>
              <span className="flex items-center gap-3"><Ticket size={16}/>Movie tickets, made simple.</span>
            </div>
          </div>
        </div>
        <div className="mt-12 flex flex-col justify-between gap-3 border-t border-white/[.07] pt-6 text-xs text-white/30 sm:flex-row">
          <span>© {new Date().getFullYear()} SRH CinePass. All rights reserved.</span>
          <span className="flex items-center gap-1">Built for the big screen <ArrowUpRight size={13}/></span>
        </div>
      </div>
    </footer>
  );
}
