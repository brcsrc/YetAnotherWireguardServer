import * as React from "react";
import { applyMode, Mode as CloudscapeMode } from "@cloudscape-design/global-styles";

/**
 * original author github.com/jon-hetrick
 * applyMode and Mode are the preferred approach for setting theme in the latest cloudscape version:
 * https://cloudscape.design/get-started/for-developers/global-styles/#apply-global-styles
 */

const THEME_STORAGE_KEY = "yaws-theme";

export type Mode = "light" | "dark";

const getTheme = (): Mode => {
  const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
  return storedTheme === "dark" || !storedTheme ? "dark" : "light";
};

const setThemeMode = (mode: Mode) => {
  applyMode(mode as CloudscapeMode);
  localStorage.setItem(THEME_STORAGE_KEY, mode);
};

const toggleThemeMode = () => {
  const currentTheme = getTheme();
  const newTheme = currentTheme === "dark" ? "light" : "dark";
  setThemeMode(newTheme);
  return newTheme;
};

// this is placed in a context provider to wrap the entire <App/> to ensure that the entire dom is in scope

interface ThemeContextType {
  theme: Mode;
  setTheme: (mode: Mode) => void;
  toggleTheme: () => void;
}

const ThemeContext = React.createContext<ThemeContextType>({
  theme: "dark",
  setTheme: () => {},
  toggleTheme: () => {},
});

interface ThemeContextProviderProps {
  children: React.ReactNode;
}

export const ThemeContextProvider = (props: ThemeContextProviderProps) => {
  const [theme, setThemeState] = React.useState<Mode>(getTheme());

  React.useEffect(() => {
    // Initialize theme on mount
    const currentTheme = getTheme();
    handleSetTheme(currentTheme);
  }, []);

  const handleSetTheme = (mode: Mode) => {
    setThemeMode(mode);
    setThemeState(mode);
  };

  const handleToggleTheme = () => {
    const newTheme = toggleThemeMode();
    setThemeState(newTheme);
    return newTheme;
  };

  return (
    <ThemeContext.Provider
      value={{ theme, setTheme: handleSetTheme, toggleTheme: handleToggleTheme }}
    >
      {props.children}
    </ThemeContext.Provider>
  );
};

export const useThemeContext = () => {
  return React.useContext(ThemeContext);
};
