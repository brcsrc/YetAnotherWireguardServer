import {
  Wizard,
  Container,
  Header,
  SpaceBetween,
  FormField,
  Input,
  ColumnLayout,
  KeyValuePairs,
  Toggle,
} from "@cloudscape-design/components";
import { useEffect, useState } from "react";
import { useNavigate, useLocation, useParams } from "react-router";
import { networkClientClient } from "../../api/HTTPClients";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";
import { Client } from "@yaws/yaws-ts-api-client";

const UpdateClient = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { networkName, clientName } = useParams<{ networkName: string; clientName: string }>();
  const { addFlashbarItem } = useFlashbarContext();

  // the client may be passed through navigation state from the detail page, otherwise it is
  // fetched so this page works on a direct link or a refresh
  const [client, setClient] = useState<Client | undefined>(location.state);
  const [clientTag, setClientTag] = useState("");
  const [peerIsolationEnabled, setPeerIsolationEnabled] = useState(false);

  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    (async function () {
      if (!client) {
        try {
          const response = await networkClientClient.describeNetworkClient({
            networkName: networkName,
            clientName: clientName,
          });
          setClient(response.client);
        } catch (error) {
          const errorMessage =
            error.response?.data?.message || error.response?.data?.error || error.message;
          addFlashbarItem({
            type: "error",
            header: "Failure in DescribeNetworkClient",
            content: errorMessage,
            dismissLabel: "Dismiss",
            duration: 10000,
          });
        }
      }
    })();
  }, [client, networkName, clientName, addFlashbarItem]);

  // seed the form from the client once it is available
  useEffect(() => {
    if (client) {
      setClientTag(client.clientTag || "");
      setPeerIsolationEnabled(client.peerIsolationEnabled ?? false);
    }
  }, [client]);

  const handleSubmit = async () => {
    setLoading(true);
    try {
      await networkClientClient.updateNetworkClient({
        updateNetworkClientRequest: {
          networkName: networkName,
          clientName: clientName,
          clientTag: clientTag || undefined,
          peerIsolationEnabled,
        },
      });
      addFlashbarItem({
        type: "success",
        header: "Client Updated",
        content: `Client "${clientName}" was updated successfully.`,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
      navigate(`/networks/${networkName}/clients/${clientName}`);
    } catch (error) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Update Client Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 10000,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Wizard
      i18nStrings={{
        stepNumberLabel: (stepNumber) => `Step ${stepNumber}`,
        collapsedStepsLabel: (stepNumber, stepsCount) => `Step ${stepNumber} of ${stepsCount}`,
        cancelButton: "Cancel",
        previousButton: "Previous",
        nextButton: "Next",
        submitButton: "Update client",
        optional: "optional",
      }}
      onNavigate={({ detail }) => setActiveStepIndex(detail.requestedStepIndex)}
      onCancel={() => navigate(`/networks/${networkName}/clients/${clientName}`)}
      onSubmit={handleSubmit}
      activeStepIndex={activeStepIndex}
      isLoadingNextStep={loading}
      steps={[
        {
          title: "Configure client",
          description: "Update the client configuration",
          content: (
            <Container header={<Header variant="h2">Client configuration</Header>}>
              <SpaceBetween size="l">
                <FormField label="Client name" description="This field cannot be updated">
                  <Input value={client?.clientName || ""} disabled={true} />
                </FormField>

                <FormField label="Client CIDR" description="This field cannot be updated">
                  <Input value={client?.clientCidr || ""} disabled={true} />
                </FormField>

                <FormField label="Client DNS" description="This field cannot be updated">
                  <Input value={client?.clientDns || ""} disabled={true} />
                </FormField>

                <FormField label="Client tag" description="Optional tag for the client">
                  <Input
                    value={clientTag}
                    onChange={({ detail }) => setClientTag(detail.value)}
                    placeholder="e.g., client1"
                  />
                </FormField>

                <FormField
                  label="Peer isolation"
                  description="When enabled, this client cannot reach any other client on the network, and no other client can reach it. Its traffic still routes through the server to the internet. Changing this does not disconnect any client."
                >
                  <Toggle
                    checked={peerIsolationEnabled}
                    onChange={({ detail }) => setPeerIsolationEnabled(detail.checked)}
                  >
                    Isolate this client from other peers
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
              <Container header={<Header variant="h2">Review client configuration</Header>}>
                <ColumnLayout columns={2} variant="text-grid">
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Network name",
                        value: networkName || "-",
                      },
                      {
                        label: "Client name",
                        value: client?.clientName || "-",
                      },
                      {
                        label: "Client CIDR",
                        value: client?.clientCidr || "-",
                      },
                    ]}
                  />
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Client tag",
                        value: clientTag || "-",
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

export default UpdateClient;
