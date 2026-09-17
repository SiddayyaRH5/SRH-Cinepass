import React from "react";

import {
  Link,
  useLocation,
  useNavigate,
} from "react-router-dom";

import {
  Mail,
  Lock,
  ArrowRight,
  Film,
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

  const [loading, setLoading] = React.useState(false);
  const [googleLoading, setGoogleLoading] = React.useState(false);


  // ============================================================
  // NORMAL LOGIN
  // ============================================================

  const handleSubmit = async (e) => {
    e.preventDefault();

    // ----------------------------------------------------------
    // VALIDATION
    // ----------------------------------------------------------

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

      // --------------------------------------------------------
      // LOGIN API
      // --------------------------------------------------------

      const response = await apiFetch(
        "/api/auth/login",
        {
          method: "POST",

          body: JSON.stringify({
            email: email.trim(),
            password: password,
          }),
        }
      );

      console.log("Login response:", response);

      // --------------------------------------------------------
      // SAVE LOGIN
      // --------------------------------------------------------

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

      // --------------------------------------------------------
      // REDIRECT
      // --------------------------------------------------------

      const from =
        location.state?.from || "/";

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
       * Spring Security OAuth2 endpoint.
       *
       * Local backend:
       * http://localhost:8080/oauth2/authorization/google
       *
       * After successful authentication,
       * backend redirects to the frontend.
       */

      window.location.href =
        "http://localhost:8080/oauth2/authorization/google";

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
              Welcome back
            </h1>

            <p className="mt-2 text-sm text-white/35">
              Sign in to continue your CinePass
              experience.
            </p>

          </div>


          {/* ==================================================
              LOGIN FORM
          ================================================== */}

          <form
            onSubmit={handleSubmit}
            className="space-y-4"
          >

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
                  className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3.5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 transition focus:border-cyan-300/35 focus:bg-white/[.045]"
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
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3.5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 transition focus:border-cyan-300/35 focus:bg-white/[.045]"
                />

              </div>

            </div>


            {/* LOGIN BUTTON */}

            <button
              type="submit"
              disabled={
                loading || googleLoading
              }
              className="group flex w-full items-center justify-center gap-2 rounded-xl bg-cyan-300 px-4 py-3.5 text-sm font-semibold text-[#061014] transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-50"
            >

              {loading
                ? "Signing in..."
                : "Sign In"}

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
              GOOGLE BUTTON
          ================================================== */}

          <button
            type="button"
            onClick={handleGoogleLogin}
            disabled={
              loading || googleLoading
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


          {/* ==================================================
              SIGNUP
          ================================================== */}

          <p className="mt-7 text-center text-sm text-white/30">

            Don't have an account?

            <Link
              to="/signup"
              className="ml-1 text-cyan-300 transition hover:text-cyan-200"
            >
              Create account
            </Link>

          </p>

        </div>

      </div>

    </div>
  );
}