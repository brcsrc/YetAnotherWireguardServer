import {
  Wizard,
  Container,
  Header,
  SpaceBetween,
  FormField,
  Input,
  ColumnLayout,
  KeyValuePairs,
  Popover,
  Icon
} from "@cloudscape-design/components";
import {useEffect, useState} from "react";
import { useNavigate, useParams } from "react-router";
import {networkClientClient, toolsClient} from "../../utils/clients";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";

const CreateClient = () => {
  const navigate = useNavigate();
  const { networkName } = useParams<{ networkName: string }>();
  const { addFlashbarItem } = useFlashbarContext();

  const [clientName, setClientName] = useState("");
  const [clientIp, setClientIp] = useState("");
  const [clientSubnetMask, setClientSubnetMask] = useState("/32");
  const [clientDns, setClientDns] = useState("");
  const [allowedIps, setAllowedIps] = useState("0.0.0.0/0");
  const [networkEndpoint, setNetworkEndpoint] = useState("");

  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [showErrorText, setShowErrorText] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setLoading(true);
    try {
      const clientCidr = `${clientIp}${clientSubnetMask}`;
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
           clientIp.trim() !== "" &&
           clientSubnetMask.trim() !== "" &&
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

  // try to help the client creation by getting the public ip and next client ip
  useEffect(() => {
    (async function (){
      try {
        const response = await toolsClient.getPublicIp()
        setNetworkEndpoint(response.publicIp)
      } catch (error) {
        const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message;
        addFlashbarItem({
          type: "error",
          header: "Failure in Get Public IP",
          content: errorMessage,
          dismissLabel: "Dismiss",
          duration: 10000
        });
      }
      try {
        const response = await networkClientClient.getNextAvailableClientAddress({
          networkName: networkName
        })
        setClientIp(response.nextAvailableAddress)
      } catch (error) {
        const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message;
        addFlashbarItem({
          type: "error",
          header: "Failure in Get Next Available Client IP",
          content: errorMessage,
          dismissLabel: "Dismiss",
          duration: 10000
        });
      }
    })()
  }, [])

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
                  label={
                    <span>
                      Client IP address / Subnet mask{" "}
                      <Popover
                          dismissButton={false}
                          position="top"
                          size="small"
                          triggerType="custom"
                          content="This is the next available IP address for this network"
                      >
                        <Icon name="status-info" variant="link" />
                      </Popover>
                    </span>
                  }
                  description="IP address and subnet mask for the client (typically /32 for single device)"
                >
                  <div style={{ display: "flex", gap: "8px" }}>
                    <div style={{ flex: "3" }}>
                      <Input
                        value={clientIp}
                        onChange={({ detail }) => setClientIp(detail.value)}
                        placeholder="e.g., 10.100.0.3"
                      />
                    </div>
                    <div style={{ flex: "1" }}>
                      <Input
                        value={clientSubnetMask}
                        onChange={({ detail }) => setClientSubnetMask(detail.value)}
                        placeholder="/32"
                      />
                    </div>
                  </div>
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
                  label={
                    <span>
                      Network endpoint{" "}
                      <Popover
                        dismissButton={false}
                        position="top"
                        size="small"
                        triggerType="custom"
                        content="Public IP reported from ifconfig.me"
                      >
                        <Icon name="status-info" variant="link" />
                      </Popover>
                    </span>
                  }
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
                        label: "Client IP address",
                        value: clientIp || "-"
                      },
                      {
                        label: "Subnet mask",
                        value: clientSubnetMask || "-"
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
