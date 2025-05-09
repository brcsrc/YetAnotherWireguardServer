import {
  Badge,
  Button,
  Container,
  Header,
  SpaceBetween,
  Table,
} from "@cloudscape-design/components";
import { Network } from "../types/network";
import Layout from "../components/Layout";

// TODO Have Dashboard Page Load List Networks API Call on load of page
const mockNetworks: Network[] = [
  {
    networkName: "Network1",
    networkCidr: "10.100.0.1/24",
    networkListenPort: 51820,
    networkPrivateKeyName: "Network1-private-key",
    networkPublicKeyName: "Network1-public-key",
    networkTag: "net1",
    networkStatus: "ACTIVE",
  },
];

// TODO: Refresh  button  to refresh List Networks API Call manually
export default function Dashboard() {
  return (
    <Layout
      breadcrumbs={[
        { text: "Home", href: "<Placeholder>" }, // TODO: Probably  make home/dashboard the same
        { text: "Dashboard", href: "/dashboard" },
      ]}
    >
      <SpaceBetween size="m">
        <Container>
          <Table
            columnDefinitions={[
              {
                id: "name",
                header: "Name",
                cell: (item) => item.networkName,
              },
              {
                id: "cidr",
                header: "CIDR",
                cell: (item) => item.networkCidr,
              },
              {
                id: "port",
                header: "Listen Port",
                cell: (item) => item.networkListenPort,
              },
              {
                id: "tag",
                header: "Tag",
                cell: (item) => item.networkTag,
              },
              {
                id: "status",
                header: "Status",
                cell: (item) => (
                  <Badge
                    color={item.networkStatus === "ACTIVE" ? "green" : "red"}
                  >
                    {item.networkStatus}
                  </Badge>
                ),
              },
            ]}
            items={mockNetworks}
            empty={
              <div style={{ textAlign: "center", padding: "20px" }}>
                No networks found.
              </div>
            }
            header={
              <Header
                counter={
                  mockNetworks.length ? `(${mockNetworks.length})` : undefined
                }
                actions={
                  <Button
                    variant="primary"
                    disabled // TODO Make Button Call Create Network API Call
                    onClick={() => alert("Create Network coming soon!")}
                  >
                    Create Network
                  </Button>
                }
              >
                Networks
              </Header>
            }
          />
        </Container>
      </SpaceBetween>
    </Layout>
  );
}
