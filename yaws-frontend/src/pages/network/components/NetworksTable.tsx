import {
  Button,
  CollectionPreferences,
  Header,
  PropertyFilter,
  Table,
} from "@cloudscape-design/components";
import { useCollection } from "@cloudscape-design/collection-hooks";
import { Network } from "yaws-ts-api-client/dist/types/index";
import { networkClient } from "../../../utils/clients";
import {
  NETWORKS_TABLE_COL_DEF,
  NETWORKS_TABLE_FILTER_PROPS,
  NETWORKS_TABLE_PREFERENCES,
} from "./networksTableUtilities";
import * as React from "react";
import { useEffect, useState } from "react";
import { useFlashbarContext } from "../../../context/FlashbarContextProvider";

const NetworksTable = (): React.ReactNode => {
  const [networks, setNetworks] = useState<Network[]>([]);
  const [loading, setLoading] = useState(true);
  const [preferences, setPreferences] = useState({
    pageSize: 10,
    visibleContent: ["name", "publicKeyValue", "cidr", "port", "tag", "status"],
  });

  const { addFlashbarItem } = useFlashbarContext();

  useEffect(() => {
    (async function () {
      try {
        let page: number | undefined = 0;
        while (page !== undefined) {
          const response = await networkClient.listNetworks({
            listNetworksRequest: {
              page: page,
            },
          });
          setNetworks((prevState) => [...prevState, ...response.networks]);
          page = response.nextPage;
        }
      } catch (error) {
        const errorMessage =
          error.response?.data?.message || error.response?.data?.error || error.message;
        addFlashbarItem({
          type: "error",
          header: "Failure in ListNetworks",
          content: errorMessage,
          dismissLabel: "Dismiss",
          duration: 10000,
        });
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const { items, collectionProps, propertyFilterProps } = useCollection(networks, {
    sorting: {},
    filtering: {
      empty: <div style={{ textAlign: "center", padding: "20px" }}>No networks found.</div>,
      noMatch: <div style={{ textAlign: "center", padding: "20px" }}>No matches found.</div>,
    },
    propertyFiltering: {
      filteringProperties: NETWORKS_TABLE_FILTER_PROPS.filteringProperties,
    },
    pagination: { pageSize: preferences.pageSize },
  });

  return (
    <Table
      {...collectionProps}
      filter={<PropertyFilter {...propertyFilterProps} {...NETWORKS_TABLE_FILTER_PROPS} />}
      header={
        <Header counter={networks.length ? `(${networks.length})` : undefined}>Networks</Header>
      }
      columnDefinitions={NETWORKS_TABLE_COL_DEF}
      items={items}
      loading={loading}
      visibleColumns={preferences.visibleContent}
      preferences={
        <CollectionPreferences
          {...NETWORKS_TABLE_PREFERENCES}
          preferences={preferences}
          onConfirm={({ detail }) => setPreferences(detail)}
        />
      }
      empty={<div style={{ textAlign: "center", padding: "20px" }}>No networks found.</div>}
    />
  );
};
export default NetworksTable;
