import React from "react";
import {
  Link,
  useNavigate,
} from "react-router-dom";

import {
  ArrowLeft,
  Eye,
  EyeOff,
  Lock,
  Mail,
  Ticket,
} from "lucide-react";

import toast from "react-hot-toast";

import { assets } from "../assets/assets";
import { apiFetch } from "../lib/api";
import { useAuth } from "../context/AuthContext";


// ============================================================
// LOGIN PAGE
// ============================================================

export default function Login() {
  const navigate = useNavigate();

  const { login } = useAuth();

  const [email, setEmail] =
    React.useState("");

  const [password, setPassword] =
    React.useState("");

  const [show, setShow] =
    React.useState(false);

  const [busy, setBusy] =
    React.useState(false);


  // ==========================================================
  // EMAIL / PASSWORD LOGIN
  // ==========================================================

  const submit = async (e) => {
    e.preventDefault();

    if (
      !email.trim() ||
      !password
    ) {
      return toast.error(
        "Enter your email and password"
      );
    }

    try {
      setBusy(true);

      const data = await apiFetch(
        "/api/auth/login",
        {
          method: "POST",

          body: JSON.stringify({
            email: email.trim(),
            password,
          }),
        }
      );

      login(data);

      toast.success(
        "Welcome back!"
      );

      navigate("/");

    } catch (err) {
      toast.error(
        err.message ||
          "Invalid email or password"
      );

    } finally {
      setBusy(false);
    }
  };


  // ==========================================================
  // GOOGLE LOGIN
  // ==========================================================

  const continueWithGoogle = () => {

    // IMPORTANT:
    // Production backend URL
    window.location.href =
      "https://srh-cinepass.onrender.com/oauth2/authorization/google?prompt=select_account";
  };


  // ==========================================================
  // UI
  // ==========================================================

  return (
    <AuthLayout
      eyebrow="Welcome back"
      title="Continue your movie night."
      subtitle="Sign in to your CinePass account and pick up where the story left off."
    >

      {/* ======================================================
          EMAIL / PASSWORD LOGIN
      ====================================================== */}

      <form
        onSubmit={submit}
        className="space-y-5"
      >

        {/* Email */}

        <Field
          icon={Mail}
          label="Email address"
        >

          <input
            type="email"
            value={email}
            onChange={(e) =>
              setEmail(
                e.target.value
              )
            }
            placeholder="you@example.com"
            autoComplete="email"
            required
          />

        </Field>


        {/* Password */}

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
                setPassword(
                  e.target.value
                )
              }
              placeholder="Enter your password"
              autoComplete="current-password"
              required
            />


            {/* Show / Hide Password */}

            <button
              type="button"
              onClick={() =>
                setShow(!show)
              }
              className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-2 text-white/35 transition hover:text-cyan-300"
            >

              {show ? (
                <EyeOff size={17} />
              ) : (
                <Eye size={17} />
              )}

            </button>

          </div>

        </Field>


        {/* Sign In Button */}

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
          GOOGLE LOGIN SEPARATOR
      ====================================================== */}

      <div className="my-6 flex items-center gap-4">

        <span className="h-px flex-1 bg-white/10" />

        <span className="text-[10px] uppercase tracking-[.25em] text-white/25">
          Or continue with
        </span>

        <span className="h-px flex-1 bg-white/10" />

      </div>


      {/* ======================================================
          GOOGLE LOGIN BUTTON
      ====================================================== */}

      <button
        type="button"
        onClick={
          continueWithGoogle
        }
        className="flex h-13 w-full items-center justify-center gap-3 rounded-xl border border-white/10 bg-white/[.04] text-sm font-semibold text-white transition hover:border-white/20 hover:bg-white/[.08]"
      >

        <GoogleIcon />

        Continue with Google

      </button>


      {/* ======================================================
          SIGNUP SEPARATOR
      ====================================================== */}

      <div className="my-7 flex items-center gap-4">

        <span className="h-px flex-1 bg-white/10" />

        <span className="text-[10px] uppercase tracking-[.25em] text-white/25">
          New here?
        </span>

        <span className="h-px flex-1 bg-white/10" />

      </div>


      {/* ======================================================
          SIGNUP LINK
      ====================================================== */}

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
// GOOGLE ICON
// ============================================================

function GoogleIcon() {

  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >

      <path
        fill="#4285F4"
        d="M21.35 12.27c0-.73-.07-1.43-.2-2.1H12v3.98h5.24a4.48 4.48 0 0 1-1.94 2.94v2.45h3.14c1.84-1.69 2.91-4.18 2.91-7.27Z"
      />

      <path
        fill="#34A853"
        d="M12 21.6c2.63 0 4.84-.87 6.45-2.36l-3.14-2.45c-.87.58-1.98.93-3.31.93-2.55 0-4.71-1.72-5.49-4.03H3.26v2.53A9.74 9.74 0 0 0 12 21.6Z"
      />

      <path
        fill="#FBBC05"
        d="M6.51 13.69A5.85 5.85 0 0 1 6.2 12c0-.59.11-1.16.31-1.69V7.78H3.26A9.6 9.6 0 0 0 2.25 12c0 1.53.37 2.98 1.01 4.22l3.25-2.53Z"
      />

      <path
        fill="#EA4335"
        d="M12 6.28c1.43 0 2.71.49 3.72 1.45l2.79-2.79C16.84 3.35 14.63 2.4 12 2.4a9.74 9.74 0 0 0-8.74 5.38l3.25 2.53C7.29 8 9.45 6.28 12 6.28Z"
      />

    </svg>
  );
}


