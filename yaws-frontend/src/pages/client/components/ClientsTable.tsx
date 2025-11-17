import {
  Button,
  CollectionPreferences,
  Header,
  PropertyFilter,
  Table,
} from "@cloudscape-design/components";
import { useCollection } from "@cloudscape-design/collection-hooks";
import { NetworkClient } from "yaws-ts-api-client/dist/types/index";
import { networkClientClient } from "../../../utils/clients"
import {getClientsTableColDef, CLIENTS_TABLE_FILTER_PROPS, CLIENTS_TABLE_PREFERENCES} from "./clientsTableUtilities";
import * as React from "react";
import {useEffect, useState} from "react";
import {useFlashbarContext} from "../../../context/FlashbarContextProvider";
import { useNavigate } from "react-router";
import {Network} from "yaws-ts-api-client/dist/types/models/Network";

interface ClientsTableProps {
  network: Network;
  showCreateButton?: boolean
}

const ClientsTable = ({ network, showCreateButton }: ClientsTableProps): React.ReactNode => {
  const navigate = useNavigate();
  const [clients, setClients] = useState<NetworkClient[]>([])
  const [loading, setLoading] = useState(true)
  const [preferences, setPreferences] = useState({
    pageSize: 10,
    visibleContent: ["name", "cidr", "dns", "allowedIps", "tag"]
  })

  const { addFlashbarItem } = useFlashbarContext()

  useEffect(() => {
      if (!network) {
          setLoading(false)
          return
      }
      (async function () {
          try {
              let page: number | undefined = 0;
              while (page !== undefined) {
                  const response = await networkClientClient.listNetworkClients({
                      listNetworkClientsRequest: {
                          networkName: network.networkName,
                          page: page
                      },
                  })
                  setClients((prevState) => [...prevState, ...response.clients])
                  page = response.nextPage
              }
          } catch (error) {
              const errorMessage = error.response?.data?.message || error.response?.data?.error || error.message
              addFlashbarItem({
                  type: "error",
                  header: "Failure in ListNetworkClients",
                  content: errorMessage,
                  dismissLabel: "Dismiss",
                  duration: 5000
              })
          } finally {
              setLoading(false)
          }
      })()
  }, [network])

  const { items, collectionProps, propertyFilterProps } = useCollection(clients, {
    sorting: {},
    filtering: {
      empty: (
        <div style={{ textAlign: "center", padding: "20px" }}>
          No clients found.
        </div>
      ),
      noMatch: (
        <div style={{ textAlign: "center", padding: "20px" }}>
          No matches found.
        </div>
      ),
    },
    propertyFiltering: {
      filteringProperties: CLIENTS_TABLE_FILTER_PROPS.filteringProperties,
    },
    pagination: { pageSize: preferences.pageSize },
  });

  return (
      <Table
          {...collectionProps}
          filter={
            <PropertyFilter
              {...propertyFilterProps}
              {...CLIENTS_TABLE_FILTER_PROPS}
            />
          }
          header={
              <Header
                  counter={
                      clients.length ? `(${clients.length})` : undefined
                  }
                  actions={(showCreateButton) ? (<Button
                      disabled={(network?.networkStatus !== "ACTIVE")}
                      variant="primary"
                      onClick={() => navigate(`/networks/${network?.networkName}/clients/create`)}
                  >
                      Create Client
                  </Button>) : <></>}
              >
                  Clients
              </Header>
          }
        columnDefinitions={getClientsTableColDef(network?.networkName)}
        items={items}
        loading={loading}
        visibleColumns={preferences.visibleContent}
        preferences={
          <CollectionPreferences
            {...CLIENTS_TABLE_PREFERENCES}
            preferences={preferences}
            onConfirm={({ detail }) => setPreferences(detail)}
          />
        }
        empty={
          <div style={{ textAlign: "center", padding: "20px" }}>
            No clients found.
          </div>
        }
      />
  );
}
export default ClientsTable
