import { useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import { apiFetch } from "../lib/api";
import { useAuth } from "../context/AuthContext";

export default function OAuth2Success() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const { login } = useAuth();

  const processed = useRef(false);

  useEffect(() => {
    // Prevent React StrictMode from running this twice
    if (processed.current) return;

    processed.current = true;

    const completeGoogleLogin = async () => {
      try {
        // ============================================================
        // GET JWT FROM URL
        // ============================================================

        const token = searchParams.get("token");

        console.log(
          "Google OAuth token received:",
          Boolean(token)
        );

        if (!token) {
          console.error(
            "Google OAuth failed: token not found"
          );

          localStorage.removeItem("cinepassToken");
          localStorage.removeItem("cinepassUser");

          navigate("/login", {
            replace: true,
          });

          return;
        }

        // ============================================================
        // STORE JWT
        // ============================================================

        localStorage.setItem(
          "cinepassToken",
          token
        );

        console.log(
          "Google JWT stored successfully"
        );

        // ============================================================
        // GET LOGGED-IN USER
        // ============================================================
        //
        // apiFetch automatically reads cinepassToken
        // and sends:
        //
        // Authorization: Bearer <JWT>
        //
        // ============================================================

        const userResponse = await apiFetch(
          "/api/auth/me"
        );

        console.log(
          "Google authenticated user:",
          userResponse
        );

        // ============================================================
        // SAVE AUTHENTICATION USING AuthContext
        // ============================================================

        login({
          token: token,
          id: userResponse.id,
          name: userResponse.name,
          email: userResponse.email,
          role: userResponse.role,
        });

        console.log(
          "Google login completed successfully"
        );

        // ============================================================
        // REMOVE TOKEN FROM URL
        // ============================================================

        window.history.replaceState(
          {},
          document.title,
          "/oauth2/success"
        );

        // ============================================================
        // GO HOME
        // ============================================================

        navigate("/", {
          replace: true,
        });

      } catch (error) {
        console.error(
          "Google OAuth completion error:",
          error
        );

        // Clean invalid authentication
        localStorage.removeItem(
          "cinepassToken"
        );

        localStorage.removeItem(
          "cinepassUser"
        );

        navigate("/login", {
          replace: true,
          state: {
            googleError:
              error?.message ||
              "Google authentication failed",
          },
        });
      }
    };

    completeGoogleLogin();
  }, [navigate, searchParams, login]);

  return (
    <div
      className="
        min-h-screen
        flex
        items-center
        justify-center
        bg-[#030507]
        text-white
      "
    >
      <div className="text-center">

        <div
          className="
            mx-auto
            mb-5
            h-10
            w-10
            animate-spin
            rounded-full
            border-2
            border-white/10
            border-t-cyan-300
          "
        />

        <h2 className="text-lg font-medium">
          Signing you in...
        </h2>

        <p className="mt-2 text-sm text-white/40">
          Please wait while we complete your Google login.
        </p>

      </div>
    </div>
  );
}