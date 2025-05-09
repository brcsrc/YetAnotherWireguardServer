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
} from "@cloudscape-design/components";
import { setTheme } from "../utils/theme";

export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  // Apply dark mode on mount
  useEffect(() => {
    setTheme("dark");
  }, []);

  const handleSubmit = () => {
    // Mock submit action; replace with API call (e.g., POST /api/v1/login) later
    navigate("/dashboard");
  };

  // TODO: Add Create Admin User  and Authenicate User API Call Functions to work with Login / Register Actions
  return (
    <div
      style={{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        minHeight: "100vh",
        padding: "20px",
        backgroundColor: "#0f1b2a", // Cloudscape dark mode background for consistency
      }}
    >
      <Container>
        <Form
          actions={
            <Button variant="primary" onClick={handleSubmit}>
              Sign In
            </Button>
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
              />
            </FormField>
            <FormField label="Password">
              <Input
                value={password}
                onChange={({ detail }) => setPassword(detail.value)}
                placeholder="Enter password"
                type="password"
              />
            </FormField>
          </SpaceBetween>
        </Form>
      </Container>
    </div>
  );
}
