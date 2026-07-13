import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import {
  Box,
  Button,
  ColumnLayout,
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
import { useAuthContext } from "../../context/AuthContextProvider";
import { userClient } from "../../api/HTTPClients";
import { validatePassword } from "../../utils/validation";
import { useDebouncedValue } from "../../utils/debounce";

const Login = () => {
  const navigate = useNavigate();
  const { addFlashbarItem } = useFlashbarContext();
  const [usernameInput, setUsernameInput] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [activeTabId, setActiveTabId] = useState("login");
  const [showPassword, setShowPassword] = useState(false);

  // Validation for login form
  const loginFormValid = usernameInput.trim() !== "" && password !== "";

  // Debounce error states by 500ms so they don't flash on every keystroke
  // while the user is still typing - only shown once typing pauses.
  const debouncedPassword = useDebouncedValue(password);
  const passwordError = password === "" ? null : validatePassword(debouncedPassword);

  const debouncedConfirmPassword = useDebouncedValue(confirmPassword);
  const passwordsMatch = password === confirmPassword;
  const passwordsMismatch =
    confirmPassword !== "" && debouncedPassword !== debouncedConfirmPassword;

  const registerFormValid =
    usernameInput.trim() !== "" &&
    password !== "" &&
    confirmPassword !== "" &&
    passwordsMatch &&
    passwordError === null;

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
        await userClient.authenticateAndIssueToken({
          user: {
            userName: usernameInput,
            password: password,
          },
        });
        navigate("/");
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
      await userClient.authenticateAndIssueToken({
        user: {
          userName: usernameInput,
          password: password,
        },
      });
      addFlashbarItem({
        type: "success",
        header: "Login Successful",
        content: "Welcome back!",
        dismissLabel: "Dismiss",
        duration: 3000,
      });
      navigate("/");
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
              <img src="/favicon.ico" alt="YAWS Logo" style={{ width: "58px", height: "58px" }} />
              <Header variant="h1">YetAnotherWireguardServer</Header>
            </div>

            <Tabs
              activeTabId={activeTabId}
              onChange={({ detail }) => setActiveTabId(detail.activeTabId)}
              tabs={[
                {
                  id: "login",
                  label: "Sign In",
                  content: (
                    <Form
                      actions={
                        <Button
                          variant="primary"
                          onClick={handleAuthenticateClick}
                          disabled={loading || !loginFormValid}
                          formAction="none"
                        >
                          {loading ? "Signing In..." : "Sign In"}
                        </Button>
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
                          />
                        </FormField>
                        <FormField label="Password">
                          <Input
                            value={password}
                            onChange={({ detail }) => setPassword(detail.value)}
                            placeholder="Enter password"
                            type={showPassword ? "text" : "password"}
                            autoComplete="current-password"
                          />
                        </FormField>
                        <Toggle
                          onChange={({ detail }) => setShowPassword(detail.checked)}
                          checked={showPassword}
                        >
                          Show Password
                        </Toggle>
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
                        <FormField label="Password" errorText={passwordError ?? undefined}>
                          <Input
                            value={password}
                            onChange={({ detail }) => setPassword(detail.value)}
                            placeholder="Enter password"
                            type={showPassword ? "text" : "password"}
                            autoComplete="new-password"
                            invalid={passwordError !== null}
                          />
                        </FormField>
                        <FormField
                          label="Confirm Password"
                          errorText={passwordsMismatch ? "Passwords do not match" : undefined}
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
                            invalid={passwordsMismatch}
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
