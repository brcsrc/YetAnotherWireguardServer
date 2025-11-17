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
import { useNavigate } from "react-router";
import { networkClient } from "../../utils/clients";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";

const CreateNetwork = () => {
  const navigate = useNavigate();
  const { addFlashbarItem } = useFlashbarContext();

  const [networkName, setNetworkName] = useState("");
  const [networkCidr, setNetworkCidr] = useState("");
  const [networkListenPort, setNetworkListenPort] = useState("");
  const [networkTag, setNetworkTag] = useState("");

  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [showErrorText, setShowErrorText] = useState(false)
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setLoading(true);
    try {
      await networkClient.createNetwork({
        network: {
          networkName,
          networkCidr,
          networkListenPort: parseInt(networkListenPort),
          networkTag: networkTag || undefined
        }
      });
      addFlashbarItem({
        type: "success",
        header: "Network Created",
        content: `Network "${networkName}" was created successfully.`,
        dismissLabel: "Dismiss",
        duration: 5000
      });
      navigate("/networks");
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Create Network Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 10000
      });
    } finally {
      setLoading(false);
    }
  };

  const isConfigureStepValid = () => {
    return networkName.trim() !== "" &&
           networkCidr.trim() !== "" &&
           networkListenPort.trim() !== "" &&
           !isNaN(parseInt(networkListenPort));
  };

  const handleNavigate = ({ detail }) => {
    const requestedStepIndex = detail.requestedStepIndex;
    // If moving forward from step 0, validate the configure step
    if (activeStepIndex === 0 && requestedStepIndex > activeStepIndex) {
      if (!isConfigureStepValid()) {
        setShowErrorText(true)
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
        submitButton: "Create network",
        optional: "optional"
      }}
      onNavigate={handleNavigate}
      onCancel={() => navigate("/networks")}
      onSubmit={handleSubmit}
      activeStepIndex={activeStepIndex}
      isLoadingNextStep={loading}
      steps={[
        {
          title: "Configure network",
          description: "Enter the network configuration details",
          isOptional: false,
          errorText: (showErrorText) ? "Please fill in all required fields" : "",
          content: (
            <Container header={<Header variant="h2">Network configuration</Header>}>
              <SpaceBetween size="l">
                <FormField
                  label="Network name"
                  description="Unique, alphanumeric, 1-64 character name for the network"
                >
                  <Input
                    value={networkName}
                    onChange={({ detail }) => setNetworkName(detail.value)}
                    placeholder="e.g., Network1"
                  />
                </FormField>

                <FormField
                  label="Network CIDR"
                  description="CIDR block for the network"
                >
                  <Input
                    value={networkCidr}
                    onChange={({ detail }) => setNetworkCidr(detail.value)}
                    placeholder="e.g., 10.100.0.1/24"
                  />
                </FormField>

                <FormField
                  label="Network listen port"
                  description="Server listen port for the network (1025-65535)"
                >
                  <Input
                    value={networkListenPort}
                    onChange={({ detail }) => setNetworkListenPort(detail.value)}
                    placeholder="e.g., 51820"
                    type="number"
                  />
                </FormField>

                <FormField
                  label="Network tag"
                  description="Optional tag for the network"
                >
                  <Input
                    value={networkTag}
                    onChange={({ detail }) => setNetworkTag(detail.value)}
                    placeholder="e.g., net1"
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
              <Container header={<Header variant="h2">Review network configuration</Header>}>
                <ColumnLayout columns={2} variant="text-grid">
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Network name",
                        value: networkName || "-"
                      },
                      {
                        label: "Network CIDR",
                        value: networkCidr || "-"
                      }
                    ]}
                  />
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Network listen port",
                        value: networkListenPort || "-"
                      },
                      {
                        label: "Network tag",
                        value: networkTag || "-"
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

export default CreateNetwork;
