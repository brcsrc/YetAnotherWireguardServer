import {
  Button,
  Container,
  Header,
  SpaceBetween,
  ColumnLayout,
  KeyValuePairs,
  Modal,
  Box
} from "@cloudscape-design/components";
import { useParams, useLocation, useNavigate } from "react-router";
import { useEffect, useState } from "react";
import { Client } from "@yaws/yaws-ts-api-client";
import { networkClientClient } from "../../utils/clients";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";

const Client = () => {
  const { networkName, clientName } = useParams<{ networkName: string; clientName: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const initialClient = location.state;
  const [client, setClient] = useState<Client>(initialClient);
  const [loading, setLoading] = useState(!client);
  const [deleting, setDeleting] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const { addFlashbarItem } = useFlashbarContext();

  useEffect(() => {
    (async function() {
      if (!client) {
        try {
          const response = await networkClientClient.describeNetworkClient({
            networkName: networkName,
            clientName: clientName
          });
          setClient(response.client);
        } catch (error) {
          const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message;
          addFlashbarItem({
            type: "error",
            header: "Failure in DescribeNetworkClient",
            content: errorMessage,
            dismissLabel: "Dismiss",
            duration: 10000
          });
        } finally {
          setLoading(false);
        }
      }
    })();
  }, [location, client, networkName, clientName]);

  const handleDownloadClientClick = async () => {
    try {
      const response = await networkClientClient.getNetworkClientConfigFile({
        networkName: networkName,
        clientName: clientName
      });

      // Create a blob from the response (response is already a Blob)
      const blob = new Blob([response], { type: 'text/plain' });

      // Create a download link and trigger it
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${clientName}.conf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Download Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 5000
      });
    }
  };

  const handleDeleteClientClick = async () => {
    setDeleting(true);
    setShowDeleteModal(false);
    try {
      await networkClientClient.deleteNetworkClient({
        networkName: networkName,
        clientName: clientName
      });
      addFlashbarItem({
        type: "success",
        header: "Client Deleted",
        content: `Client "${clientName}" was deleted successfully.`,
        dismissLabel: "Dismiss",
        duration: 5000
      });
      navigate(`/networks/${networkName}`);
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Delete Client Failed",
        content: errorMessage,
        dismissLabel: "Dismiss",
        duration: 5000
      });
    } finally {
      setDeleting(false);
    }
  };

  return (
    <SpaceBetween size="l">
      <Modal
        visible={showDeleteModal}
        onDismiss={() => setShowDeleteModal(false)}
        header="Delete client"
        footer={
          <Box float="right">
            <SpaceBetween direction="horizontal" size="xs">
              <Button
                variant="link"
                onClick={() => setShowDeleteModal(false)}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleDeleteClientClick}
                disabled={deleting}
              >
                {deleting ? "Deleting..." : "Delete"}
              </Button>
            </SpaceBetween>
          </Box>
        }
      >
        <SpaceBetween size="m">
          <Box variant="span">
            Permanently delete client <strong>{clientName}</strong>? This action cannot be undone.
          </Box>
        </SpaceBetween>
      </Modal>

      <Header
        variant="h1"
        actions={
          <SpaceBetween direction="horizontal" size="xs">
            <Button
              variant="primary"
              iconName="download"
              onClick={handleDownloadClientClick}
            >
              Download Client Config
            </Button>
            <Button
              variant="normal"
              onClick={() => setShowDeleteModal(true)}
              disabled={deleting}
            >
              Delete Client
            </Button>
          </SpaceBetween>
        }
      >
        {clientName}
      </Header>

      <Container header={<Header variant="h2">Client details</Header>}>
        <ColumnLayout columns={2} variant="text-grid">
          <KeyValuePairs
            columns={1}
            items={[
              {
                label: "Name",
                value: client?.clientName || "-"
              },
                {
                    label: "Client Private Key Name",
                    value: client?.clientPrivateKeyName || "-"
                },
                {
                    label: "Client Public Key Name",
                    value: client?.clientPublicKeyName || "-"
                },
                {
                    label: "Network Public Key Name",
                    value: client?.networkPublicKeyName || "-"
                },
                {
                    label: "Tag",
                    value: client?.clientTag || "-"
                },
            ]}
          />
          <KeyValuePairs
            columns={1}
            items={[
                {
                    label: "CIDR",
                    value: client?.clientCidr || "-"
                },
                {
                    label: "DNS",
                    value: client?.clientDns || "-"
                },
                {
                    label: "Allowed IPs",
                    value: client?.allowedIps || "-"
                },
                {
                    label: "Network Endpoint",
                    value: client?.networkEndpoint || "-"
                },
                {
                    label: "Network Listen Port",
                    value: client?.networkListenPort?.toString() || "-"
                }
            ]}
          />
        </ColumnLayout>
      </Container>
    </SpaceBetween>
  );
};

export default Client;
