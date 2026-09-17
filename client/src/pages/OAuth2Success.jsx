import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

export default function OAuth2Success() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const token = searchParams.get("token");

    console.log("Google OAuth token received:", !!token);

    if (!token) {
      console.error("Google OAuth failed: token not found");
      navigate("/login", { replace: true });
      return;
    }

    // Store JWT exactly where api.js expects it
    localStorage.setItem("cinepassToken", token);

    // Optional cleanup of old authentication data
    localStorage.removeItem("token");

    console.log("Google OAuth token stored successfully");

    // Give localStorage a moment, then go home
    navigate("/", { replace: true });
  }, [navigate, searchParams]);

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#030507",
        color: "#ffffff",
        fontFamily: "inherit",
      }}
    >
      <div style={{ textAlign: "center" }}>
        <h2>Signing you in...</h2>
        <p style={{ opacity: 0.6 }}>
          Please wait while we complete your Google login.
        </p>
      </div>
    </div>
  );
}