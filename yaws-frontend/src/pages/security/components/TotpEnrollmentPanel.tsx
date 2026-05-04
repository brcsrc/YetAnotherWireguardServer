import {
  Alert,
  Box,
  Button,
  Container,
  Header,
  Input,
  Link,
  SpaceBetween,
  StatusIndicator,
} from "@cloudscape-design/components";
import { useState } from "react";
import QRCode from "qrcode";
import { userClient } from "../../../api/HTTPClients";
import { useFlashbarContext } from "../../../context/FlashbarContextProvider";

type TotpEnrollmentPanelProps = {
  twoFactorGloballyEnabled: boolean;
};

const TotpEnrollmentPanel = ({ twoFactorGloballyEnabled }: TotpEnrollmentPanelProps) => {
  const { addFlashbarItem } = useFlashbarContext();
  const [loading, setLoading] = useState(false);
  const [manualEntryKey, setManualEntryKey] = useState<string | null>(null);
  const [otpAuthUri, setOtpAuthUri] = useState<string | null>(null);
  const [otpQrCodeDataUrl, setOtpQrCodeDataUrl] = useState<string | null>(null);
  const [otpCode, setOtpCode] = useState("");
  const [isEnabled, setIsEnabled] = useState(false);

  const handleStartSetup = async () => {
    setLoading(true);
    try {
      const response = await userClient.startTotpEnrollment();
      setManualEntryKey(response.manualEntryKey || null);
      setOtpAuthUri(response.otpauthUri || null);
      if (response.otpauthUri) {
        const qrDataUrl = await QRCode.toDataURL(response.otpauthUri);
        setOtpQrCodeDataUrl(qrDataUrl);
      } else {
        setOtpQrCodeDataUrl(null);
      }
      setOtpCode("");
      setIsEnabled(false);
      addFlashbarItem({
        type: "success",
        header: "Setup Started",
        content:
          "Use the manual key or URI with your authenticator app, then confirm with a 6-digit code.",
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Failed to Start Setup",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmSetup = async () => {
    setLoading(true);
    try {
      await userClient.confirmTotpEnrollment({
        totpEnrollConfirmRequest: {
          otpCode,
        },
      });
      setIsEnabled(true);
      setOtpCode("");
      addFlashbarItem({
        type: "success",
        header: "TOTP Enabled",
        content: "Two-factor authentication is now enabled for your account.",
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Failed to Confirm Setup",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container
      header={
        <Header variant="h2" description="Manage authenticator app setup for your account.">
          TOTP Authenticator
        </Header>
      }
    >
      <SpaceBetween size="m">
        <StatusIndicator
          type={
            !twoFactorGloballyEnabled
              ? "warning"
              : isEnabled
                ? "success"
                : manualEntryKey
                  ? "in-progress"
                  : "info"
          }
        >
          {!twoFactorGloballyEnabled
            ? "2FA globally disabled"
            : isEnabled
              ? "Enabled"
              : manualEntryKey
                ? "Awaiting confirmation"
                : "Not configured"}
        </StatusIndicator>

        {isEnabled ? (
          <Alert type="success" header="Setup complete">
            TOTP is enabled for your account. Your next login will require a 6-digit authenticator
            code.
          </Alert>
        ) : null}

        {twoFactorGloballyEnabled ? (
          <>
            <Alert type="info" header="How setup works">
              Start setup, add the key to your authenticator app, then confirm with a 6-digit code.
            </Alert>

            <Box color="text-status-inactive">
              Supported apps: 1Password, Authy, Google Authenticator, Microsoft Authenticator.
            </Box>
          </>
        ) : null}

        {manualEntryKey ? (
          <SpaceBetween size="xs">
            {otpQrCodeDataUrl ? (
              <Box>
                <img
                  src={otpQrCodeDataUrl}
                  alt="Scan this QR code with your authenticator app"
                  style={{ width: 220, height: 220 }}
                />
              </Box>
            ) : null}
            <Box>
              <strong>Manual key:</strong> {manualEntryKey}
            </Box>
            {otpAuthUri ? (
              <Box>
                <Link external={true} href={otpAuthUri}>
                  Open OTP URI
                </Link>
              </Box>
            ) : null}
            {!isEnabled ? (
              <>
                <Input
                  value={otpCode}
                  onChange={({ detail }) => setOtpCode(detail.value.replace(/\D/g, "").slice(0, 6))}
                  placeholder="Enter 6-digit code"
                  inputMode="numeric"
                />
                <Button
                  variant="primary"
                  onClick={handleConfirmSetup}
                  loading={loading}
                  disabled={otpCode.length !== 6}
                >
                  Confirm Setup
                </Button>
              </>
            ) : null}
          </SpaceBetween>
        ) : null}

        <Button
          variant="normal"
          onClick={handleStartSetup}
          loading={loading}
          disabled={!twoFactorGloballyEnabled}
        >
          Start Setup
        </Button>
      </SpaceBetween>
    </Container>
  );
};

export default TotpEnrollmentPanel;
