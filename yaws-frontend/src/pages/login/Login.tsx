import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import {
  Button,
  Container,
  Form,
  FormField,
  Grid,
  Header,
  Input,
  SpaceBetween,
  Tabs,
  Toggle,
} from "@cloudscape-design/components";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";
import { userClient } from "../../api/HTTPClients";

const Login = () => {
  const navigate = useNavigate();
  const { addFlashbarItem } = useFlashbarContext();
  const [usernameInput, setUsernameInput] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [activeTabId, setActiveTabId] = useState("login");
  const [showPassword, setShowPassword] = useState(false);
  const [otpCode, setOtpCode] = useState("");
  const [recoveryCode, setRecoveryCode] = useState("");
  const [requiresSecondFactor, setRequiresSecondFactor] = useState(false);
  const [allowedSecondFactors, setAllowedSecondFactors] = useState<string[]>([]);
  const [secondFactorMethod, setSecondFactorMethod] = useState<"TOTP" | "RECOVERY">("TOTP");
  const [challengeExpiresAt, setChallengeExpiresAt] = useState<string | null>(null);

  // Validation for register form
  const passwordsMatch = password === confirmPassword;
  const registerFormValid =
    usernameInput.trim() !== "" && password !== "" && confirmPassword !== "" && passwordsMatch;

  // Validation for login form
  const loginFormValid = usernameInput.trim() !== "" && password !== "";
  const verifyTotpFormValid = otpCode.trim().match(/^\d{6}$/) !== null;
  const verifyRecoveryCodeFormValid =
    recoveryCode.trim().match(/^[A-Z2-9]{4}-[A-Z2-9]{4}$/) !== null;

  const formatRecoveryCodeInput = (inputValue: string): string => {
    const sanitized = inputValue
      .toUpperCase()
      .replace(/[^A-Z2-9]/g, "")
      .slice(0, 8);
    if (sanitized.length <= 4) {
      return sanitized;
    }
    return `${sanitized.slice(0, 4)}-${sanitized.slice(4)}`;
  };

  // if we are already authenticated then navigate back to home
  useEffect(() => {
    (async function () {
      try {
        await userClient.whoami();
        navigate("/");
      } catch (error) {
        // nothing
      }
    })();
  }, []);

  // TODO: Integrate with utils/validation.ts for username / password validation
  const handleStartAuthentication = async (showSuccessToast: boolean) => {
    const authStartResponse = await userClient.authenticateStart({
      authenticateStartRequest: {
        userName: usernameInput,
        password: password,
      },
    });

    if (authStartResponse.twoFactorRequired) {
      setRequiresSecondFactor(true);
      setChallengeExpiresAt(authStartResponse.challengeExpiresAt || null);
      const serverAllowedFactors = (authStartResponse.allowedSecondFactors || []).filter(
        (factor): factor is "TOTP" | "RECOVERY" => factor === "TOTP" || factor === "RECOVERY"
      );
      const effectiveAllowedFactors =
        serverAllowedFactors.length > 0 ? serverAllowedFactors : ["TOTP"];
      setAllowedSecondFactors(effectiveAllowedFactors);
      setSecondFactorMethod(effectiveAllowedFactors.includes("TOTP") ? "TOTP" : "RECOVERY");
      setOtpCode("");
      setRecoveryCode("");
      addFlashbarItem({
        type: "info",
        header: "Second Factor Required",
        content: effectiveAllowedFactors.includes("RECOVERY")
          ? "Use your authenticator code or a recovery code to finish signing in."
          : "Enter the 6-digit code from your authenticator app to finish signing in.",
        dismissLabel: "Dismiss",
        duration: 5000,
      });
      return;
    }

    if (showSuccessToast) {
      addFlashbarItem({
        type: "success",
        header: "Login Successful",
        content: "Welcome back!",
        dismissLabel: "Dismiss",
        duration: 3000,
      });
    }

    navigate("/");
  };

  // Register admin user
  const handleCreateUserClick = async () => {
    // Validate passwords match
    if (password !== confirmPassword) {
      addFlashbarItem({
        type: "error",
        header: "Passwords Do Not Match",
        content: "Please ensure both password fields match.",
        dismissLabel: "Dismiss",
        duration: 5000,
      });
      return;
    }

    setLoading(true);
    try {
      await userClient.createAdminUser({
        user: {
          userName: usernameInput,
          password: password,
        },
      });
      addFlashbarItem({
        type: "success",
        header: "Registration Successful",
        content: `User "${usernameInput}" was created successfully.`,
        dismissLabel: "Dismiss",
        duration: 5000,
      });

      // Automatically log in after successful registration
      try {
        await handleStartAuthentication(false);
      } catch (loginError: any) {
        const errorMessage =
          loginError.response?.data?.message ||
          loginError.response?.data?.error ||
          loginError.message;
        addFlashbarItem({
          type: "error",
          header: "Auto-login Failed",
          content: `Registration succeeded but automatic login failed: ${errorMessage}. Please sign in manually.`,
          dismissLabel: "Dismiss",
          duration: 5000,
        });
      }
    } catch (e: any) {
      const errorMessage = e.response?.data?.message || e.response?.data?.error || e.message;
      addFlashbarItem({
        type: "error",
        header: "Registration Failed",
        content: `Failed to create admin user: ${errorMessage}`,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } finally {
      setLoading(false);
    }
  };

  // Authenticate user and navigate to dashboard on success
  const handleAuthenticateClick = async () => {
    setLoading(true);
    try {
      await handleStartAuthentication(true);
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Authentication Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyTotpClick = async () => {
    setLoading(true);
    try {
      await userClient.verifyTotp({
        verifyTotpRequest: {
          otpCode,
        },
      });
      addFlashbarItem({
        type: "success",
        header: "Login Successful",
        content: "Two-factor verification successful.",
        dismissLabel: "Dismiss",
        duration: 3000,
      });
      navigate("/");
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Verification Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyRecoveryCodeClick = async () => {
    setLoading(true);
    try {
      await userClient.verifyRecoveryCode({
        verifyRecoveryCodeRequest: {
          recoveryCode,
        },
      });
      addFlashbarItem({
        type: "success",
        header: "Login Successful",
        content: "Recovery code verification successful.",
        dismissLabel: "Dismiss",
        duration: 3000,
      });
      navigate("/");
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Verification Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        minHeight: "75vh",
        padding: "20px",
        backgroundColor: "inherit",
      }}
    >
      <div style={{ width: "100%", maxWidth: "500px" }}>
        <Container>
          <SpaceBetween size="l">
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "16px",
              }}
            >
              <img src="/favicon.ico" alt="YAWS Logo" style={{ width: "48px", height: "48px" }} />
              <Header variant="h1">YetAnotherWireguardServer</Header>
            </div>

            <Tabs
              activeTabId={activeTabId}
              onChange={({ detail }) => {
                setActiveTabId(detail.activeTabId);
                if (detail.activeTabId !== "login") {
                  setRequiresSecondFactor(false);
                  setAllowedSecondFactors([]);
                  setSecondFactorMethod("TOTP");
                  setChallengeExpiresAt(null);
                  setOtpCode("");
                  setRecoveryCode("");
                }
              }}
              tabs={[
                {
                  id: "login",
                  label: "Sign In",
                  content: (
                    <Form
                      actions={
                        <SpaceBetween size="xs" direction="horizontal">
                          {requiresSecondFactor ? (
                            <Button
                              variant="normal"
                              onClick={() => {
                                setRequiresSecondFactor(false);
                                setAllowedSecondFactors([]);
                                setSecondFactorMethod("TOTP");
                                setChallengeExpiresAt(null);
                                setOtpCode("");
                                setRecoveryCode("");
                              }}
                              disabled={loading}
                              formAction="none"
                            >
                              Back
                            </Button>
                          ) : null}
                          <Button
                            variant="primary"
                            onClick={
                              requiresSecondFactor
                                ? secondFactorMethod === "RECOVERY"
                                  ? handleVerifyRecoveryCodeClick
                                  : handleVerifyTotpClick
                                : handleAuthenticateClick
                            }
                            disabled={
                              loading ||
                              (requiresSecondFactor
                                ? secondFactorMethod === "RECOVERY"
                                  ? !verifyRecoveryCodeFormValid
                                  : !verifyTotpFormValid
                                : !loginFormValid)
                            }
                            formAction="none"
                          >
                            {loading
                              ? requiresSecondFactor
                                ? "Verifying..."
                                : "Signing In..."
                              : requiresSecondFactor
                                ? secondFactorMethod === "RECOVERY"
                                  ? "Verify Recovery Code"
                                  : "Verify Code"
                                : "Sign In"}
                          </Button>
                        </SpaceBetween>
                      }
                    >
                      <SpaceBetween size="m">
                        <FormField label="Username">
                          <Input
                            value={usernameInput}
                            onChange={({ detail }) => setUsernameInput(detail.value)}
                            placeholder="Enter username"
                            type="text"
                            autoComplete="username"
                            disabled={requiresSecondFactor}
                          />
                        </FormField>
                        <FormField label="Password">
                          <Input
                            value={password}
                            onChange={({ detail }) => setPassword(detail.value)}
                            placeholder="Enter password"
                            type="password"
                            autoComplete="current-password"
                            disabled={requiresSecondFactor}
                          />
                        </FormField>
                        {requiresSecondFactor ? (
                          <SpaceBetween size="s">
                            {allowedSecondFactors.includes("RECOVERY") ? (
                              <SpaceBetween direction="horizontal" size="xs">
                                <Button
                                  variant={secondFactorMethod === "TOTP" ? "primary" : "normal"}
                                  onClick={() => setSecondFactorMethod("TOTP")}
                                  disabled={loading}
                                  formAction="none"
                                >
                                  Authenticator App
                                </Button>
                                <Button
                                  variant={secondFactorMethod === "RECOVERY" ? "primary" : "normal"}
                                  onClick={() => setSecondFactorMethod("RECOVERY")}
                                  disabled={loading}
                                  formAction="none"
                                >
                                  Recovery Code
                                </Button>
                              </SpaceBetween>
                            ) : null}

                            {secondFactorMethod === "RECOVERY" ? (
                              <FormField
                                label="Recovery Code"
                                description="Enter one of your backup codes in format XXXX-XXXX"
                              >
                                <Input
                                  value={recoveryCode}
                                  onChange={({ detail }) =>
                                    setRecoveryCode(formatRecoveryCodeInput(detail.value))
                                  }
                                  placeholder="ABCD-2345"
                                  type="text"
                                />
                              </FormField>
                            ) : (
                              <FormField
                                label="Authenticator Code"
                                description={
                                  challengeExpiresAt
                                    ? `Challenge expires at ${new Date(challengeExpiresAt).toLocaleTimeString()}`
                                    : "Enter the 6-digit code from your authenticator app"
                                }
                              >
                                <Input
                                  value={otpCode}
                                  onChange={({ detail }) =>
                                    setOtpCode(detail.value.replace(/\D/g, "").slice(0, 6))
                                  }
                                  placeholder="123456"
                                  type="text"
                                  inputMode="numeric"
                                />
                              </FormField>
                            )}
                          </SpaceBetween>
                        ) : null}
                      </SpaceBetween>
                    </Form>
                  ),
                },
                {
                  id: "register",
                  label: "Register",
                  content: (
                    <Form>
                      <SpaceBetween size="m">
                        <FormField label="Username">
                          <Input
                            value={usernameInput}
                            onChange={({ detail }) => setUsernameInput(detail.value)}
                            placeholder="Enter username"
                            type="text"
                            autoComplete="username"
                          />
                        </FormField>
                        <FormField label="Password">
                          <Input
                            value={password}
                            onChange={({ detail }) => setPassword(detail.value)}
                            placeholder="Enter password"
                            type={showPassword ? "text" : "password"}
                            autoComplete="new-password"
                          />
                        </FormField>
                        <FormField
                          label="Confirm Password"
                          errorText={
                            confirmPassword && !passwordsMatch
                              ? "Passwords do not match"
                              : undefined
                          }
                          constraintText={
                            confirmPassword && passwordsMatch && password !== ""
                              ? "Passwords match"
                              : undefined
                          }
                        >
                          <Input
                            value={confirmPassword}
                            onChange={({ detail }) => setConfirmPassword(detail.value)}
                            placeholder="Re-enter password"
                            type={showPassword ? "text" : "password"}
                            autoComplete="new-password"
                            invalid={confirmPassword !== "" && !passwordsMatch}
                          />
                        </FormField>
                        <Grid gridDefinition={[{ colspan: 6 }, { colspan: 6 }]}>
                          <Toggle
                            onChange={({ detail }) => setShowPassword(detail.checked)}
                            checked={showPassword}
                          >
                            Show Password
                          </Toggle>
                          <div style={{ textAlign: "right" }}>
                            <Button
                              variant="primary"
                              onClick={handleCreateUserClick}
                              disabled={loading || !registerFormValid}
                              formAction="none"
                            >
                              {loading ? "Registering..." : "Register"}
                            </Button>
                          </div>
                        </Grid>
                      </SpaceBetween>
                    </Form>
                  ),
                },
              ]}
            />
          </SpaceBetween>
        </Container>
      </div>
    </div>
  );
};
export default Login;
