import React from "react";

import {
  Link,
  useNavigate,
} from "react-router-dom";

import {
  User,
  Building2,
  Mail,
  Lock,
  ArrowRight,
  Film,
} from "lucide-react";

import toast from "react-hot-toast";

import { apiFetch, API_BASE } from "../lib/api";

export default function Signup() {
  const navigate = useNavigate();

  // ============================================================
  // FORM STATE
  // ============================================================

  const [role, setRole] =
    React.useState("USER");

  const [name, setName] =
    React.useState("");

  const [email, setEmail] =
    React.useState("");

  const [password, setPassword] =
    React.useState("");

  const [loading, setLoading] =
    React.useState(false);

  const [googleLoading, setGoogleLoading] =
    React.useState(false);

  // ============================================================
  // NORMAL SIGNUP
  // ============================================================

  const handleSubmit = async (e) => {
    e.preventDefault();

    // ----------------------------------------------------------
    // VALIDATION
    // ----------------------------------------------------------

    if (!name.trim()) {
      toast.error(
        "Please enter your name"
      );
      return;
    }

    if (!email.trim()) {
      toast.error(
        "Please enter your email"
      );
      return;
    }

    if (password.length < 6) {
      toast.error(
        "Password must contain at least 6 characters"
      );
      return;
    }

    try {
      setLoading(true);

      // --------------------------------------------------------
      // SIGNUP API
      // --------------------------------------------------------

      const response = await apiFetch(
        "/api/auth/signup",
        {
          method: "POST",

          body: JSON.stringify({
            name: name.trim(),
            email: email.trim().toLowerCase(),
            password: password,
            role: role,
          }),
        }
      );

      console.log(
        "Signup response:",
        response
      );

      // --------------------------------------------------------
      // SUCCESS
      // --------------------------------------------------------

      toast.success(
        role === "THEATRE_OWNER"
          ? "Theatre owner account created!"
          : "Account created successfully!"
      );

      // --------------------------------------------------------
      // REDIRECT TO LOGIN
      // --------------------------------------------------------

      navigate("/login", {
        replace: true,
      });

    } catch (error) {
      console.error(
        "Signup error:",
        error
      );

      toast.error(
        error?.message ||
          "Unable to create account"
      );

    } finally {
      setLoading(false);
    }
  };

  // ============================================================
  // GOOGLE SIGNUP
  // ============================================================

  const handleGoogleSignup = () => {
    try {
      setGoogleLoading(true);

      /*
       * Google authentication is handled
       * completely by Spring Security.
       *
       * IMPORTANT:
       * Do NOT use localhost here.
       *
       * API_BASE comes from:
       *
       * VITE_API_BASE_URL
       *
       * Production:
       * https://srh-cinepass.onrender.com
       *
       * Local:
       * http://localhost:8080
       */

      const backendUrl =
        API_BASE ||
        "http://localhost:8080";

      const googleUrl =
        `${backendUrl}/oauth2/authorization/google`;

      console.log(
        "Google OAuth URL:",
        googleUrl
      );

      window.location.href =
        googleUrl;

    } catch (error) {
      console.error(
        "Google signup error:",
        error
      );

      setGoogleLoading(false);

      toast.error(
        "Unable to continue with Google"
      );
    }
  };

  // ============================================================
  // UI
  // ============================================================

  return (
    <div className="min-h-screen bg-[#040609] px-5 py-10 text-white">

      <div className="mx-auto flex min-h-[90vh] max-w-md items-center">

        <div className="w-full">

          {/* ==================================================
              BRAND
          ================================================== */}

          <div className="mb-8 text-center">

            <div className="mx-auto mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-cyan-300/10 text-cyan-300">

              <Film size={22} />

            </div>

            <h1 className="text-3xl font-semibold">
              Create your account
            </h1>

            <p className="mt-2 text-sm text-white/35">
              Join CinePass and experience
              movies differently.
            </p>

          </div>


          {/* ==================================================
              ACCOUNT TYPE
          ================================================== */}

          <div className="mb-6">

            <p className="mb-3 text-xs font-medium uppercase tracking-[.16em] text-white/35">
              I want to join as
            </p>

            <div className="grid grid-cols-2 gap-3">

              {/* USER */}

              <button
                type="button"
                onClick={() =>
                  setRole("USER")
                }
                disabled={
                  loading ||
                  googleLoading
                }
                className={`rounded-2xl border p-4 text-left transition-all ${
                  role === "USER"
                    ? "border-cyan-300/40 bg-cyan-300/10 shadow-[0_0_30px_rgba(34,211,238,.06)]"
                    : "border-white/10 bg-white/[.025] hover:border-white/20 hover:bg-white/[.04]"
                }`}
              >

                <div
                  className={`mb-3 grid h-10 w-10 place-items-center rounded-xl ${
                    role === "USER"
                      ? "bg-cyan-300/15 text-cyan-300"
                      : "bg-white/[.05] text-white/40"
                  }`}
                >
                  <User size={18} />
                </div>

                <p className="text-sm font-semibold">
                  Movie User
                </p>

                <p className="mt-1 text-[11px] leading-4 text-white/30">
                  Book tickets and manage bookings
                </p>

              </button>


              {/* THEATRE OWNER */}

              <button
                type="button"
                onClick={() =>
                  setRole("THEATRE_OWNER")
                }
                disabled={
                  loading ||
                  googleLoading
                }
                className={`rounded-2xl border p-4 text-left transition-all ${
                  role === "THEATRE_OWNER"
                    ? "border-cyan-300/40 bg-cyan-300/10 shadow-[0_0_30px_rgba(34,211,238,.06)]"
                    : "border-white/10 bg-white/[.025] hover:border-white/20 hover:bg-white/[.04]"
                }`}
              >

                <div
                  className={`mb-3 grid h-10 w-10 place-items-center rounded-xl ${
                    role === "THEATRE_OWNER"
                      ? "bg-cyan-300/15 text-cyan-300"
                      : "bg-white/[.05] text-white/40"
                  }`}
                >
                  <Building2 size={18} />
                </div>

                <p className="text-sm font-semibold">
                  Theatre Owner
                </p>

                <p className="mt-1 text-[11px] leading-4 text-white/30">
                  Manage theatres, shows and business
                </p>

              </button>

            </div>

          </div>


          {/* ==================================================
              NORMAL SIGNUP FORM
          ================================================== */}

          <form
            onSubmit={handleSubmit}
            className="space-y-4"
          >

            {/* NAME */}

            <div>

              <label className="mb-2 block text-xs font-medium text-white/40">
                Full Name
              </label>

              <div className="relative">

                <User
                  size={17}
                  className="absolute left-4 top-1/2 -translate-y-1/2 text-white/25"
                />

                <input
                  value={name}
                  onChange={(e) =>
                    setName(e.target.value)
                  }
                  placeholder="Enter your name"
                  autoComplete="name"
                  disabled={
                    loading ||
                    googleLoading
                  }
                  className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3.5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 transition focus:border-cyan-300/35 focus:bg-white/[.045] disabled:opacity-50"
                />

              </div>

            </div>


            {/* EMAIL */}

            <div>

              <label className="mb-2 block text-xs font-medium text-white/40">
                Email
              </label>

              <div className="relative">

                <Mail
                  size={17}
                  className="absolute left-4 top-1/2 -translate-y-1/2 text-white/25"
                />

                <input
                  type="email"
                  value={email}
                  onChange={(e) =>
                    setEmail(e.target.value)
                  }
                  placeholder="you@example.com"
                  autoComplete="email"
                  disabled={
                    loading ||
                    googleLoading
                  }
                  className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3.5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 transition focus:border-cyan-300/35 focus:bg-white/[.045] disabled:opacity-50"
                />

              </div>

            </div>


            {/* PASSWORD */}

            <div>

              <label className="mb-2 block text-xs font-medium text-white/40">
                Password
              </label>

              <div className="relative">

                <Lock
                  size={17}
                  className="absolute left-4 top-1/2 -translate-y-1/2 text-white/25"
                />

                <input
                  type="password"
                  value={password}
                  onChange={(e) =>
                    setPassword(e.target.value)
                  }
                  placeholder="Minimum 6 characters"
                  autoComplete="new-password"
                  disabled={
                    loading ||
                    googleLoading
                  }
                  className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3.5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 transition focus:border-cyan-300/35 focus:bg-white/[.045] disabled:opacity-50"
                />

              </div>

            </div>


            {/* SELECTED ROLE */}

            <div className="rounded-xl border border-cyan-300/10 bg-cyan-300/[.035] px-4 py-3">

              <p className="text-[10px] uppercase tracking-[.16em] text-white/25">
                Selected account
              </p>

              <p className="mt-1 text-sm font-medium text-cyan-200">

                {role === "USER"
                  ? "Movie User"
                  : "Theatre Owner"}

              </p>

            </div>


            {/* CREATE ACCOUNT */}

            <button
              type="submit"
              disabled={
                loading ||
                googleLoading
              }
              className="group flex w-full items-center justify-center gap-2 rounded-xl bg-cyan-300 px-4 py-3.5 text-sm font-semibold text-[#061014] transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-50"
            >

              {loading
                ? "Creating account..."
                : "Create Account"}

              {!loading && (
                <ArrowRight
                  size={17}
                  className="transition-transform group-hover:translate-x-1"
                />
              )}

            </button>

          </form>


          {/* ==================================================
              OR
          ================================================== */}

          <div className="my-7 flex items-center gap-4">

            <div className="h-px flex-1 bg-white/10" />

            <span className="text-[10px] uppercase tracking-[.25em] text-white/30">
              or continue with
            </span>

            <div className="h-px flex-1 bg-white/10" />

          </div>


          {/* ==================================================
              GOOGLE SIGNUP
          ================================================== */}

          <button
            type="button"
            onClick={handleGoogleSignup}
            disabled={
              loading ||
              googleLoading
            }
            className="group flex w-full items-center justify-center gap-3 rounded-xl border border-white/10 bg-white/[.03] px-4 py-3.5 text-sm font-medium text-white transition hover:border-white/20 hover:bg-white/[.06] disabled:cursor-not-allowed disabled:opacity-50"
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
                  d="M12 21.5c2.63 0 4.84-.87 6.45-2.35l-3.14-2.45c-.87.58-1.98.92-3.31.92-2.54 0-4.69-1.72-5.46-4.03H3.3v2.53A9.74 9.74 0 0 12 21.5Z"
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


          {/* ==================================================
              LOGIN
          ================================================== */}

          <p className="mt-7 text-center text-sm text-white/30">

            Already have an account?

            <Link
              to="/login"
              className="ml-1 text-cyan-300 transition hover:text-cyan-200"
            >
              Sign in
            </Link>

          </p>

        </div>

      </div>

    </div>
  );
}