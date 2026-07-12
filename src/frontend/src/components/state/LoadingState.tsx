import * as React from "react";
import Skeleton, { SkeletonTheme } from "react-loading-skeleton";
import "react-loading-skeleton/dist/skeleton.css";
import { useThemeContext } from "../../context/ThemeContextProvider";

interface LoadingStateProps {
  children?: React.ReactNode;
  loading?: boolean;
  rows?: number;
  height: number;
  width?: number;
  [key: string]: any;
}

const LoadingState = ({
  children,
  loading = true,
  rows = 1,
  height,
  width = "100%",
  ...props
}: LoadingStateProps) => {
  const { theme } = useThemeContext();

  // Actual hex values for light and dark mode
  // These match Cloudscape's design tokens but as resolved colors
  const baseColor = theme === "dark" ? "#35393f" : "#e0e0e0";
  const highlightColor = theme === "dark" ? "#4a4f56" : "#f5f5f5";

  if (!loading) {
    return <>{children}</>;
  }

  if (rows == null || rows === 1) {
    return (
      <SkeletonTheme baseColor={baseColor} highlightColor={highlightColor}>
        <Skeleton width={width} height={height} {...props} />
      </SkeletonTheme>
    );
  }

  return (
    <SkeletonTheme baseColor={baseColor} highlightColor={highlightColor}>
      {[...Array(rows)].map((x, i) => (
        <Skeleton key={i} width={width} height={height} {...props} />
      ))}
    </SkeletonTheme>
  );
};

export default LoadingState;
