import {
  Wizard,
  Container,
  Header,
  SpaceBetween,
  FormField,
  Input,
  Select,
  ColumnLayout,
  KeyValuePairs,
  Toggle,
} from "@cloudscape-design/components";
import { useState } from "react";
import { useNavigate, useLocation, useParams } from "react-router";
import { networkClient } from "../../api/HTTPClients";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";
import { Network, UpdateNetworkRequestNetworkStatusEnum } from "@yaws/yaws-ts-api-client";

const UpdateNetwork = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { networkName } = useParams<{ networkName: string }>();
  const { addFlashbarItem } = useFlashbarContext();

  const network: Network = location.state;

  const [networkTag, setNetworkTag] = useState(network?.networkTag || "");
  const [networkStatus, setNetworkStatus] = useState(network?.networkStatus);
  const [peerIsolationEnabled, setPeerIsolationEnabled] = useState(
    network?.peerIsolationEnabled ?? false
  );

  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setLoading(true);
    try {
      await networkClient.updateNetwork({
        updateNetworkRequest: {
          networkName: networkName,
          networkTag: networkTag || undefined,
          networkStatus: networkStatus,
          peerIsolationEnabled,
        },
      });
      addFlashbarItem({
        type: "success",
        header: "Network Updated",
        content: `Network "${networkName}" was updated successfully.`,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
      navigate(`/networks/${networkName}`);
    } catch (error) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Update Network Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 10000,
      });
    } finally {
      setLoading(false);
    }
  };

  const statusOptions = [
    { label: "ACTIVE", value: UpdateNetworkRequestNetworkStatusEnum.Active },
    { label: "INACTIVE", value: UpdateNetworkRequestNetworkStatusEnum.Inactive },
  ];

  return (
    <Wizard
      i18nStrings={{
        stepNumberLabel: (stepNumber) => `Step ${stepNumber}`,
        collapsedStepsLabel: (stepNumber, stepsCount) => `Step ${stepNumber} of ${stepsCount}`,
        cancelButton: "Cancel",
        previousButton: "Previous",
        nextButton: "Next",
        submitButton: "Update network",
        optional: "optional",
      }}
      onNavigate={({ detail }) => setActiveStepIndex(detail.requestedStepIndex)}
      onCancel={() => navigate(`/networks/${networkName}`)}
      onSubmit={handleSubmit}
      activeStepIndex={activeStepIndex}
      isLoadingNextStep={loading}
      steps={[
        {
          title: "Configure network",
          description: "Update the network configuration",
          content: (
            <Container header={<Header variant="h2">Network configuration</Header>}>
              <SpaceBetween size="l">
                <FormField label="Network name" description="This field cannot be updated">
                  <Input value={network?.networkName || ""} disabled={true} />
                </FormField>

                <FormField label="Network CIDR" description="This field cannot be updated">
                  <Input value={network?.networkCidr || ""} disabled={true} />
                </FormField>

                <FormField label="Network listen port" description="This field cannot be updated">
                  <Input value={network?.networkListenPort?.toString() || ""} disabled={true} />
                </FormField>

                <FormField label="Network tag" description="Optional tag for the network">
                  <Input
                    value={networkTag}
                    onChange={({ detail }) => setNetworkTag(detail.value)}
                    placeholder="e.g., net1"
                  />
                </FormField>

                <FormField label="Network status" description="Set the network status">
                  <Select
                    selectedOption={
                      statusOptions.find((opt) => opt.value === networkStatus) || null
                    }
                    onChange={({ detail }) =>
                      setNetworkStatus(detail.selectedOption.value as NetworkStatusEnum)
                    }
                    options={statusOptions}
                  />
                </FormField>

                <FormField
                  label="Peer isolation"
                  description="When enabled, clients on this network cannot reach each other. Clients can still reach the internet and the server. Changing this does not disconnect any client."
                >
                  <Toggle
                    checked={peerIsolationEnabled}
                    onChange={({ detail }) => setPeerIsolationEnabled(detail.checked)}
                  >
                    Prevent clients from reaching each other
                  </Toggle>
                </FormField>
              </SpaceBetween>
            </Container>
          ),
        },
        {
          title: "Review and update",
          content: (
            <SpaceBetween size="l">
              <Container header={<Header variant="h2">Review network configuration</Header>}>
                <ColumnLayout columns={2} variant="text-grid">
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Network name",
                        value: network?.networkName || "-",
                      },
                      {
                        label: "Network CIDR",
                        value: network?.networkCidr || "-",
                      },
                      {
                        label: "Network listen port",
                        value: network?.networkListenPort?.toString() || "-",
                      },
                    ]}
                  />
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Network tag",
                        value: networkTag || "-",
                      },
                      {
                        label: "Network status",
                        value: networkStatus || "-",
                      },
                      {
                        label: "Peer isolation",
                        value: peerIsolationEnabled ? "Enabled" : "Disabled",
                      },
                    ]}
                  />
                </ColumnLayout>
              </Container>
            </SpaceBetween>
          ),
        },
      ]}
    />
  );
};

export default UpdateNetwork;