// ============================================================
// AUTH LAYOUT
// ============================================================

function AuthLayout({
  eyebrow,
  title,
  subtitle,
  children,
}) {

  const navigate =
    useNavigate();


  return (
    <main className="relative min-h-screen overflow-hidden bg-[#040609]">

      {/* Background */}

      <img
        src="/backgroundImage.png"
        alt=""
        className="absolute inset-0 h-full w-full object-cover opacity-35"
      />


      {/* Dark Gradient */}

      <div className="absolute inset-0 bg-gradient-to-r from-[#040609] via-[#040609]/85 to-[#040609]/55" />

      <div className="absolute inset-0 bg-gradient-to-t from-[#040609] via-transparent to-[#040609]/70" />

      <div className="cine-noise absolute inset-0" />


      {/* ======================================================
          BACK BUTTON
      ====================================================== */}

      <button
        type="button"
        onClick={() =>
          navigate("/")
        }
        className="absolute left-5 top-5 z-20 flex items-center gap-2 rounded-full border border-white/10 bg-black/20 px-4 py-2.5 text-xs text-white/60 backdrop-blur-md transition hover:text-white"
      >

        <ArrowLeft size={15} />

        Back to CinePass

      </button>


      {/* ======================================================
          MAIN CONTENT
      ====================================================== */}

      <div className="relative z-10 flex min-h-screen items-center justify-center px-5 py-24">

        <div className="grid w-full max-w-6xl items-center gap-12 lg:grid-cols-[1fr_460px]">


          {/* ==================================================
              LEFT SIDE
          ================================================== */}

          <div className="hidden lg:block animate-cine-rise">

            <img
              src={assets.logo}
              alt="SRH CinePass"
              className="h-14 w-auto"
            />


            <div className="mt-8 max-w-xl">

              <p className="text-xs font-semibold uppercase tracking-[.3em] text-cyan-300">
                Cinema without friction
              </p>


              <h2 className="mt-4 text-5xl font-semibold leading-tight">

                Every seat has a story.

                <br />

                <span className="text-white/45">
                  Choose yours.
                </span>

              </h2>


              <p className="mt-5 max-w-md text-sm leading-7 text-white/45">
                Discover shows, choose your theatre
                and reserve the perfect seats with a
                seamless CinePass experience.
              </p>

            </div>


            <div className="mt-8 flex gap-3">

              <span className="rounded-full border border-white/10 bg-white/[.04] px-4 py-2 text-xs text-white/45">
                Curated movies
              </span>

              <span className="rounded-full border border-white/10 bg-white/[.04] px-4 py-2 text-xs text-white/45">
                Secure JWT login
              </span>

            </div>

          </div>


          {/* ==================================================
              LOGIN CARD
          ================================================== */}

          <div className="w-full max-w-[460px] justify-self-center animate-cine-scale">


            {/* Mobile Logo */}

            <div className="mb-5 flex justify-center lg:hidden">

              <img
                src={assets.logo}
                alt="SRH CinePass"
                className="h-11 w-auto"
              />

            </div>


            {/* Card */}

            <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-[#0c1017]/85 p-7 shadow-2xl backdrop-blur-2xl sm:p-9">

              <div className="absolute inset-x-10 top-0 h-px bg-gradient-to-r from-transparent via-cyan-300 to-transparent" />


              <p className="text-xs font-semibold uppercase tracking-[.25em] text-cyan-300">
                {eyebrow}
              </p>


              <h1 className="mt-3 text-3xl font-semibold leading-tight sm:text-4xl">
                {title}
              </h1>


              <p className="mt-3 text-sm leading-6 text-white/40">
                {subtitle}
              </p>


              <div className="mt-8">
                {children}
              </div>

            </div>

          </div>

        </div>

      </div>

    </main>
  );
}


// ============================================================
// INPUT FIELD
// ============================================================

function Field({
  icon: Icon,
  label,
  children,
}) {

  return (
    <label className="block">

      <span className="mb-2 flex items-center gap-2 text-xs text-white/50">

        <Icon
          size={14}
          className="text-cyan-300"
        />

        {label}

      </span>


      <div className="[&_input]:h-13 [&_input]:w-full [&_input]:rounded-xl [&_input]:border [&_input]:border-white/10 [&_input]:bg-white/[.04] [&_input]:px-4 [&_input]:text-sm [&_input]:text-white [&_input]:outline-none [&_input]:placeholder:text-white/20 [&_input]:focus:border-cyan-300/40 [&_input]:focus:bg-white/[.06]">

        {children}

      </div>

    </label>
  );
}