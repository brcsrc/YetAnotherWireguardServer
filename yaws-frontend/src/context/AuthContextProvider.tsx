import * as React from "react";
import { useNavigate, useLocation } from "react-router";
import { userClient } from "../utils/clients";
import { useFlashbarContext } from "./FlashbarContextProvider";
import { handleAuthError } from "../utils/auth";

interface AuthContextType {
  username: string | null;
  logout: () => Promise<void>;
}

const AuthContext = React.createContext<AuthContextType>({
  username: null,
  logout: async () => {},
});

interface AuthContextProviderProps {
  children: React.ReactNode;
}

const AUTH_CHECK_INTERVAL = 5 * 60 * 1000; // 5 minutes

export const AuthContextProvider = (props: AuthContextProviderProps) => {
  const [username, setUsername] = React.useState<string | null>(null);
  const navigate = useNavigate();
  const location = useLocation();
  const { addFlashbarItem } = useFlashbarContext();

  const checkAuth = React.useCallback(async () => {
    if (location.pathname === "/login") {
      return;
    }

    try {
      const response = await userClient.whoami();
      setUsername(response.user || null);
    } catch (error: any) {
      setUsername(null);
      navigate("/login");
    }
  }, [location.pathname, navigate, addFlashbarItem]);

  const logout = React.useCallback(async () => {
    try {
      await userClient.logout();
      setUsername(null);
      navigate("/login");
    } catch (error: any) {
      addFlashbarItem({
        type: "error",
        header: "Logout Failed",
        content: "Failed to logout. Please try again.",
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    }
  }, [navigate, addFlashbarItem]);

  // Check auth on mount and set up interval
  React.useEffect(() => {
    checkAuth();

    const intervalId = setInterval(() => {
      checkAuth();
    }, AUTH_CHECK_INTERVAL);

    return () => clearInterval(intervalId);
  }, [checkAuth]);

  return <AuthContext.Provider value={{ username, logout }}>{props.children}</AuthContext.Provider>;
};

export const useAuthContext = () => {
  return React.useContext(AuthContext);
};
