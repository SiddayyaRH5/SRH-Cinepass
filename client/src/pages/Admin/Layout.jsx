import React from "react";
import { Outlet } from "react-router-dom";
import AdminNavBar from "../../components/Admin/AdminNavBar";
import AdminSidebar from "../../components/Admin/AdminSidebar";
export default function Layout(){return <div className="min-h-screen bg-[#05070b] text-white"><AdminNavBar/><div className="flex min-h-[calc(100vh-64px)]"><AdminSidebar/><main className="min-w-0 flex-1 overflow-y-auto p-5 sm:p-8 lg:p-10"><Outlet/></main></div></div>}
