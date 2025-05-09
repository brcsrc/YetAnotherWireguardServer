import {
  applyMode,
  Mode as CloudscapeMode,
} from "@cloudscape-design/global-styles";

export const THEME_STORAGE_KEY = "yaws-theme";

export type Mode = "light" | "dark";

export const getTheme = (): Mode => {
  const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
  return storedTheme === "dark" || !storedTheme ? "dark" : "light";
};

export const setTheme = (mode: Mode) => {
  applyMode(mode as CloudscapeMode);
  localStorage.setItem(THEME_STORAGE_KEY, mode);
};

export const toggleTheme = () => {
  const currentTheme = getTheme();
  const newTheme = currentTheme === "dark" ? "light" : "dark";
  setTheme(newTheme);
  return newTheme;
};
