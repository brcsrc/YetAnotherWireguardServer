import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import {
  Button,
  Container,
  Form,
  FormField,
  Header,
  Input,
  SpaceBetween,
  Alert,
} from "@cloudscape-design/components";
import { setTheme } from "../utils/theme";
import {
  UserControllerApi,
  Configuration,
} from "@yaws/yaws-ts-api-client";

/**
 * Login page for YAWS.
 * Handles admin user registration and authentication.
 */
export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Set the theme to dark on mount
  useEffect(() => {
    setTheme("dark");
  }, []);

  // TODO: Integrade with utils/validation.ts for username / password validation
  // TODO: Make Register a seperate tab on the login component, so we can populate the second password field etc.
  // Register admin user
  const handleCreateUserClick = async () => {
    setError(null);
    setSuccess(null);
    setLoading(true);
    const userClient = new UserControllerApi(new Configuration());
    try {
      await userClient.createAdminUser({
        user: {
          userName: username,
          password: password,
        },
      });
      setSuccess(`User "${username}" was created successfully.`);
    } catch (e: any) {
      setError("Failed to create admin user.");

      // Only log detailed errors in development mode. Vite special Obj
      // In production, this block does not run.
      if (import.meta.env.DEV) {
        console.error("Error creating admin user:", e);
      }
    } finally {
      setLoading(false);
    }
  };

  // Authenticate user and navigate to dashboard on success
  const handleAuthenticateClick = async () => {
    setError(null);
    setSuccess(null);
    setLoading(true);
    const userClient = new UserControllerApi(new Configuration());
    try {
      await userClient.authenticateAndIssueToken({
        user: {
          userName: username,
          password: password,
        },
      });
      navigate("/dashboard");
    } catch (error: any) {
      setError("Authentication failed. Please check your credentials.");

      // Only log detailed errors in development mode. Vite special Obj
      // In production, this block does not run.
      if (import.meta.env.DEV) {
        console.error("Authentication failed:", error);
      }
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
        minHeight: "100vh",
        padding: "20px",
        backgroundColor: "#0f1b2a",
      }}
    >
      <Container>
        {success && <Alert type="success">{success}</Alert>}
        {error && (
          <Alert header="Error" type="error">
            {error}
          </Alert>
        )}
        <Form
          actions={
            <SpaceBetween direction="horizontal" size="s">
              <Button
                variant="primary"
                onClick={handleCreateUserClick}
                disabled={loading}
              >
                {loading ? "Registering..." : "Register"}
              </Button>
              <Button
                variant="primary"
                onClick={handleAuthenticateClick}
                disabled={loading}
              >
                {loading ? "Signing In..." : "Sign In"}
              </Button>
            </SpaceBetween>
          }
        >
          <SpaceBetween size="m">
            <Header variant="h1">Sign In to YAWS</Header>
            <FormField label="Username">
              <Input
                value={username}
                onChange={({ detail }) => setUsername(detail.value)}
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
                type="password"
                autoComplete="current-password"
              />
            </FormField>
          </SpaceBetween>
        </Form>
      </Container>
    </div>
  );
}
