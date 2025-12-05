import React from "react";
import { CopyToClipboard } from "@cloudscape-design/components";

interface PublicKeyDisplayProps {
  publicKey: string | undefined;
  /** Optional label for the copy button */
  copyButtonLabel?: string;
  /** Optional text shown after successful copy */
  copySuccessText?: string;
  /** Optional text shown on copy error */
  copyErrorText?: string;
}

/**
 * Displays a public key (or any cryptographic key) in a monospace font with copy-to-clipboard functionality.
 *
 * This component is designed for displaying WireGuard public keys and similar cryptographic values
 * that users frequently need to copy. The key is shown in a code-style format with nowrap to
 * prevent breaking the key across lines.
 *
 * @example
 * ```tsx
 * <PublicKeyDisplay publicKey={client?.clientPublicKeyValue} />
 * ```
 *
 * @example With custom copy labels
 * ```tsx
 * <PublicKeyDisplay
 *   publicKey={network?.publicKey}
 *   copyButtonLabel="Copy network key"
 * />
 * ```
 */
export const PublicKeyDisplay: React.FC<PublicKeyDisplayProps> = ({
  publicKey,
  copyButtonLabel = "Copy",
  copySuccessText = "Copied",
  copyErrorText = "Failed to copy",
}) => {
  if (!publicKey) {
    return <span style={{ color: "#888" }}>—</span>;
  }

  return (
    <code style={{ whiteSpace: "nowrap", fontSize: "14px" }}>
      <CopyToClipboard
        copyButtonLabel={copyButtonLabel}
        copySuccessText={copySuccessText}
        copyErrorText={copyErrorText}
        textToCopy={publicKey}
        variant="inline"
      />
    </code>
  );
};
