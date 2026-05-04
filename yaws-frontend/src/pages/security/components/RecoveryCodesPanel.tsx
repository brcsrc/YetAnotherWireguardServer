import {
  Alert,
  Box,
  Button,
  Container,
  Header,
  ColumnLayout,
  SpaceBetween,
  StatusIndicator,
} from "@cloudscape-design/components";
import { useState } from "react";
import { userClient } from "../../../api/HTTPClients";
import { useFlashbarContext } from "../../../context/FlashbarContextProvider";

type RecoveryCodesPanelProps = {
  twoFactorGloballyEnabled: boolean;
};

const RecoveryCodesPanel = ({ twoFactorGloballyEnabled }: RecoveryCodesPanelProps) => {
  const { addFlashbarItem } = useFlashbarContext();
  const [loading, setLoading] = useState(false);
  const [codes, setCodes] = useState<string[]>([]);

  const handleGenerateCodes = async () => {
    setLoading(true);
    try {
      const response = await userClient.regenerateRecoveryCodes();
      setCodes(response.codes || []);
      addFlashbarItem({
        type: "success",
        header: "Recovery Codes Generated",
        content: "Store these codes in a safe place. Each code can be used only once.",
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Failed to Generate Codes",
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
        <Header variant="h2" description="Generate and rotate one-time backup codes.">
          Recovery Codes
        </Header>
      }
    >
      <SpaceBetween size="m">
        <StatusIndicator
          type={!twoFactorGloballyEnabled ? "warning" : codes.length > 0 ? "success" : "info"}
        >
          {!twoFactorGloballyEnabled
            ? "2FA globally disabled"
            : codes.length > 0
              ? "Codes generated"
              : "No active code set shown"}
        </StatusIndicator>

        {twoFactorGloballyEnabled ? (
          <>
            <Alert type="info" header="How codes work">
              Generate a new set of one-time backup codes, store them safely, and use one if you
              lose access to your authenticator app.
            </Alert>
            <Box color="text-status-inactive">
              Generating a new set invalidates older unused codes.
            </Box>
          </>
        ) : null}
        {codes.length > 0 ? (
          <Alert type="warning" header="Shown once">
            Save these codes now. They are only displayed immediately after generation.
            <ColumnLayout columns={2} variant="text-grid">
              {codes.map((code) => (
                <Box key={code}>{code}</Box>
              ))}
            </ColumnLayout>
          </Alert>
        ) : null}
        <Button
          variant="primary"
          onClick={handleGenerateCodes}
          loading={loading}
          disabled={!twoFactorGloballyEnabled}
        >
          Generate New Codes
        </Button>
      </SpaceBetween>
    </Container>
  );
};

export default RecoveryCodesPanel;
