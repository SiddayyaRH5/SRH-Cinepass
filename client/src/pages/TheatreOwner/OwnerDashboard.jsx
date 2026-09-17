import React from "react";
import {
  BarChart3,
  Building2,
  CalendarDays,
  Clapperboard,
  IndianRupee,
  LayoutDashboard,
  Ticket,
  TrendingUp,
  Users,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";

export default function OwnerDashboard() {

  const { user } = useAuth();

  const stats = [
    {
      title: "Total Shows",
      value: "0",
      icon: Clapperboard,
      description: "Shows scheduled",
    },
    {
      title: "Total Bookings",
      value: "0",
      icon: Ticket,
      description: "Bookings received",
    },
    {
      title: "Tickets Sold",
      value: "0",
      icon: Users,
      description: "Tickets sold",
    },
    {
      title: "Revenue",
      value: "₹0",
      icon: IndianRupee,
      description: "Total revenue",
    },
  ];

  return (
    <div className="min-h-screen bg-[#080b12] text-white">

      {/* HEADER */}

      <div className="border-b border-white/10 bg-[#0d111a]">

        <div className="mx-auto max-w-7xl px-6 py-7">

          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">

            <div>

              <div className="mb-2 flex items-center gap-2 text-sm text-blue-400">
                <LayoutDashboard size={17} />
                Theatre Owner Dashboard
              </div>

              <h1 className="text-3xl font-bold tracking-tight">
                Welcome, {user?.name || "Theatre Owner"}
              </h1>

              <p className="mt-2 text-sm text-gray-400">
                Manage your theatre, shows, bookings and business performance.
              </p>

            </div>

            <div className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-3">

              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500/10">
                <Building2
                  size={20}
                  className="text-blue-400"
                />
              </div>

              <div>

                <p className="text-xs text-gray-500">
                  Account
                </p>

                <p className="text-sm font-semibold">
                  Theatre Owner
                </p>

              </div>

            </div>

          </div>

        </div>

      </div>


      {/* CONTENT */}

      <main className="mx-auto max-w-7xl px-6 py-8">

        {/* STAT CARDS */}

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">

          {stats.map((stat) => {

            const Icon = stat.icon;

            return (
              <div
                key={stat.title}
                className="group rounded-2xl border border-white/10 bg-[#0d111a] p-5 transition-all duration-300 hover:-translate-y-1 hover:border-blue-500/30 hover:bg-[#111722]"
              >

                <div className="flex items-start justify-between">

                  <div>

                    <p className="text-sm text-gray-400">
                      {stat.title}
                    </p>

                    <h2 className="mt-3 text-3xl font-bold">
                      {stat.value}
                    </h2>

                  </div>

                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-500/10">
                    <Icon
                      size={21}
                      className="text-blue-400"
                    />
                  </div>

                </div>

                <p className="mt-4 text-xs text-gray-500">
                  {stat.description}
                </p>

              </div>
            );

          })}

        </div>


        {/* QUICK ACTIONS */}

        <section className="mt-8">

          <div className="mb-5">

            <h2 className="text-xl font-semibold">
              Quick Actions
            </h2>

            <p className="mt-1 text-sm text-gray-500">
              Manage your theatre operations.
            </p>

          </div>


          <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">

            <ActionCard
              icon={Building2}
              title="My Theatre"
              description="View and manage your approved theatre."
            />

            <ActionCard
              icon={Clapperboard}
              title="Manage Shows"
              description="Create, update and remove movie shows."
            />

            <ActionCard
              icon={Ticket}
              title="Bookings"
              description="View bookings and ticket sales."
            />

            <ActionCard
              icon={BarChart3}
              title="Analytics"
              description="Track revenue, bookings and occupancy."
            />

            <ActionCard
              icon={TrendingUp}
              title="Business Performance"
              description="Understand your theatre performance."
            />

            <ActionCard
              icon={CalendarDays}
              title="Show Schedule"
              description="Manage your upcoming show schedule."
            />

          </div>

        </section>


        {/* EMPTY STATE */}

        <section className="mt-8 rounded-2xl border border-white/10 bg-[#0d111a] p-8">

          <div className="flex flex-col items-center justify-center py-10 text-center">

            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-500/10">

              <BarChart3
                size={28}
                className="text-blue-400"
              />

            </div>

            <h2 className="mt-5 text-xl font-semibold">
              Your business analytics will appear here
            </h2>

            <p className="mt-2 max-w-lg text-sm leading-6 text-gray-500">
              Once your theatre starts receiving bookings, this section
              will show revenue, ticket sales, occupancy, popular movies,
              preferred show times and other business insights.
            </p>

          </div>

        </section>

      </main>

    </div>
  );
}


/* ============================================================
   ACTION CARD
============================================================ */

function ActionCard({
  icon: Icon,
  title,
  description,
}) {

  return (
    <button
      type="button"
      className="group rounded-2xl border border-white/10 bg-[#0d111a] p-5 text-left transition-all duration-300 hover:-translate-y-1 hover:border-blue-500/30 hover:bg-[#111722]"
    >

      <div className="flex items-start gap-4">

        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-500/10">

          <Icon
            size={22}
            className="text-blue-400"
          />

        </div>

        <div>

          <h3 className="font-semibold">
            {title}
          </h3>

          <p className="mt-1 text-sm leading-5 text-gray-500">
            {description}
          </p>

        </div>

      </div>

    </button>
  );
}