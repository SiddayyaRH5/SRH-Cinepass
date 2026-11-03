import React from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Eye,
  EyeOff,
  Lock,
  Mail,
  ArrowLeft,
  Ticket,
  Sparkles,
} from "lucide-react";
import toast from "react-hot-toast";
import { assets } from "../assets/assets";
import { apiFetch } from "../lib/api";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [show, setShow] = React.useState(false);
  const [busy, setBusy] = React.useState(false);

  const submit = async (e) => {
    e.preventDefault();

    if (!email.trim() || !password) {
      return toast.error("Enter your email and password");
    }

    try {
      setBusy(true);

      const data = await apiFetch("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: email.trim(),
          password,
        }),
      });

      login(data);

      toast.success("Welcome back!");
      navigate("/");
    } catch (err) {
      console.error("Login error:", err);

      toast.error(
        err?.message || "Invalid email or password"
      );
    } finally {
      setBusy(false);
    }
  };

  // ============================================================

  const continueWithGoogle = () => {
    /*
     * IMPORTANT:
     * Always use the deployed Spring Boot backend for Google OAuth.
     *
     * This prevents Google authentication from going to:
     * http://localhost:8080
     *
     * Production backend:
     * https://srh-cinepass.onrender.com
     */

    const backendUrl =
      import.meta.env.VITE_API_BASE_URL ||
      "https://srh-cinepass.onrender.com";

    window.location.href =
      `${backendUrl}/oauth2/authorization/google`;
  };

  return (
    <AuthLayout
      eyebrow="Welcome back"
      title="Continue your movie night."
      subtitle="Sign in to your CinePass account and pick up where the story left off."
    >
      <form
        onSubmit={submit}
        className="space-y-5"
      >
        <Field
          icon={Mail}
          label="Email address"
        >
          <input
            type="email"
            value={email}
            onChange={(e) =>
              setEmail(e.target.value)
            }
            placeholder="you@example.com"
            autoComplete="email"
            required
          />
        </Field>

        <Field
          icon={Lock}
          label="Password"
        >
          <div className="relative">
            <input
              type={
                show
                  ? "text"
                  : "password"
              }
              value={password}
              onChange={(e) =>
                setPassword(e.target.value)
              }
              placeholder="Enter your password"
              autoComplete="current-password"
              required
            />

            <button
              type="button"
              onClick={() =>
                setShow(!show)
              }
              className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-2 text-white/35 hover:text-cyan-300"
            >
              {show ? (
                <EyeOff size={17} />
              ) : (
                <Eye size={17} />
              )}
            </button>
          </div>
        </Field>

        <button
          type="submit"
          disabled={busy}
          className="mt-2 flex h-13 w-full items-center justify-center gap-2 rounded-xl bg-cyan-300 text-sm font-bold text-slate-950 transition hover:bg-cyan-200 hover:shadow-[0_0_35px_rgba(18,207,232,.18)] disabled:opacity-40"
        >
          {busy ? (
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-black/30 border-t-black" />
          ) : (
            <Ticket size={17} />
          )}

          {busy
            ? "Signing in..."
            : "Sign in to CinePass"}
        </button>
      </form>

      {/* ======================================================
          GOOGLE LOGIN
      ====================================================== */}

      <div className="my-6 flex items-center gap-4">
        <span className="h-px flex-1 bg-white/10" />

        <span className="text-[10px] uppercase tracking-[.25em] text-white/25">
          Or continue with
        </span>

        <span className="h-px flex-1 bg-white/10" />
      </div>

      <button
        type="button"
        onClick={continueWithGoogle}
        className="flex h-13 w-full items-center justify-center gap-3 rounded-xl border border-white/10 bg-white/[.04] text-sm font-semibold text-white transition hover:border-white/20 hover:bg-white/[.08]"
      >
        <GoogleIcon />

        Continue with Google
      </button>

      <div className="my-7 flex items-center gap-4">
        <span className="h-px flex-1 bg-white/10" />

        <span className="text-[10px] uppercase tracking-[.25em] text-white/25">
          New here?
        </span>

        <span className="h-px flex-1 bg-white/10" />
      </div>

      <p className="text-center text-sm text-white/40">
        Don't have an account?{" "}

        <Link
          to="/signup"
          className="font-medium text-cyan-300 hover:text-cyan-200"
        >
          Create one
        </Link>
      </p>
    </AuthLayout>
  );
}


// ============================================================
