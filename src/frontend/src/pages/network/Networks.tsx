import { Header, SpaceBetween, Button } from "@cloudscape-design/components";
import { useNavigate } from "react-router";
import NetworksTable from "./components/NetworksTable";
import { useFlashbarContext } from "../../context/FlashbarContextProvider";

export default function Networks() {
  const navigate = useNavigate();

  const { addFlashbarItem } = useFlashbarContext();

  return (
    <SpaceBetween size="l">
      <Header
        variant="h1"
        actions={
          <Button variant="primary" onClick={() => navigate("/networks/create")}>
            Create Network
          </Button>
        }
      >
        Networks
      </Header>
      <NetworksTable />
    </SpaceBetween>
  );
}
