import * as React from "react";
import { Link as RouterLink, LinkProps } from "react-router";
import { useThemeContext } from "../../context/ThemeContextProvider";

/**
 * A themed Link component built on top of react-router's Link.
 *
 * React Router's Link component applies its own styling that differs from
 * Cloudscape's default link styling. This wrapper ensures consistent theming
 * by applying theme-aware colors:
 * - Dark mode: #42b4ff (lighter blue)
 * - Light mode: #006CE0 (darker blue)
 */
export const Link: React.FC<LinkProps> = (props) => {
  const { theme } = useThemeContext();
  const linkColor = theme === "dark" ? "#42b4ff" : "#006CE0";

  return <RouterLink {...props} style={{ color: linkColor, ...props.style }} />;
};
