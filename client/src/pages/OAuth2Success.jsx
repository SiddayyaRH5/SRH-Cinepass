import React from "react";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";

import { useAuth } from "../context/AuthContext";
import { apiFetch } from "../lib/api";

export default function OAuth2Success() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const processedRef = React.useRef(false);

  React.useEffect(() => {
    // Prevent the OAuth callback from being processed twice.
    if (processedRef.current) {
      return;
    }

    processedRef.current = true;

    const completeGoogleLogin = async () => {
      try {
        const params = new URLSearchParams(
          window.location.search
        );

        const token = params.get("token");

        /*
         * If the callback runs again after the first
         * successful login, the token may already have
         * been removed from the URL.
         *
         * In that case, check whether the user is already
         * logged in instead of showing an error.
         */
        if (!token) {
          const existingToken =
            localStorage.getItem("cinepassToken");

          const existingUser =
            localStorage.getItem("cinepassUser");

          if (existingToken && existingUser) {
            navigate("/", {
              replace: true,
            });

            return;
          }

          throw new Error(
            "Google login token was not received"
          );
        }

        /*
         * Save the JWT temporarily.
         * apiFetch() will automatically add:
         *
         * Authorization: Bearer <token>
         */
        localStorage.setItem(
          "cinepassToken",
          token
        );

        /*
         * Ask the backend for the authenticated
         * user's information.
         */
        const user = await apiFetch(
          "/api/auth/me"
        );

        /*
         * Store authentication information
         * inside AuthContext.
         */
        login({
          token: token,
          id: user.id,
          name: user.name,
          email: user.email,
          role: user.role,
        });

        /*
         * Remove the token from the browser URL.
         *
         * Before:
         * /oauth2/success?token=xxxxx
         *
         * After:
         * /oauth2/success
         */
        window.history.replaceState(
          {},
          document.title,
          window.location.pathname
        );

        toast.success(
          `Welcome, ${user.name}!`
        );

        /*
         * Go to the CinePass home page.
         */
        navigate("/", {
          replace: true,
        });

      } catch (error) {
        console.error(
          "Google login error:",
          error
        );

        /*
         * Only clear authentication if this
         * was actually a failed login.
         */
        localStorage.removeItem(
          "cinepassToken"
        );

        localStorage.removeItem(
          "cinepassUser"
        );

        toast.error(
          error.message ||
            "Google login failed"
        );

        navigate("/login", {
          replace: true,
        });
      }
    };

    completeGoogleLogin();
  }, [login, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#040609] text-white">

      <div className="text-center">

        <div className="mx-auto mb-6 h-12 w-12 animate-spin rounded-full border-4 border-white/10 border-t-cyan-400" />

        <h2 className="text-xl font-semibold">
          Completing Google Sign In...
        </h2>

        <p className="mt-2 text-sm text-white/50">
          Please wait while we sign you into CinePass.
        </p>

      </div>

    </div>
  );
}