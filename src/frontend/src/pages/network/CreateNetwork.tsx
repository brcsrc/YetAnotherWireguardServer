import {
  Wizard,
  Container,
  Header,
  SpaceBetween,
  FormField,
  Input,
  ColumnLayout,
  KeyValuePairs,
} from "@cloudscape-design/components";
import { useState } from "react";
import { useNavigate } from "react-router";
import { networkClient } from "../../api/HTTPClients";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";
import { useDebouncedValue } from "../../utils/debounce";

// Regex patterns for validity borrowed from backend Constants.java
const NETWORK_NAME_REGEX = /^[a-zA-Z0-9_-]{4,15}$/;
const NETWORK_IP_REGEX =
  /^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$/;
const NETWORK_TAG_REGEX = /^[a-zA-Z0-9_-]{1,64}$/;

interface NetworkFieldValidity {
  isNetworkNameInvalid: boolean;
  isNetworkIpInvalid: boolean;
  isNetworkIpHostInvalid: boolean;
  isSubnetMaskInvalid: boolean;
  isListenPortInvalid: boolean;
  isNetworkTagInvalid: boolean;
}

function validateNetworkFields(
  networkName: string,
  networkIp: string,
  networkSubnetMask: string,
  networkListenPort: string,
  networkTag: string
): NetworkFieldValidity {
  const isNetworkNameInvalid = networkName.length > 0 && !NETWORK_NAME_REGEX.test(networkName);

  const isNetworkIpInvalid = networkIp.length > 0 && !NETWORK_IP_REGEX.test(networkIp);

  const networkInterfaceOctet = parseInt(networkIp.split(".")[3]);
  const isNetworkIpHostInvalid =
    networkIp.length > 0 &&
    !isNetworkIpInvalid &&
    (networkInterfaceOctet === 0 || networkInterfaceOctet > 254);

  const subnetMaskValue = parseInt(networkSubnetMask.replace("/", ""));
  const isSubnetMaskInvalid =
    networkSubnetMask.length > 0 &&
    (isNaN(subnetMaskValue) || subnetMaskValue < 24 || subnetMaskValue > 32);

  const listenPortValue = parseInt(networkListenPort);
  const isListenPortInvalid =
    networkListenPort.length > 0 &&
    (isNaN(listenPortValue) || listenPortValue < 1025 || listenPortValue > 65535);

  const isNetworkTagInvalid = networkTag.length > 0 && !NETWORK_TAG_REGEX.test(networkTag);

  return {
    isNetworkNameInvalid,
    isNetworkIpInvalid,
    isNetworkIpHostInvalid,
    isSubnetMaskInvalid,
    isListenPortInvalid,
    isNetworkTagInvalid,
  };
}

