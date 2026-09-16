import React, { createContext, useContext, useEffect, useMemo, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("cinepassToken"));
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("cinepassUser") || "null");
    } catch {
      return null;
    }
  });

  useEffect(() => {
    const sync = () => {
      setToken(localStorage.getItem("cinepassToken"));
      try {
        setUser(JSON.parse(localStorage.getItem("cinepassUser") || "null"));
      } catch {
        setUser(null);
      }
    };
    window.addEventListener("cinepass-auth", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("cinepass-auth", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  const login = (data) => {
    localStorage.setItem("cinepassToken", data.token);
    localStorage.setItem(
      "cinepassUser",
      JSON.stringify({
        id: data.id,
        name: data.name,
        email: data.email,
        role: data.role,
      })
    );
    setToken(data.token);
    setUser({ id: data.id, name: data.name, email: data.email, role: data.role });
    window.dispatchEvent(new Event("cinepass-auth"));
  };

  const logout = () => {
    localStorage.removeItem("cinepassToken");
    localStorage.removeItem("cinepassUser");
    setToken(null);
    setUser(null);
    window.dispatchEvent(new Event("cinepass-auth"));
  };

  const value = useMemo(
    () => ({ token, user, isAuthenticated: Boolean(token && user), login, logout }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
};
