import React from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import {
  Building2,
  ChevronDown,
  Clapperboard,
  Heart,
  House,
  LogOut,
  Menu,
  Search,
  Ticket,
  User,
  X,
} from "lucide-react";
import { assets } from "../assets/assets";
import LocationSelector from "./LocationSelector";
import { useAuth } from "../context/AuthContext";

const links = [
  {
    to: "/",
    label: "Home",
    icon: House,
  },
  {
    to: "/movies",
    label: "Movies",
    icon: Clapperboard,
  },
  {
    to: "/theaters",
    label: "Theatres",
    icon: Building2,
  },
  {
    to: "/favorite",
    label: "Favorites",
    icon: Heart,
  },
];

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [mobileOpen, setMobileOpen] = React.useState(false);
  const [profileOpen, setProfileOpen] = React.useState(false);
  const [searchOpen, setSearchOpen] = React.useState(false);
  const [search, setSearch] = React.useState("");

  React.useEffect(() => {
    setMobileOpen(false);
    setProfileOpen(false);
    setSearchOpen(false);
  }, [location.pathname]);

  const submitSearch = (e) => {
    e.preventDefault();

    const value = search.trim();

    if (value) {
      navigate(`/movies?search=${encodeURIComponent(value)}`);
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/");
    setMobileOpen(false);
    setProfileOpen(false);
  };

  return (
    <header
      className="
        fixed
        inset-x-0
        top-0
        z-[100]
        px-3
        py-3
        sm:px-5
        lg:px-8
      "
    >
      {/* =====================================================
          MAIN NAVBAR
      ===================================================== */}
      <div
        className="
          mx-auto
          flex
          h-[68px]
          max-w-[1440px]
          items-center
          gap-3
          rounded-2xl
          border
          border-white/[.09]
          bg-[#070a0f]/80
          px-3
          shadow-2xl
          backdrop-blur-xl
          sm:px-5
        "
      >
        {/* ===================================================
            LOGO
        =================================================== */}
        <Link
          to="/"
          className="
            shrink-0
            transition
            hover:scale-[1.02]
          "
        >
          <img
            src={assets.logo}
            alt="SRH CinePass"
            className="h-9 w-auto sm:h-11"
          />
        </Link>

        {/* ===================================================
            DESKTOP NAVIGATION
        =================================================== */}
        <nav
          className="
            mx-auto
            hidden
            items-center
            gap-1
            lg:flex
          "
        >
          {links.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `
                  relative
                  rounded-xl
                  px-4
                  py-2.5
                  text-sm
                  transition

                  ${
                    isActive
                      ? "bg-white/[.07] text-cyan-300"
                      : "text-white/65 hover:bg-white/[.05] hover:text-white"
                  }
                `
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        {/* ===================================================
            RIGHT SIDE
        =================================================== */}
        <div className="ml-auto flex items-center gap-2">

          {/* =================================================
              SEARCH
          ================================================= */}
          <div
            className={`
              flex
              items-center
              overflow-hidden
              transition-all
              duration-300

              ${
                searchOpen
                  ? "w-48 sm:w-64"
                  : "w-10"
              }
            `}
          >
            {searchOpen && (
              <form
                onSubmit={submitSearch}
                className="
                  relative
                  w-full
                  animate-cine-scale
                "
              >
                <Search
                  size={16}
                  className="
                    absolute
                    left-3
                    top-1/2
                    -translate-y-1/2
                    text-white/35
                  "
                />

                <input
                  autoFocus
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search movies..."
                  className="
                    h-10
                    w-full
                    rounded-full
                    border
                    border-white/10
                    bg-white/[.06]
                    pl-9
                    pr-9
                    text-sm
                    text-white
                    outline-none
                    placeholder:text-white/30
                    focus:border-cyan-400/50
                  "
                />

                <button
                  type="button"
                  onClick={() => {
                    setSearchOpen(false);
                    setSearch("");
                  }}
                  className="
                    absolute
                    right-2
                    top-1/2
                    -translate-y-1/2
                    rounded-full
                    p-1
                    text-white/40
                    hover:text-white
                  "
                >
                  <X size={15} />
                </button>
              </form>
            )}

            {!searchOpen && (
              <button
                type="button"
                onClick={() => setSearchOpen(true)}
                className="
                  grid
                  h-10
                  w-10
                  place-items-center
                  rounded-full
                  text-white/75
                  hover:bg-white/[.06]
                  hover:text-cyan-300
                "
              >
                <Search size={21} />
              </button>
            )}
          </div>

          {/* =================================================
              DESKTOP LOCATION
          ================================================= */}
          <div className="hidden sm:block">
            <LocationSelector />
          </div>

          {/* =================================================
              PROFILE
          ================================================= */}
          {isAuthenticated ? (
            <div className="relative">

              <button
                type="button"
                onClick={() =>
                  setProfileOpen((v) => !v)
                }
                className="
                  flex
                  items-center
                  gap-2
                  rounded-full
                  border
                  border-white/10
                  bg-white/[.05]
                  p-1.5
                  pr-2.5
                  transition
                  hover:border-cyan-400/30
                "
              >
                <span
                  className="
                    grid
                    h-9
                    w-9
                    place-items-center
                    rounded-full
                    bg-gradient-to-br
                    from-cyan-300
                    to-blue-600
                    text-black
                  "
                >
                  <User size={18} />
                </span>

                <span
                  className="
                    hidden
                    max-w-24
                    truncate
                    text-sm
                    font-medium
                    sm:block
                  "
                >
                  {user?.name || "Account"}
                </span>

                <ChevronDown
                  size={14}
                  className={`
                    text-white/45
                    transition
                    ${
                      profileOpen
                        ? "rotate-180"
                        : ""
                    }
                  `}
                />
              </button>

              {/* =================================================
                  PROFILE DROPDOWN
              ================================================= */}
              {profileOpen && (
                <div
                  className="
                    absolute
                    right-0
                    top-[calc(100%+10px)]
                    w-64
                    overflow-hidden
                    rounded-2xl
                    border
                    border-white/10
                    bg-[#0d1119]/95
                    shadow-2xl
                    backdrop-blur-xl
                    animate-cine-scale
                  "
                >
                  <div
                    className="
                      border-b
                      border-white/10
                      p-4
                    "
                  >
                    <p className="truncate font-semibold">
                      {user?.name}
                    </p>

                    <p className="truncate text-xs text-white/40">
                      {user?.email}
                    </p>

                    <span
                      className="
                        mt-2
                        inline-block
                        rounded-full
                        bg-cyan-400/10
                        px-2.5
                        py-1
                        text-[10px]
                        font-semibold
                        uppercase
                        tracking-wider
                        text-cyan-300
                      "
                    >
                      {user?.role || "USER"}
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={() =>
                      navigate("/My-Bookings")
                    }
                    className="
                      flex
                      w-full
                      items-center
                      gap-3
                      px-4
                      py-3.5
                      text-sm
                      text-white/75
                      hover:bg-white/[.06]
                      hover:text-white
                    "
                  >
                    <Ticket size={17} />
                    My Bookings
                  </button>

                  {user?.role === "ADMIN" && (
                    <button
                      type="button"
                      onClick={() =>
                        navigate("/admin")
                      }
                      className="
                        flex
                        w-full
                        items-center
                        gap-3
                        px-4
                        py-3.5
                        text-sm
                        text-white/75
                        hover:bg-white/[.06]
                        hover:text-white
                      "
                    >
                      <User size={17} />
                      Admin Dashboard
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={handleLogout}
                    className="
                      flex
                      w-full
                      items-center
                      gap-3
                      border-t
                      border-white/10
                      px-4
                      py-3.5
                      text-sm
                      text-red-300
                      hover:bg-red-500/10
                    "
                  >
                    <LogOut size={17} />
                    Logout
                  </button>
                </div>
              )}
            </div>
          ) : (
            /* =================================================
               DESKTOP LOGIN / SIGNUP
            ================================================= */
            <div
              className="
                hidden
                items-center
                gap-2
                sm:flex
              "
            >
              <Link
                to="/login"
                className="
                  rounded-full
                  border
                  border-white/15
                  px-4
                  py-2
                  text-sm
                  text-white/80
                  transition
                  hover:border-cyan-400/40
                  hover:text-white
                "
              >
                Login
              </Link>

              <Link
                to="/signup"
                className="
                  rounded-full
                  bg-cyan-400
                  px-4
                  py-2
                  text-sm
                  font-semibold
                  text-slate-950
                  transition
                  hover:bg-cyan-300
                  hover:shadow-[0_0_25px_rgba(18,207,232,.2)]
                "
              >
                Sign Up
              </Link>
            </div>
          )}

          {/* =================================================
              MOBILE MENU BUTTON
          ================================================= */}
          <button
            type="button"
            onClick={() =>
              setMobileOpen((v) => !v)
            }
            className="
              grid
              h-10
              w-10
              place-items-center
              rounded-full
              border
              border-white/10
              text-white
              transition
              hover:border-cyan-400/30
              hover:text-cyan-300
              lg:hidden
            "
          >
            {mobileOpen ? (
              <X size={21} />
            ) : (
              <Menu size={21} />
            )}
          </button>
        </div>
      </div>

      {/* =====================================================
          MOBILE MENU
      ===================================================== */}
      {mobileOpen && (
        <div
          className="
            mx-auto
            mt-2
            max-w-[1440px]
            rounded-2xl
            border
            border-white/10
            bg-[#090c12]/95
            p-4
            shadow-2xl
            backdrop-blur-xl
            animate-cine-scale

            max-md:px-4
            max-md:py-4
          "
        >

          {/* =================================================
              MOBILE LOCATION
          ================================================= */}
          <div
            className="
              mb-4
              flex
              justify-center
              sm:hidden
            "
          >
            <LocationSelector />
          </div>

          {/* =================================================
              MOBILE NAVIGATION
          ================================================= */}
          <div className="grid gap-1">

            {links.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  `
                    flex
                    w-full
                    items-center
                    justify-center
                    gap-3
                    rounded-xl
                    px-4
                    py-3.5
                    text-center
                    text-[15px]
                    font-medium
                    transition

                    ${
                      isActive
                        ? "bg-white/[.07] text-cyan-300"
                        : "text-white/70 hover:bg-white/[.06] hover:text-white"
                    }
                  `
                }
              >
                <Icon
                  size={18}
                  strokeWidth={1.8}
                  className="shrink-0"
                />

                <span>{label}</span>
              </NavLink>
            ))}

          </div>

          {/* =================================================
              MOBILE LOGIN / SIGNUP
          ================================================= */}
          {!isAuthenticated && (
            <div
              className="
                mt-4
                grid
                grid-cols-2
                gap-2
                border-t
                border-white/10
                pt-4
              "
            >
              <Link
                to="/login"
                className="
                  flex
                  items-center
                  justify-center
                  rounded-xl
                  border
                  border-white/10
                  py-3
                  text-center
                  text-sm
                  transition
                  hover:bg-white/[.05]
                "
              >
                Login
              </Link>

              <Link
                to="/signup"
                className="
                  flex
                  items-center
                  justify-center
                  rounded-xl
                  bg-cyan-400
                  py-3
                  text-center
                  text-sm
                  font-semibold
                  text-black
                  transition
                  hover:bg-cyan-300
                "
              >
                Sign Up
              </Link>
            </div>
          )}

          {/* =================================================
              MOBILE LOGOUT
          ================================================= */}
          {isAuthenticated && (
            <button
              type="button"
              onClick={handleLogout}
              className="
                mt-4
                flex
                w-full
                items-center
                justify-center
                gap-2
                rounded-xl
                border
                border-red-400/10
                bg-red-500/10
                py-3.5
                text-center
                text-sm
                text-red-300
                transition
                hover:bg-red-500/15
              "
            >
              <LogOut size={17} />

              Logout
            </button>
          )}
        </div>
      )}
    </header>
  );
}