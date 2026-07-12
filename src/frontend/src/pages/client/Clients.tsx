import { SpaceBetween } from "@cloudscape-design/components";
import ClientsTable from "./components/ClientsTable";
import { Network } from "yaws-ts-api-client/dist/types/models/Network";

interface ClientsProps {
  network: Network;
}

const Clients = ({ network }: ClientsProps) => {
  return (
    <SpaceBetween size="l">
      <ClientsTable network={network} showCreateButton={true} />
    </SpaceBetween>
  );
};

export default Clients;
