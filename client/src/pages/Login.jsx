import React from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  Mail,
  Lock,
  Eye,
  EyeOff,
  MapPin,
  Search,
  Ticket,
  Sparkles,
} from "lucide-react";
import toast from "react-hot-toast";

import { apiFetch } from "../lib/api";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();

  const { login } = useAuth();

  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");

  const [showPassword, setShowPassword] = React.useState(false);

  const [loading, setLoading] = React.useState(false);
  const [googleLoading, setGoogleLoading] = React.useState(false);

  // ============================================================
  // LOGIN
  // ============================================================

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!email.trim()) {
      toast.error("Please enter your email");
      return;
    }

    if (!password) {
      toast.error("Please enter your password");
      return;
    }

    try {
      setLoading(true);

      const response = await apiFetch("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: email.trim(),
          password,
        }),
      });

      console.log("Login response:", response);

      if (!response) {
        throw new Error("Invalid server response");
      }

      if (!response.token) {
        throw new Error(
          response.message || "Login failed"
        );
      }

      login({
        token: response.token,
        id: response.id,
        name: response.name,
        email: response.email,
        role: response.role,
      });

      toast.success(
        `Welcome back, ${response.name || "User"}!`
      );

      const from = location.state?.from || "/";

      navigate(from, {
        replace: true,
      });
    } catch (error) {
      console.error("Login error:", error);

      toast.error(
        error?.message ||
          "Invalid email or password"
      );
    } finally {
      setLoading(false);
    }
  };

  // ============================================================
  // GOOGLE LOGIN
  // ============================================================

  const handleGoogleLogin = () => {
    try {
      setGoogleLoading(true);

      /*
       * Local development:
       * http://localhost:8080
       *
       * Production:
       * Replace this with your Render backend URL
       * or use VITE_API_URL from .env
       */

      const backendUrl =
        import.meta.env.VITE_API_URL ||
        "http://localhost:8080";

      window.location.href =
        `${backendUrl}/oauth2/authorization/google`;
    } catch (error) {
      console.error(
        "Google login error:",
        error
      );

      setGoogleLoading(false);

      toast.error(
        "Unable to continue with Google"
      );
    }
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-[#030507] text-white">

      {/* ======================================================
          BACKGROUND
      ====================================================== */}

      <div
        className="absolute inset-0 bg-cover bg-center"
        style={{
          backgroundImage:
            "linear-gradient(90deg, rgba(3,5,7,0.96) 0%, rgba(3,5,7,0.86) 38%, rgba(3,5,7,0.55) 65%, rgba(3,5,7,0.82) 100%), url('/images/cinema-bg.jpg')",
        }}
      />

      {/* Dark cinematic overlay */}

      <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_45%,rgba(34,211,238,0.08),transparent_32%)]" />

      <div className="relative z-10 min-h-screen">

        {/* ==================================================
            NAVBAR
        ================================================== */}

        <header className="px-2 pt-2 sm:px-3">

          <nav className="mx-auto flex h-[64px] max-w-[1560px] items-center justify-between rounded-2xl border border-white/[0.08] bg-[#080b0f]/90 px-4 shadow-2xl backdrop-blur-xl sm:px-7">

            {/* LOGO */}

            <Link
              to="/"
              className="flex items-center gap-3"
            >
              <div className="relative flex h-10 w-10 items-center justify-center overflow-hidden rounded-lg bg-gradient-to-br from-blue-600 to-cyan-400 shadow-lg">

                <Ticket
                  size={22}
                  strokeWidth={2.4}
                />

              </div>

              <div className="hidden leading-none sm:block">

                <div className="text-[13px] font-semibold tracking-wide text-white">
                  SRH CinePass
                </div>

                <div className="mt-1 text-[6px] tracking-[0.12em] text-white/55">
                  Movies • Theatre • Seats • Offers
                </div>

              </div>
            </Link>


            {/* DESKTOP NAVIGATION */}

            <div className="hidden items-center gap-10 lg:flex">

              <Link
                to="/"
                className="text-sm text-white/65 transition hover:text-white"
              >
                Home
              </Link>

              <Link
                to="/movies"
                className="text-sm text-white/65 transition hover:text-white"
              >
                Movies
              </Link>

              <Link
                to="/theatres"
                className="text-sm text-white/65 transition hover:text-white"
              >
                Theatres
              </Link>

              <Link
                to="/favorites"
                className="text-sm text-white/65 transition hover:text-white"
              >
                Favorites
              </Link>

            </div>


            {/* RIGHT NAV */}

            <div className="flex items-center gap-2 sm:gap-3">

              {/* SEARCH */}

              <button
                type="button"
                className="hidden p-2 text-white/70 transition hover:text-white sm:block"
              >
                <Search size={21} />
              </button>


              {/* LOCATION */}

              <button
                type="button"
                className="hidden items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-4 py-2.5 text-sm text-white/80 transition hover:bg-white/[0.08] md:flex"
              >

                <MapPin
                  size={18}
                  className="text-cyan-300"
                />

                <span>
                  Bengaluru
                </span>

                <span className="text-white/30">
                 ⌄
                </span>

              </button>


              {/* LOGIN */}

              <Link
                to="/login"
                className="rounded-full border border-white/10 bg-white/[0.04] px-4 py-2.5 text-sm font-medium text-white transition hover:bg-white/[0.08]"
              >
                Login
              </Link>


              {/* SIGN UP */}

              <Link
                to="/signup"
                className="rounded-full bg-cyan-400 px-5 py-2.5 text-sm font-semibold text-[#031015] transition hover:bg-cyan-300"
              >
                Sign Up
              </Link>

            </div>

          </nav>

        </header>


        {/* ==================================================
            MAIN CONTENT
        ================================================== */}

        <main className="mx-auto flex min-h-[calc(100vh-80px)] max-w-[1280px] items-center px-6 py-12 lg:px-8">

          <div className="grid w-full grid-cols-1 items-center gap-14 lg:grid-cols-[1fr_500px] xl:gap-24">


            {/* =================================================
                LEFT SIDE
            ================================================= */}

            <section className="hidden lg:block">

              {/* Logo / Brand */}

              <div className="mb-12 flex items-center gap-3">

                <div className="flex h-11 w-14 items-center justify-center rounded-lg bg-gradient-to-br from-blue-600 to-cyan-400">

                  <Ticket
                    size={25}
                    className="text-white"
                  />

                </div>

                <div>

                  <div className="text-base font-semibold">
                    SRH CinePass
                  </div>

                  <div className="text-[7px] tracking-[0.13em] text-white/50">
                    MOVIE TICKETS • SHOWS • OFFERS
                  </div>

                </div>

              </div>


              {/* Small Heading */}

              <div className="mb-6 flex items-center gap-3">

                <div className="h-px w-8 bg-cyan-300/60" />

                <span className="text-xs font-semibold uppercase tracking-[0.3em] text-cyan-300">
                  Cinema without friction
                </span>

              </div>


              {/* Main Heading */}

              <h1 className="max-w-[650px] text-5xl font-semibold leading-[1.08] tracking-[-0.035em] xl:text-6xl">

                Every seat has a story.

                <br />

                <span className="text-white/35">
                  Choose yours.
                </span>

              </h1>


              {/* Description */}

              <p className="mt-8 max-w-[550px] text-base leading-7 text-white/45">

                Discover shows, choose your theatre
                and reserve the perfect seats with a
                seamless CinePass experience.

              </p>


              {/* Feature Pills */}

              <div className="mt-10 flex flex-wrap gap-3">

                <div className="flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.035] px-4 py-2.5 text-xs text-white/45 backdrop-blur-md">

                  <Sparkles
                    size={14}
                    className="text-cyan-300"
                  />

                  Curated movies

                </div>


                <div className="flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.035] px-4 py-2.5 text-xs text-white/45 backdrop-blur-md">

                  <Lock
                    size={14}
                    className="text-cyan-300"
                  />

                  Secure JWT login

                </div>

              </div>

            </section>


            {/* =================================================
                LOGIN CARD
            ================================================= */}

            <section className="w-full">

              <div className="rounded-[28px] border border-white/[0.09] bg-[#0a0e13]/90 p-7 shadow-[0_25px_100px_rgba(0,0,0,0.55)] backdrop-blur-2xl sm:p-10">


                {/* CARD HEADER */}

                <div className="mb-8">

                  <div className="mb-4 text-[11px] font-semibold uppercase tracking-[0.3em] text-cyan-300">
                    Welcome back
                  </div>

                  <h2 className="text-3xl font-semibold leading-tight tracking-[-0.025em] sm:text-4xl">

                    Continue your movie
                    <br />

                    night.

                  </h2>

                  <p className="mt-4 max-w-[420px] text-sm leading-6 text-white/40">

                    Sign in to your CinePass account
                    and pick up where the story left off.

                  </p>

                </div>


                {/* =================================================
                    FORM
                ================================================= */}

                <form
                  onSubmit={handleSubmit}
                  className="space-y-5"
                >

                  {/* EMAIL */}

                  <div>

                    <label className="mb-2.5 flex items-center gap-2 text-xs font-medium text-white/50">

                      <Mail
                        size={15}
                        className="text-cyan-300"
                      />

                      Email address

                    </label>

                    <div className="relative">

                      <input
                        type="email"
                        value={email}
                        onChange={(e) =>
                          setEmail(e.target.value)
                        }
                        placeholder="you@example.com"
                        autoComplete="email"
                        className="h-14 w-full rounded-xl border border-white/10 bg-[#edf3ff] px-4 text-sm font-medium text-[#0b1018] outline-none transition placeholder:text-[#68758a] focus:border-cyan-300"
                      />

                    </div>

                  </div>


                  {/* PASSWORD */}

                  <div>

                    <label className="mb-2.5 flex items-center gap-2 text-xs font-medium text-white/50">

                      <Lock
                        size={15}
                        className="text-cyan-300"
                      />

                      Password

                    </label>

                    <div className="relative">

                      <input
                        type={
                          showPassword
                            ? "text"
                            : "password"
                        }
                        value={password}
                        onChange={(e) =>
                          setPassword(e.target.value)
                        }
                        placeholder="Enter your password"
                        autoComplete="current-password"
                        className="h-14 w-full rounded-xl border border-white/10 bg-[#edf3ff] px-4 pr-12 text-sm font-medium text-[#0b1018] outline-none transition placeholder:text-[#68758a] focus:border-cyan-300"
                      />


                      <button
                        type="button"
                        onClick={() =>
                          setShowPassword(
                            !showPassword
                          )
                        }
                        className="absolute right-4 top-1/2 -translate-y-1/2 text-[#7a8799] transition hover:text-[#17202d]"
                      >

                        {showPassword ? (
                          <EyeOff size={18} />
                        ) : (
                          <Eye size={18} />
                        )}

                      </button>

                    </div>

                  </div>


                  {/* SIGN IN BUTTON */}

                  <button
                    type="submit"
                    disabled={
                      loading ||
                      googleLoading
                    }
                    className="mt-2 flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-cyan-400 text-sm font-semibold text-[#031015] shadow-[0_10px_35px_rgba(34,211,238,0.15)] transition hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-50"
                  >

                    {loading ? (
                      <>
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-[#031015]/30 border-t-[#031015]" />

                        Signing in...
                      </>
                    ) : (
                      <>
                        <Ticket size={17} />

                        Sign in to CinePass
                      </>
                    )}

                  </button>

                </form>


                {/* =================================================
                    DIVIDER
                ================================================= */}

                <div className="my-7 flex items-center gap-4">

                  <div className="h-px flex-1 bg-white/10" />

                  <span className="text-[10px] uppercase tracking-[0.3em] text-white/25">
                    or continue with
                  </span>

                  <div className="h-px flex-1 bg-white/10" />

                </div>


                {/* =================================================
                    GOOGLE
                ================================================= */}

                <button
                  type="button"
                  onClick={handleGoogleLogin}
                  disabled={
                    loading ||
                    googleLoading
                  }
                  className="flex h-14 w-full items-center justify-center gap-3 rounded-xl border border-white/10 bg-white/[0.015] text-sm font-medium text-white transition hover:border-white/20 hover:bg-white/[0.04] disabled:cursor-not-allowed disabled:opacity-50"
                >

                  {googleLoading ? (

                    <div className="h-5 w-5 animate-spin rounded-full border-2 border-white/20 border-t-cyan-300" />

                  ) : (

                    <svg
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                    >

                      <path
                        fill="#4285F4"
                        d="M21.35 12.23c0-.79-.07-1.55-.2-2.27H12v4.3h5.24a4.48 4.48 0 0 1-1.94 2.94v2.45h3.14c1.84-1.7 2.91-4.2 2.91-7.42Z"
                      />

                      <path
                        fill="#34A853"
                        d="M12 21.5c2.63 0 4.84-.87 6.45-2.35l-3.14-2.45c-.87.58-1.98.92-3.31.92-2.54 0-4.69-1.72-5.46-4.03H3.3v2.53A9.74 9.74 0 0 0 12 21.5Z"
                      />

                      <path
                        fill="#FBBC05"
                        d="M6.54 13.59A5.85 5.85 0 0 1 6.23 12c0-.55.1-1.08.31-1.59V7.88H3.3A9.5 9.5 0 0 0 2.25 12c0 1.53.37 2.98 1.05 4.12l3.24-2.53Z"
                      />

                      <path
                        fill="#EA4335"
                        d="M12 6.38c1.43 0 2.71.49 3.72 1.45l2.79-2.79C16.83 3.47 14.63 2.5 12 2.5a9.74 9.74 0 0 0-8.7 5.38l3.24 2.53C7.31 8.1 9.46 6.38 12 6.38Z"
                      />

                    </svg>

                  )}

                  <span>
                    {googleLoading
                      ? "Connecting to Google..."
                      : "Continue with Google"}
                  </span>

                </button>


                {/* =================================================
                    SIGNUP
                ================================================= */}

                <div className="mt-8 text-center">

                  <span className="text-sm text-white/35">
                    Don't have an account?
                  </span>

                  <Link
                    to="/signup"
                    className="ml-1 text-sm font-medium text-cyan-300 transition hover:text-cyan-200"
                  >
                    Create one
                  </Link>

                </div>

              </div>

            </section>

          </div>

        </main>

      </div>

    </div>
  );
}