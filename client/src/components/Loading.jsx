import React from "react";
import { Loader2 } from "lucide-react";
export default function Loading({ label="Loading..." }) {
  return <div className="flex min-h-[50vh] items-center justify-center"><div className="flex items-center gap-3 rounded-full border border-white/10 bg-white/[.04] px-5 py-3 text-sm text-white/55"><Loader2 className="animate-spin text-cyan-300" size={18}/>{label}</div></div>;
}
