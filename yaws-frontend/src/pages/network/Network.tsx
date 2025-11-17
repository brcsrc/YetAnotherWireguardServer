import {
  Button,
  Container,
  Header,
  SpaceBetween,
  ColumnLayout,
  KeyValuePairs,
  Tabs,
  Badge,
  Modal,
  Box
} from "@cloudscape-design/components";
import { useParams, useLocation, useNavigate } from "react-router";
import {useEffect, useState} from "react";
import {Network} from "yaws-ts-api-client/dist/types/models/Network";
import {networkClient} from "../../utils/clients";
import {useFlashbarContext} from "../../context/FlashbarContextProvider";
import Clients from "../client/Clients";

const Network = () => {
  const { networkName } = useParams<{ networkName: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const initialNetwork = location.state;
  const [network, setNetwork] = useState<Network>(initialNetwork)
  const [loading, setLoading] = useState(!network)
  const [deleting, setDeleting] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)

  const { addFlashbarItem } = useFlashbarContext()

  useEffect(() => {
      (async function() {
          if (!network) {
              try {
                  const response = await networkClient.describeNetwork({networkName: networkName})
                  setNetwork(response)
              } catch (error) {
                  const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message
                  addFlashbarItem({
                      type: "error",
                      header: "Failure in DescribeNetwork",
                      content: errorMessage,
                      dismissLabel: "Dismiss",
                      duration: 10000
                  })
              } finally {
                  setLoading(false)
              }
          }
      })()
  }, [location, network])

  const handleDeleteNetworkClick = async () => {
    setDeleting(true);
    setShowDeleteModal(false);
    try {
      await networkClient.deleteNetwork({networkName: networkName});
      addFlashbarItem({
        type: "success",
        header: "Network Deleted",
        content: `Network "${networkName}" was deleted successfully.`,
        dismissLabel: "Dismiss",
        duration: 5000
      });
      navigate("/networks");
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message;
      addFlashbarItem({
        type: "error",
        header: "Delete Network Failed",
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
        header="Delete network"
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
                onClick={handleDeleteNetworkClick}
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
            Permanently delete network <strong>{networkName}</strong>? This action cannot be undone.
          </Box>
        </SpaceBetween>
      </Modal>

      <Header
        variant="h1"
        actions={
          <SpaceBetween direction="horizontal" size="xs">
            <Button
              variant="primary"
              onClick={() => navigate(`/networks/${networkName}/update`, { state: network })}
            >
              Update Network
            </Button>
            <Button
              variant="normal"
              onClick={() => setShowDeleteModal(true)}
              disabled={deleting}
            >
              Delete Network
            </Button>
          </SpaceBetween>
        }
      >
        {networkName}
      </Header>

      <Container header={<Header variant="h2">Network details</Header>}>
        <ColumnLayout columns={2} variant="text-grid">
          <KeyValuePairs
            columns={1}
            items={[
              {
                label: "Name",
                value: network?.networkName || "-"
              },
              {
                label: "CIDR",
                value: network?.networkCidr || "-"
              },
              {
                label: "Listen Port",
                value: network?.networkListenPort || "-"
              }
            ]}
          />
          <KeyValuePairs
            columns={1}
            items={[
              {
                label: "Tag",
                value: network?.networkTag || "-"
              },
              {
                label: "Status",
                value: network?.networkStatus ? (
                  <Badge color={network.networkStatus === "ACTIVE" ? "green" : "red"}>
                    {network.networkStatus}
                  </Badge>
                ) : "-"
              }
            ]}
          />
        </ColumnLayout>
      </Container>

      <Tabs
        tabs={[
          {
            id: "clients",
            label: "Clients",
            content: <Clients networkName={networkName} />
          }
        ]}
      />
    </SpaceBetween>
  );
}

export default Network
