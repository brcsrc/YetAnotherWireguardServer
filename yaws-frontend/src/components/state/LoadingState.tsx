import * as React from "react";
import Skeleton, { SkeletonTheme } from "react-loading-skeleton";
import {
  colorBackgroundControlDisabled,
  colorBackgroundButtonPrimaryDisabled,
} from "@cloudscape-design/design-tokens";

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
  width,
  ...props
}: LoadingStateProps) => {
  if (!loading) {
    return <>{children}</>;
  }

  if (rows == null || rows === 1) {
    return (
      <SkeletonTheme
        baseColor={colorBackgroundControlDisabled}
        highlightColor={colorBackgroundButtonPrimaryDisabled}
      >
        <Skeleton style={{ width: width, height: height }} {...props} />
      </SkeletonTheme>
    );
  }

  return (
    <SkeletonTheme
      baseColor={colorBackgroundControlDisabled}
      highlightColor={colorBackgroundButtonPrimaryDisabled}
    >
      {[...Array(rows)].map((x, i) => (
        <Skeleton key={i} style={{ width: width, height: height }} {...props} />
      ))}
    </SkeletonTheme>
  );
};

export default LoadingState;
