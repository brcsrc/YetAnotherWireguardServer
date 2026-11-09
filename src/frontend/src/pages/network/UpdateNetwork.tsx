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
  Checkbox,
} from "@cloudscape-design/components";
import { useEffect, useState } from "react";
import { useNavigate, useLocation, useParams } from "react-router";
import { networkClient } from "../../api/HTTPClients";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";
import { Network, UpdateNetworkRequestNetworkStatusEnum } from "@yaws/yaws-ts-api-client";

const UpdateNetwork = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { networkName } = useParams<{ networkName: string }>();
  const { addFlashbarItem } = useFlashbarContext();

  // the network may be passed through navigation state from the detail page, otherwise it is
  // fetched so this page works on a direct link or a refresh
  const [network, setNetwork] = useState<Network | undefined>(location.state);
  const [networkTag, setNetworkTag] = useState("");
  const [networkStatus, setNetworkStatus] = useState<
    UpdateNetworkRequestNetworkStatusEnum | undefined
  >(undefined);
  const [peerIsolationEnabled, setPeerIsolationEnabled] = useState(false);
  const [keepaliveEnabled, setKeepaliveEnabled] = useState(true);
  const [persistentKeepalive, setPersistentKeepalive] = useState("25");

  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    (async function () {
      if (!network) {
        try {
          const response = await networkClient.describeNetwork({
            describeNetworkRequest: { networkName: networkName },
          });
          setNetwork(response.network);
        } catch (error) {
          const errorMessage =
            error.response?.data?.message || error.response?.data?.error || error.message;
          addFlashbarItem({
            type: "error",
            header: "Failure in DescribeNetwork",
            content: errorMessage,
            dismissLabel: "Dismiss",
            duration: 10000,
          });
        }
      }
    })();
  }, [network, networkName, addFlashbarItem]);

  // seed the form from the network once it is available
  useEffect(() => {
    if (network) {
      setNetworkTag(network.networkTag || "");
      setNetworkStatus(network.networkStatus);
      setPeerIsolationEnabled(network.peerIsolationEnabled ?? false);
      setKeepaliveEnabled((network.persistentKeepalive ?? 25) > 0);
      setPersistentKeepalive(String(network.persistentKeepalive || 25));
    }
  }, [network]);

  const handleSubmit = async () => {
    // an unloaded network means the form holds defaults rather than current values, and
    // submitting those would overwrite fields the user never saw
    if (!network) {
      addFlashbarItem({
        type: "error",
        header: "Update Network Failed",
        content: `Network "${networkName}" has not loaded yet.`,
        dismissLabel: "Dismiss",
        duration: 10000,
      });
      return;
    }

    setLoading(true);
    try {
      await networkClient.updateNetwork({
        updateNetworkRequest: {
          networkName: networkName,
          networkTag: networkTag || undefined,
          networkStatus: networkStatus,
          peerIsolationEnabled,
          persistentKeepalive: keepaliveEnabled ? parseInt(persistentKeepalive) : 0,
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
                      setNetworkStatus(
                        detail.selectedOption.value as UpdateNetworkRequestNetworkStatusEnum
                      )
                    }
                    options={statusOptions}
                  />
                </FormField>

                <FormField
                  label="Peer isolation"
                  description="When enabled, clients on this network cannot reach each other. Clients can still reach the internet and the server. This can be changed at any time without disconnecting clients."
                >
                  <Checkbox
                    checked={peerIsolationEnabled}
                    onChange={({ detail }) => setPeerIsolationEnabled(detail.checked)}
                  >
                    Enabled
                  </Checkbox>
                </FormField>

                <FormField
                  label="Persistent keepalive"
                  description="Default for clients on this network. Only applies to client configurations generated after this change; existing clients must re-download their configuration."
                >
                  <SpaceBetween direction="horizontal" size="s" alignItems="center">
                    <Checkbox
                      checked={keepaliveEnabled}
                      onChange={({ detail }) => setKeepaliveEnabled(detail.checked)}
                    >
                      Enabled
                    </Checkbox>
                    <div style={{ width: "8em" }}>
                      <Input
                        type="number"
                        value={persistentKeepalive}
                        onChange={({ detail }) => setPersistentKeepalive(detail.value)}
                        placeholder="25"
                        disabled={!keepaliveEnabled}
                      />
                    </div>
                  </SpaceBetween>
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
                      {
                        label: "Persistent keepalive",
                        value: keepaliveEnabled ? `${persistentKeepalive}s` : "Disabled",
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
