import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() =>
    localStorage.getItem("cinepassToken")
  );

  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(
        localStorage.getItem("cinepassUser") || "null"
      );
    } catch {
      return null;
    }
  });

  // ============================================================
  // SYNC AUTH STATE
  // ============================================================

  useEffect(() => {
    const syncAuth = () => {
      setToken(localStorage.getItem("cinepassToken"));

      try {
        setUser(
          JSON.parse(
            localStorage.getItem("cinepassUser") || "null"
          )
        );
      } catch {
        setUser(null);
      }
    };

    window.addEventListener("cinepass-auth", syncAuth);
    window.addEventListener("storage", syncAuth);

    return () => {
      window.removeEventListener("cinepass-auth", syncAuth);
      window.removeEventListener("storage", syncAuth);
    };
  }, []);

  // ============================================================
  // LOGIN
  // ============================================================

  const login = (data) => {
    if (!data?.token) {
      throw new Error("Authentication token is missing");
    }

    const userData = {
      id: data.id,
      name: data.name,
      email: data.email,
      role: data.role,
    };

    localStorage.setItem(
      "cinepassToken",
      data.token
    );

    localStorage.setItem(
      "cinepassUser",
      JSON.stringify(userData)
    );

    setToken(data.token);
    setUser(userData);

    window.dispatchEvent(
      new Event("cinepass-auth")
    );
  };

  // ============================================================
  // LOGOUT
  // ============================================================

  const logout = () => {
    localStorage.removeItem("cinepassToken");
    localStorage.removeItem("cinepassUser");

    setToken(null);
    setUser(null);

    window.dispatchEvent(
      new Event("cinepass-auth")
    );
  };

  // ============================================================
  // AUTH STATE
  // ============================================================

  const value = useMemo(
    () => ({
      token,
      user,

      isAuthenticated: Boolean(
        token && user
      ),

      login,
      logout,
    }),
    [token, user]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const value = useContext(AuthContext);

  if (!value) {
    throw new Error(
      "useAuth must be used inside AuthProvider"
    );
  }

  return value;
};