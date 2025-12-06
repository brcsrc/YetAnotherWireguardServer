import { Badge } from "@cloudscape-design/components";
import { Link } from "../../../components/misc/Link";
import { PublicKeyDisplay } from "../../../components/misc/PublicKeyDisplay";

export const NETWORKS_TABLE_COL_DEF = [
  {
    id: "name",
    header: "Name",
    cell: (item) => (
      <Link to={`/networks/${item.networkName}`} state={item}>
        {item.networkName}
      </Link>
    ),
    sortingField: "networkName",
  },
  {
    id: "publicKeyValue",
    header: "Public Key",
    cell: (item) => <PublicKeyDisplay publicKey={item.networkPublicKeyValue} />,
    sortingField: "networkPublicKeyValue",
  },
  {
    id: "cidr",
    header: "CIDR",
    cell: (item) => item.networkCidr,
    sortingField: "networkCidr",
  },
  {
    id: "port",
    header: "Listen Port",
    cell: (item) => item.networkListenPort,
    sortingField: "networkListenPort",
  },
  {
    id: "tag",
    header: "Tag",
    cell: (item) => item.networkTag,
    sortingField: "networkTag",
  },
  {
    id: "status",
    header: "Status",
    cell: (item) => (
      <Badge color={item.networkStatus === "ACTIVE" ? "green" : "red"}>{item.networkStatus}</Badge>
    ),
    sortingField: "networkStatus",
  },
  {
    id: "privateKeyName",
    header: "Private Key Name",
    cell: (item) => item.networkPrivateKeyName,
    sortingField: "networkPrivateKeyName",
  },
  {
    id: "publicKeyName",
    header: "Public Key Name",
    cell: (item) => item.networkPublicKeyName,
    sortingField: "networkPublicKeyName",
  },
];

export const NETWORKS_TABLE_FILTER_PROPS = {
  filteringPlaceholder: "Search networks",
  filteringAriaLabel: "Filter networks",
  filteringProperties: [
    {
      key: "networkName",
      propertyLabel: "Name",
      groupValuesLabel: "Name values",
      operators: [":", "!:", "=", "!="],
    },
    {
      key: "networkPublicKeyValue",
      propertyLabel: "Public Key",
      groupValuesLabel: "Public Key values",
    },
    {
      key: "networkCidr",
      propertyLabel: "CIDR",
      groupValuesLabel: "CIDR values",
      operators: [":", "!:", "=", "!="],
    },
    {
      key: "networkListenPort",
      propertyLabel: "Listen Port",
      groupValuesLabel: "Port values",
      operators: ["=", "!=", ">", ">=", "<", "<="],
    },
    {
      key: "networkTag",
      propertyLabel: "Tag",
      groupValuesLabel: "Tag values",
      operators: [":", "!:", "=", "!="],
    },
    {
      key: "networkStatus",
      propertyLabel: "Status",
      groupValuesLabel: "Status values",
      operators: ["=", "!="],
    },
  ],
};

export const NETWORKS_TABLE_PREFERENCES = {
  title: "Preferences",
  confirmLabel: "Confirm",
  cancelLabel: "Cancel",
  pageSizePreference: {
    title: "Page size",
    options: [
      { value: 10, label: "10 networks" },
      { value: 20, label: "20 networks" },
      { value: 50, label: "50 networks" },
      { value: 100, label: "100 networks" },
    ],
  },
  visibleContentPreference: {
    title: "Column visibility",
    options: [
      {
        label: "Network properties",
        options: [
          { id: "name", label: "Name", editable: false },
          { id: "publicKeyValue", label: "Public Key" },
          { id: "cidr", label: "CIDR" },
          { id: "port", label: "Listen Port" },
          { id: "tag", label: "Tag" },
          { id: "status", label: "Status" },
          { id: "privateKeyName", label: "Private Key Name" },
          { id: "publicKeyName", label: "Public Key Name" },
        ],
      },
    ],
  },
};
