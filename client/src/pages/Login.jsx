import React from "react";

import { Link, useNavigate } from "react-router-dom";

import {
  Eye,
  EyeOff,
  Lock,
  Mail,
  Ticket,
} from "lucide-react";

import toast from "react-hot-toast";

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


/* ============================================================
   AUTH LAYOUT
   ============================================================ */

function AuthLayout({
  eyebrow,
  title,
  subtitle,
  children,
}) {
  return (
    <div className="min-h-screen bg-slate-950 text-white">
      <div className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-10">

        {/* Background glow */}
        <div className="pointer-events-none absolute inset-0">
          <div className="absolute left-1/2 top-1/2 h-[500px] w-[500px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-cyan-400/5 blur-3xl" />
          <div className="absolute -left-32 -top-32 h-96 w-96 rounded-full bg-cyan-500/5 blur-3xl" />
          <div className="absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-blue-500/5 blur-3xl" />
        </div>

        {/* Main card */}
        <div className="relative z-10 w-full max-w-md">

          <div className="mb-8 text-center">
            <div className="mb-6 flex justify-center">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl border border-cyan-300/20 bg-cyan-300/10 shadow-[0_0_35px_rgba(18,207,232,.08)]">
                <Ticket
                  size={23}
                  className="text-cyan-300"
                />
              </div>
            </div>

            <p className="mb-2 text-[10px] font-semibold uppercase tracking-[.3em] text-cyan-300/70">
              {eyebrow}
            </p>

            <h1 className="text-3xl font-bold tracking-tight text-white">
              {title}
            </h1>

            <p className="mx-auto mt-3 max-w-sm text-sm leading-6 text-white/40">
              {subtitle}
            </p>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-6 shadow-2xl backdrop-blur-xl">
            {children}
          </div>

          <p className="mt-6 text-center text-xs text-white/20">
            © {new Date().getFullYear()} SRH CinePass
          </p>
        </div>
      </div>
    </div>
  );
}


/* ============================================================
   FORM FIELD
   ============================================================ */

function Field({
  icon: Icon,
  label,
  children,
}) {
  return (
    <label className="block">
      <span className="mb-2 flex items-center gap-2 text-xs font-medium text-white/55">
        <Icon
          size={14}
          className="text-cyan-300/70"
        />

        {label}
      </span>

      <div className="[&_input]:h-12 [&_input]:w-full [&_input]:rounded-xl [&_input]:border [&_input]:border-white/10 [&_input]:bg-white/[0.04] [&_input]:px-4 [&_input]:text-sm [&_input]:text-white [&_input]:outline-none [&_input]:transition [&_input]:placeholder:text-white/20 [&_input]:focus:border-cyan-300/40 [&_input]:focus:bg-white/[0.06] [&_input]:focus:ring-1 [&_input]:focus:ring-cyan-300/20">
        {children}
      </div>
    </label>
  );
}