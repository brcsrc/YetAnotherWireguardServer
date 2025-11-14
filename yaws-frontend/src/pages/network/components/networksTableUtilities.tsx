import {Badge} from "@cloudscape-design/components";
import { Link } from "react-router";

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
            <Badge color={item.networkStatus === "ACTIVE" ? "green" : "red"}>
                {item.networkStatus}
            </Badge>
        ),
        sortingField: "networkStatus",
    },
    {
        id: "privateKey",
        header: "Private Key Name",
        cell: (item) => item.networkPrivateKeyName,
        sortingField: "networkPrivateKeyName",
    },
    {
        id: "publicKey",
        header: "Public Key Name",
        cell: (item) => item.networkPublicKeyName,
        sortingField: "networkPublicKeyName",
    },
]

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
}

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
            { value: 100, label: "100 networks" }
        ]
    },
    visibleContentPreference: {
        title: "Column visibility",
        options: [
            {
                label: "Network properties",
                options: [
                    { id: "name", label: "Name", editable: false },
                    { id: "cidr", label: "CIDR" },
                    { id: "port", label: "Listen Port" },
                    { id: "tag", label: "Tag" },
                    { id: "status", label: "Status" },
                    { id: "privateKey", label: "Private Key Name" },
                    { id: "publicKey", label: "Public Key Name" },
                ]
            }
        ]
    }
}