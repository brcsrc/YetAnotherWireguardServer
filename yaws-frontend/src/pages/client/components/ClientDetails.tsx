import {
  Container,
  Header,
  ColumnLayout,
  KeyValuePairs,
  SpaceBetween,
  Toggle,
} from "@cloudscape-design/components";
import { Client } from "@yaws/yaws-ts-api-client";
import { PublicKeyDisplay } from "../../../components/misc/PublicKeyDisplay";
import ClientConfigQRCode from "./ClientConfigQRCode";
import { useState } from "react";

interface ClientDetailsProps {
  client: Client | undefined;
  networkName: string | undefined;
  clientName: string | undefined;
}

const ClientDetails = (props: ClientDetailsProps): JSX.Element => {
  const { client, networkName, clientName } = props;
  const [showQR, setShowQR] = useState(false);

  return (
    <Container header={<Header variant="h2">Client details</Header>}>
        <ColumnLayout columns={3} variant="text-grid">
          <KeyValuePairs
            columns={1}
            items={[
              {
                label: "Name",
                value: client?.clientName || "-",
              },
              {
                label: "Client Public Key",
                value: <PublicKeyDisplay publicKey={client?.clientPublicKeyValue} />,
              },
              {
                label: "CIDR",
                value: client?.clientCidr || "-",
              },
              {
                label: "Tag",
                value: client?.clientTag || "-",
              },
            ]}
          />
          <KeyValuePairs
            columns={1}
            items={[
              {
                label: "DNS",
                value: client?.clientDns || "-",
              },
              {
                label: "Allowed IPs",
                value: client?.allowedIps || "-",
              },
              {
                label: "Network Endpoint",
                value: client?.networkEndpoint || "-",
              },
              {
                label: "Network Listen Port",
                value: client?.networkListenPort?.toString() || "-",
              },
            ]}
          />
          <KeyValuePairs
            columns={1}
            items={[
              {
                label: "Configuration QR Code",
                value:
                  networkName && clientName ? (
                    <SpaceBetween size="s">
                      <Toggle checked={showQR} onChange={({ detail }) => setShowQR(detail.checked)}>
                        Show QR Code
                      </Toggle>
                      <ClientConfigQRCode
                        networkName={networkName}
                        clientName={clientName}
                        blur={!showQR}
                      />
                    </SpaceBetween>
                  ) : (
                    "-"
                  ),
              },
            ]}
          />
        </ColumnLayout>
    </Container>
  );
};

export default ClientDetails;
