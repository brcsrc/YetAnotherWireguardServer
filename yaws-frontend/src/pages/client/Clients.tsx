import { SpaceBetween } from "@cloudscape-design/components";
import ClientsTable from "./components/ClientsTable";

interface ClientsProps {
  networkName: string;
}

const Clients = ({ networkName }: ClientsProps) => {
  return (
    <SpaceBetween size="l">
      <ClientsTable networkName={networkName} showCreateButton={true}/>
    </SpaceBetween>
  );
}

export default Clients
