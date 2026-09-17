import React from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  CheckCircle2,
  Eye,
  EyeOff,
  Lock,
  Mail,
  Ticket,
  UserRound,
} from "lucide-react";
import toast from "react-hot-toast";
import { assets } from "../assets/assets";
import { apiFetch } from "../lib/api";

export default function Signup() {
  const navigate = useNavigate();

  const [name, setName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [show, setShow] = React.useState(false);
  const [busy, setBusy] = React.useState(false);

  const submit = async (e) => {
    e.preventDefault();

    if (name.trim().length < 2) {
      return toast.error("Enter your full name");
    }

    if (password.length < 6) {
      return toast.error(
        "Password must be at least 6 characters"
      );
    }

    try {
      setBusy(true);

      await apiFetch("/api/auth/signup", {
        method: "POST",
        body: JSON.stringify({
          name: name.trim(),
          email: email.trim(),
          password,
        }),
      });

      toast.success(
        "Account created. Welcome to CinePass!"
      );

      navigate("/login");
    } catch (err) {
      console.error("Signup error:", err);

      toast.error(
        err?.message ||
          "Unable to create account"
      );
    } finally {
      setBusy(false);
    }
  };

  // ============================================================
  // GOOGLE SIGNUP
  // ============================================================

  const continueWithGoogle = () => {
    /*
     * Always start Google OAuth from the deployed
     * Spring Boot backend.
     */

    const backendUrl =
      import.meta.env.VITE_API_BASE_URL ||
      "https://srh-cinepass.onrender.com";

    window.location.href =
      `${backendUrl}/oauth2/authorization/google`;
  };

  return (
    <main className="relative min-h-screen overflow-hidden bg-[#040609]">

      <img
        src="/backgroundImage.png"
        alt=""
        className="absolute inset-0 h-full w-full object-cover opacity-30"
      />

      <div className="absolute inset-0 bg-gradient-to-r from-[#040609] via-[#040609]/88 to-[#040609]/55" />

      <div className="absolute inset-0 bg-gradient-to-t from-[#040609] via-transparent to-[#040609]/70" />

      <div className="cine-noise absolute inset-0" />

      <button
        type="button"
        onClick={() => navigate("/")}
        className="absolute left-5 top-5 z-20 flex items-center gap-2 rounded-full border border-white/10 bg-black/20 px-4 py-2.5 text-xs text-white/60 backdrop-blur-md hover:text-white"
      >
        <ArrowLeft size={15} />

        Back to CinePass
      </button>

      <div className="relative z-10 flex min-h-screen items-center justify-center px-5 py-24">

        <div className="grid w-full max-w-6xl items-center gap-12 lg:grid-cols-[1fr_500px]">

          {/* LEFT SIDE */}

          <div className="hidden lg:block">

            <img
              src={assets.logo}
              alt="SRH CinePass"
              className="h-14 w-auto"
            />

            <p className="mt-9 text-xs font-semibold uppercase tracking-[.3em] text-cyan-300">
              Your cinema passport
            </p>

            <h2 className="mt-4 text-5xl font-semibold leading-tight">

              Find the seat.
              <br />

              <span className="text-white/45">
                Own the moment.
              </span>

            </h2>

            <p className="mt-5 max-w-md text-sm leading-7 text-white/45">
              Create your CinePass account and unlock a faster,
              smarter way to book the big screen.
            </p>

          </div>


          {/* RIGHT SIDE */}

          <div className="w-full max-w-[500px] justify-self-center animate-cine-scale">

            <div className="mb-5 flex justify-center lg:hidden">

              <img
                src={assets.logo}
                alt="SRH CinePass"
                className="h-11 w-auto"
              />

            </div>

            <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-[#0c1017]/85 p-7 shadow-2xl backdrop-blur-2xl sm:p-9">

              <div className="absolute inset-x-10 top-0 h-px bg-gradient-to-r from-transparent via-cyan-300 to-transparent" />

              <p className="text-xs font-semibold uppercase tracking-[.25em] text-cyan-300">
                Join the experience
              </p>

              <h1 className="mt-3 text-3xl font-semibold sm:text-4xl">
                Create your CinePass.
              </h1>

              <p className="mt-3 text-sm leading-6 text-white/40">
                One account for movies, theatres, seats and tickets.
              </p>


              {/* FORM */}

              <form
                onSubmit={submit}
                className="mt-8 space-y-5"
              >

                <Field
                  icon={UserRound}
                  label="Full name"
                >
                  <input
                    value={name}
                    onChange={(e) =>
                      setName(e.target.value)
                    }
                    placeholder="Your full name"
                    autoComplete="name"
                    required
                  />
                </Field>


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
                      placeholder="Create a password"
                      autoComplete="new-password"
                      required
                    />

                    <button
                      type="button"
                      onClick={() =>
                        setShow(!show)
                      }
                      className="absolute right-3 top-1/2 -translate-y-1/2 p-2 text-white/35 hover:text-cyan-300"
                    >
                      {show ? (
                        <EyeOff size={17} />
                      ) : (
                        <Eye size={17} />
                      )}
                    </button>

                  </div>

                  <span className="mt-2 block text-[11px] text-white/25">
                    Use at least 6 characters.
                  </span>

                </Field>


                <button
                  type="submit"
                  disabled={busy}
                  className="flex h-13 w-full items-center justify-center gap-2 rounded-xl bg-cyan-300 text-sm font-bold text-slate-950 transition hover:bg-cyan-200 hover:shadow-[0_0_35px_rgba(18,207,232,.18)] disabled:opacity-40"
                >

                  {busy ? (
                    <span className="h-4 w-4 animate-spin rounded-full border-2 border-black/30 border-t-black" />
                  ) : (
                    <Ticket size={17} />
                  )}

                  {busy
                    ? "Creating account..."
                    : "Create account"}

                </button>

              </form>


              {/* GOOGLE */}

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


              {/* SECURITY MESSAGE */}

              <div className="mt-7 flex items-center gap-3 text-xs text-white/30">

                <CheckCircle2
                  size={15}
                  className="text-cyan-300"
                />

                Your password is securely protected by the backend.

              </div>


              {/* LOGIN */}

              <p className="mt-6 text-center text-sm text-white/40">

                Already have an account?{" "}

                <Link
                  to="/login"
                  className="font-medium text-cyan-300 hover:text-cyan-200"
                >
                  Sign in
                </Link>

              </p>

            </div>

          </div>

        </div>

      </div>

    </main>
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
// FIELD
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