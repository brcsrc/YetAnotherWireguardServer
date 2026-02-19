import { Alert, Box, Container, Header, SpaceBetween } from "@cloudscape-design/components";
import { useEffect, useState } from "react";
import { userClient } from "../../api/HTTPClients";
import TotpEnrollmentPanel from "./components/TotpEnrollmentPanel";
import RecoveryCodesPanel from "./components/RecoveryCodesPanel";

const Security = () => {
  const [twoFactorGloballyEnabled, setTwoFactorGloballyEnabled] = useState(true);

  useEffect(() => {
    (async function loadTwoFactorGlobalState() {
      try {
        const whoamiResponse = await userClient.whoami();
        setTwoFactorGloballyEnabled(whoamiResponse.twoFactorGloballyEnabled !== false);
      } catch {
        setTwoFactorGloballyEnabled(true);
      }
    })();
  }, []);

  return (
    <SpaceBetween size="l">
      <Header variant="h1">Security</Header>

      <Container>
        <SpaceBetween size="m">
          <Alert type="info" header="Two-factor authentication">
            You can enroll an authenticator app and generate recovery codes from this page.
          </Alert>
          <Box color="text-status-inactive">
            Use these controls from a trusted device and store recovery codes in a secure location.
          </Box>
        </SpaceBetween>
      </Container>

      {!twoFactorGloballyEnabled ? (
        <Alert type="warning" header="Two-factor authentication is globally disabled">
          Backend enforcement is disabled by environment variable <code>TWO_FA_ENABLED</code>. TOTP
          enrollment and recovery-code management are unavailable until it is enabled.
        </Alert>
      ) : null}

      <TotpEnrollmentPanel twoFactorGloballyEnabled={twoFactorGloballyEnabled} />
      <RecoveryCodesPanel twoFactorGloballyEnabled={twoFactorGloballyEnabled} />
    </SpaceBetween>
  );
};

export default Security;
