import {
  Wizard,
  Container,
  Header,
  SpaceBetween,
  FormField,
  Input,
  ColumnLayout,
  KeyValuePairs
} from "@cloudscape-design/components";
import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { networkClientClient } from "../../utils/clients";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";

const CreateClient = () => {
  const navigate = useNavigate();
  const { networkName } = useParams<{ networkName: string }>();
  const { addFlashbarItem } = useFlashbarContext();

  const [clientName, setClientName] = useState("");
  const [clientCidr, setClientCidr] = useState("");
  const [clientDns, setClientDns] = useState("");
  const [allowedIps, setAllowedIps] = useState("");
  const [networkEndpoint, setNetworkEndpoint] = useState("");

  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [showErrorText, setShowErrorText] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setLoading(true);
    try {
      await networkClientClient.createNetworkClient({
        createNetworkClientRequest: {
          clientName,
          clientCidr,
          clientDns,
          allowedIps,
          networkName,
          networkEndpoint
        }
      });
      addFlashbarItem({
        type: "success",
        header: "Client Created",
        content: `Client "${clientName}" was created successfully.`,
        dismissLabel: "Dismiss",
        duration: 5000
      });
      navigate(`/networks/${networkName}`);
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Create Client Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 10000
      });
    } finally {
      setLoading(false);
    }
  };

  const isConfigureStepValid = () => {
    return clientName.trim() !== "" &&
           clientCidr.trim() !== "" &&
           clientDns.trim() !== "" &&
           allowedIps.trim() !== "" &&
           networkEndpoint.trim() !== "";
  };

  const handleNavigate = ({ detail }) => {
    const requestedStepIndex = detail.requestedStepIndex;
    // If moving forward from step 0, validate the configure step
    if (activeStepIndex === 0 && requestedStepIndex > activeStepIndex) {
      if (!isConfigureStepValid()) {
        setShowErrorText(true);
        return; // Prevent navigation if validation fails
      }
    }

    setActiveStepIndex(requestedStepIndex);
  };

  return (
    <Wizard
      i18nStrings={{
        stepNumberLabel: stepNumber => `Step ${stepNumber}`,
        collapsedStepsLabel: (stepNumber, stepsCount) => `Step ${stepNumber} of ${stepsCount}`,
        cancelButton: "Cancel",
        previousButton: "Previous",
        nextButton: "Next",
        submitButton: "Create client",
        optional: "optional"
      }}
      onNavigate={handleNavigate}
      onCancel={() => navigate(`/networks/${networkName}`)}
      onSubmit={handleSubmit}
      activeStepIndex={activeStepIndex}
      isLoadingNextStep={loading}
      steps={[
        {
          title: "Configure client",
          description: "Enter the client configuration details",
          isOptional: false,
          errorText: showErrorText ? "Please fill in all required fields" : "",
          content: (
            <Container header={<Header variant="h2">Client configuration</Header>}>
              <SpaceBetween size="l">
                <FormField
                  label="Network name"
                  description="This client will be created for this network"
                >
                  <Input
                    value={networkName}
                    disabled={true}
                  />
                </FormField>

                <FormField
                  label="Client name"
                  description="Unique name for the client"
                >
                  <Input
                    value={clientName}
                    onChange={({ detail }) => setClientName(detail.value)}
                    placeholder="e.g., my-device-name"
                  />
                </FormField>

                <FormField
                  label="Client CIDR"
                  description="CIDR block for the client"
                >
                  <Input
                    value={clientCidr}
                    onChange={({ detail }) => setClientCidr(detail.value)}
                    placeholder="e.g., 10.100.0.3/24"
                  />
                </FormField>

                <FormField
                  label="Client DNS"
                  description="DNS server for the client"
                >
                  <Input
                    value={clientDns}
                    onChange={({ detail }) => setClientDns(detail.value)}
                    placeholder="e.g., 1.1.1.1"
                  />
                </FormField>

                <FormField
                  label="Allowed IPs"
                  description="IP ranges the client is allowed to access"
                >
                  <Input
                    value={allowedIps}
                    onChange={({ detail }) => setAllowedIps(detail.value)}
                    placeholder="e.g., 0.0.0.0/0"
                  />
                </FormField>

                <FormField
                  label="Network endpoint"
                  description="Server endpoint IP/hostname for the client"
                >
                  <Input
                    value={networkEndpoint}
                    onChange={({ detail }) => setNetworkEndpoint(detail.value)}
                    placeholder="e.g., 127.0.0.1"
                  />
                </FormField>
              </SpaceBetween>
            </Container>
          )
        },
        {
          title: "Review and create",
          content: (
            <SpaceBetween size="l">
              <Container header={<Header variant="h2">Review client configuration</Header>}>
                <ColumnLayout columns={2} variant="text-grid">
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Network name",
                        value: networkName || "-"
                      },
                      {
                        label: "Client name",
                        value: clientName || "-"
                      },
                      {
                        label: "Client CIDR",
                        value: clientCidr || "-"
                      }
                    ]}
                  />
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Client DNS",
                        value: clientDns || "-"
                      },
                      {
                        label: "Allowed IPs",
                        value: allowedIps || "-"
                      },
                      {
                        label: "Network endpoint",
                        value: networkEndpoint || "-"
                      }
                    ]}
                  />
                </ColumnLayout>
              </Container>
            </SpaceBetween>
          )
        }
      ]}
    />
  );
};

export default CreateClient;