const CreateNetwork = () => {
  const navigate = useNavigate();
  const { addFlashbarItem } = useFlashbarContext();

  const [networkName, setNetworkName] = useState("");
  const [networkIp, setNetworkIp] = useState("");
  const [networkSubnetMask, setNetworkSubnetMask] = useState("/24");
  const [networkListenPort, setNetworkListenPort] = useState("");
  const [networkTag, setNetworkTag] = useState("");

  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [showErrorText, setShowErrorText] = useState(false);
  const [loading, setLoading] = useState(false);

  // Live validity gates whether the wizard can advance to the next step -
  // this must not be debounced, otherwise a user could click "Next" during
  // the debounce window before an error has had a chance to show up.
  const liveValidity = validateNetworkFields(
    networkName,
    networkIp,
    networkSubnetMask,
    networkListenPort,
    networkTag
  );

  // Debounced validity drives the displayed error state, so errors don't
  // flash on every keystroke while the user is still typing a field.
  const {
    isNetworkNameInvalid,
    isNetworkIpInvalid,
    isNetworkIpHostInvalid,
    isSubnetMaskInvalid,
    isListenPortInvalid,
    isNetworkTagInvalid,
  } = validateNetworkFields(
    useDebouncedValue(networkName),
    useDebouncedValue(networkIp),
    useDebouncedValue(networkSubnetMask),
    useDebouncedValue(networkListenPort),
    useDebouncedValue(networkTag)
  );

  const handleSubmit = async () => {
    setLoading(true);
    try {
      const networkCidr = `${networkIp}${networkSubnetMask}`;
      await networkClient.createNetwork({
        network: {
          networkName,
          networkCidr,
          networkListenPort: parseInt(networkListenPort),
          networkTag: networkTag || undefined,
        },
      });
      addFlashbarItem({
        type: "success",
        header: "Network Created",
        content: `Network "${networkName}" was created successfully.`,
        dismissLabel: "Dismiss",
        duration: 5000,
      });
      navigate("/networks");
    } catch (error) {
      const errorMessage =
        error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Create Network Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 10000,
      });
    } finally {
      setLoading(false);
    }
  };

  const isConfigureStepValid = () => {
    return (
      networkName.trim() !== "" &&
      networkIp.trim() !== "" &&
      networkSubnetMask.trim() !== "" &&
      networkListenPort.trim() !== "" &&
      !liveValidity.isNetworkNameInvalid &&
      !liveValidity.isNetworkIpInvalid &&
      !liveValidity.isNetworkIpHostInvalid &&
      !liveValidity.isSubnetMaskInvalid &&
      !liveValidity.isListenPortInvalid &&
      !liveValidity.isNetworkTagInvalid
    );
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
        stepNumberLabel: (stepNumber) => `Step ${stepNumber}`,
        collapsedStepsLabel: (stepNumber, stepsCount) => `Step ${stepNumber} of ${stepsCount}`,
        cancelButton: "Cancel",
        previousButton: "Previous",
        nextButton: "Next",
        submitButton: "Create network",
        optional: "optional",
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
          errorText: showErrorText ? "Please fill in all required fields" : "",
          content: (
            <Container header={<Header variant="h2">Network configuration</Header>}>
              <SpaceBetween size="l">
                <FormField
                  label="Network name"
                  description="Unique, alphanumeric, 4-15 character name for the network"
                  errorText={
                    isNetworkNameInvalid
                      ? "Network name must be 4-15 alphanumeric characters, dashes, or underscores"
                      : undefined
                  }
                >
                  <Input
                    value={networkName}
                    onChange={({ detail }) => setNetworkName(detail.value)}
                    placeholder="e.g., Network1"
                    invalid={isNetworkNameInvalid}
                  />
                </FormField>

                <FormField
                  label="Network IP address / Subnet mask"
                  description="IP address and subnet mask for the network (typically /24)"
                  errorText={
                    isNetworkIpInvalid
                      ? "Must be a valid IPv4 address"
                      : isNetworkIpHostInvalid
                        ? "Host octet must be between .1 and .254"
                        : isSubnetMaskInvalid
                          ? "Subnet mask must be between /24 and /32"
                          : undefined
                  }
                >
                  <div style={{ display: "flex", gap: "8px" }}>
                    <div style={{ flex: "3" }}>
                      <Input
                        value={networkIp}
                        onChange={({ detail }) => setNetworkIp(detail.value)}
                        placeholder="e.g., 10.100.0.1"
                        invalid={isNetworkIpInvalid || isNetworkIpHostInvalid}
                      />
                    </div>
                    <div style={{ flex: "1" }}>
                      <Input
                        value={networkSubnetMask}
                        onChange={({ detail }) => setNetworkSubnetMask(detail.value)}
                        placeholder="/24"
                        invalid={isSubnetMaskInvalid}
                      />
                    </div>
                  </div>
                </FormField>

                <FormField
                  label="Network listen port"
                  description="Server listen port for the network (1025-65535)"
                  errorText={
                    isListenPortInvalid ? "Listen port must be between 1025 and 65535" : undefined
                  }
                >
                  <Input
                    value={networkListenPort}
                    onChange={({ detail }) => setNetworkListenPort(detail.value)}
                    placeholder="e.g., 51820"
                    type="number"
                    invalid={isListenPortInvalid}
                  />
                </FormField>

                <FormField
                  label="Network tag"
                  description="Optional tag for the network"
                  errorText={
                    isNetworkTagInvalid
                      ? "Tag must be alphanumeric with dashes or underscores, max 64 characters"
                      : undefined
                  }
                >
                  <Input
                    value={networkTag}
                    onChange={({ detail }) => setNetworkTag(detail.value)}
                    placeholder="e.g., net1"
                    invalid={isNetworkTagInvalid}
                  />
                </FormField>
              </SpaceBetween>
            </Container>
          ),
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
                        value: networkName || "-",
                      },
                      {
                        label: "Network IP address",
                        value: networkIp || "-",
                      },
                      {
                        label: "Subnet mask",
                        value: networkSubnetMask || "-",
                      },
                    ]}
                  />
                  <KeyValuePairs
                    columns={1}
                    items={[
                      {
                        label: "Network listen port",
                        value: networkListenPort || "-",
                      },
                      {
                        label: "Network tag",
                        value: networkTag || "-",
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

export default CreateNetwork;
