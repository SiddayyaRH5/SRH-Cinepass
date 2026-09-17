import React from "react";
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Building2,
  PlusCircle,
  CalendarPlus,
  BarChart3,
  Ticket,
  Settings,
} from "lucide-react";

const items = [
  {
    to: "/admin",
    label: "Dashboard",
    icon: LayoutDashboard,
    end: true,
  },
  {
    to: "/admin/add-theatre",
    label: "Add Theatre",
    icon: PlusCircle,
  },
  {
    to: "/admin/manage-theatres",
    label: "Theatres",
    icon: Building2,
  },
  {
    to: "/admin/add-shows",
    label: "Add Shows",
    icon: CalendarPlus,
  },
  {
    to: "/admin/list-shows",
    label: "Shows",
    icon: BarChart3,
  },
  {
    to: "/admin/list-bookings",
    label: "Bookings",
    icon: Ticket,
  },
];

export default function AdminSidebar() {
  return (
    <aside
      className="
        hidden
        w-60
        shrink-0
        border-r
        border-white/10
        bg-[#070a0f]
        p-4
        md:flex
        md:flex-col
      "
    >
      {/* ================================
          MANAGEMENT
      ================================= */}

      <p
        className="
          px-3
          pb-4
          pt-2
          text-[10px]
          font-semibold
          uppercase
          tracking-[.25em]
          text-white/25
        "
      >
        Management
      </p>

      {/* ================================
          NAVIGATION
      ================================= */}

      <nav className="grid gap-1">
        {items.map(
          ({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `
                group
                flex
                items-center
                gap-3
                rounded-xl
                px-3
                py-3
                text-sm
                transition-all
                duration-200

                ${
                  isActive
                    ? `
                      bg-cyan-300/10
                      text-cyan-200
                      shadow-[inset_2px_0_0_rgba(103,232,249,.9)]
                    `
                    : `
                      text-white/45
                      hover:bg-white/[.04]
                      hover:text-white
                    `
                }
              `
              }
            >
              <span
                className="
                  grid
                  h-8
                  w-8
                  shrink-0
                  place-items-center
                  rounded-lg
                  transition
                  group-hover:bg-white/[.04]
                "
              >
                <Icon
                  size={17}
                  strokeWidth={1.8}
                />
              </span>

              <span>{label}</span>
            </NavLink>
          )
        )}
      </nav>

      {/* ================================
          SYSTEM INFORMATION
      ================================= */}

      <div className="mt-auto pt-8">
        <div
          className="
            rounded-xl
            border
            border-white/[.06]
            bg-white/[.025]
            p-4
          "
        >
          <div className="flex items-center gap-2">
            <span
              className="
                h-2
                w-2
                rounded-full
                bg-green-400
                shadow-[0_0_10px_rgba(74,222,128,.5)]
              "
            />

            <span
              className="
                text-[10px]
                font-semibold
                uppercase
                tracking-[.18em]
                text-white/35
              "
            >
              System Online
            </span>
          </div>

          <p
            className="
              mt-3
              text-xs
              leading-5
              text-white/25
            "
          >
            CinePass administration
            system is ready.
          </p>
        </div>
      </div>
    </aside>
  );
}