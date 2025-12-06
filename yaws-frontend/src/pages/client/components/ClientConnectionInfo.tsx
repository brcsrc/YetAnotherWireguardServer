import { Container, Header, ColumnLayout, KeyValuePairs, Badge } from "@cloudscape-design/components";
import { useEffect, useState, useRef } from "react";
import { streamClientConnectionInfo, ClientConnectionInfo } from "../../../api/SSEClients";
import { useFlashbarContext } from "../../../context/FlashbarContextProvider";

interface ClientConnectionInfoProps {
  clientPublicKeyValue: string | undefined;
}

const ClientConnectionInfoComponent = (props: ClientConnectionInfoProps): JSX.Element => {
  const { clientPublicKeyValue } = props;

  const [connectionInfo, setConnectionInfo] = useState<ClientConnectionInfo | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  const { addFlashbarItem } = useFlashbarContext();

  useEffect(() => {
    if (!clientPublicKeyValue) {
      return;
    }

    const eventSource = streamClientConnectionInfo(
      { clientPublicKeyValue },
      {
        onMessage: (event) => {
          if (event.type === "client-info-update") {
            const data: ClientConnectionInfo = event.data;
            setConnectionInfo(data);
          } else if (event.type === "error") {
            addFlashbarItem({
              type: "error",
              header: "SSE Error",
              content: event.data,
              dismissLabel: "Dismiss",
              duration: 10000,
            });
          }
        },
        onError: () => {
          eventSource.close();
        },
      }
    );

    eventSourceRef.current = eventSource;

    return () => {
      eventSource.close();
    };
  }, [clientPublicKeyValue]);

  const formatBytes = (bytes: number | undefined): string => {
    if (bytes === undefined || bytes === null) return "-";
    if (bytes === 0) return "0 B";

    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  const formatTimestamp = (epochSeconds: number | undefined): JSX.Element => {
    if (epochSeconds === undefined || epochSeconds === null) {
      return <Badge color="grey">-</Badge>;
    }
    if (epochSeconds === 0) {
      return <Badge color="grey">Never</Badge>;
    }

    const date = new Date(epochSeconds * 1000);
    const now = Date.now();
    const diff = now - date.getTime();

    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    let timeText = "";
    let color: "green" | "blue" | "grey" = "grey";

    if (days > 0) {
      timeText = `${days}d ago`;
      color = "grey";
    } else if (hours > 0) {
      timeText = `${hours}h ago`;
      color = "blue";
    } else if (minutes > 0) {
      timeText = `${minutes}m ago`;
      color = "green";
    } else {
      timeText = `${seconds}s ago`;
      color = "green";
    }

    return <Badge color={color}>{timeText}</Badge>;
  };

  return (
    <Container header={<Header variant="h2">Connection Info</Header>}>
        <ColumnLayout columns={2} variant="text-grid">
          <KeyValuePairs
            columns={1}
            items={[
              {
                label: "Endpoint",
                value: connectionInfo?.endpoint || "-",
              },
              {
                label: "Allowed IPs",
                value: connectionInfo?.allowedIps || "-",
              },
              {
                label: "Latest Handshake",
                value: formatTimestamp(connectionInfo?.latestHandshakeEpochSeconds),
              },
              {
                label: "Persistent Keepalive",
                value:
                  connectionInfo?.persistentKeepalive !== null &&
                  connectionInfo?.persistentKeepalive !== undefined
                    ? `${connectionInfo.persistentKeepalive}s`
                    : "-",
              },
            ]}
          />
          <KeyValuePairs
            columns={1}
            items={[
              {
                label: "Bytes Received",
                value: formatBytes(connectionInfo?.bytesReceived),
              },
              {
                label: "Bytes Sent",
                value: formatBytes(connectionInfo?.bytesSent),
              },
              {
                label: "Preshared Key",
                value: connectionInfo?.presharedKey === "(none)" ? "No" : "Yes",
              },
            ]}
          />
        </ColumnLayout>
    </Container>
  );
};

export default ClientConnectionInfoComponent;
