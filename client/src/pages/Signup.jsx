import React from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  User,
  Building2,
  Mail,
  Lock,
  ArrowRight,
  Film,
} from "lucide-react";
import toast from "react-hot-toast";

import { apiFetch } from "../lib/api";
import { useAuth } from "../context/AuthContext";

export default function Signup() {

  const navigate = useNavigate();
  const { login } = useAuth();

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

  const handleSubmit = async (e) => {

    e.preventDefault();

    if (!name.trim()) {
      toast.error("Please enter your name");
      return;
    }

    if (!email.trim()) {
      toast.error("Please enter your email");
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

      const response = await apiFetch(
        "/api/auth/signup",
        {
          method: "POST",
          body: JSON.stringify({
            name: name.trim(),
            email: email.trim(),
            password,
            role,
          }),
        }
      );

      /*
       * Signup response does not contain a JWT.
       * Redirect to login after successful registration.
       */
      toast.success(
        role === "THEATRE_OWNER"
          ? "Theatre owner account created!"
          : "Account created successfully!"
      );

      navigate("/login");

    } catch (error) {

      console.error(error);

      toast.error(
        error.message ||
        "Unable to create account"
      );

    } finally {

      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#040609] px-5 py-10 text-white">

      <div className="mx-auto flex min-h-[90vh] max-w-md items-center">

        <div className="w-full">

          {/* BRAND */}

          <div className="mb-8 text-center">

            <div className="mx-auto mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-cyan-300/10 text-cyan-300">
              <Film size={22} />
            </div>

            <h1 className="text-3xl font-semibold">
              Create your account
            </h1>

            <p className="mt-2 text-sm text-white/35">
              Join CinePass and experience movies differently.
            </p>

          </div>

          {/* ACCOUNT TYPE */}

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

              {/* OWNER */}

              <button
                type="button"
                onClick={() =>
                  setRole("THEATRE_OWNER")
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

          {/* FORM */}

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
                  className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3.5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 focus:border-cyan-300/35"
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
                  className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3.5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 focus:border-cyan-300/35"
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
                  className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3.5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 focus:border-cyan-300/35"
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

            {/* SUBMIT */}

            <button
              type="submit"
              disabled={loading}
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

          {/* LOGIN */}

          <p className="mt-7 text-center text-sm text-white/30">

            Already have an account?

            <Link
              to="/login"
              className="ml-1 text-cyan-300 hover:text-cyan-200"
            >
              Sign in
            </Link>

          </p>

        </div>

      </div>

    </div>
  );
}
