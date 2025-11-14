import { Link } from "react-router";

export const getClientsTableColDef = (networkName: string) => [
    {
        id: "name",
        header: "Name",
        cell: (item) => (
            <Link to={`/networks/${networkName}/clients/${item.clientName}`} state={item}>
                {item.clientName}
            </Link>
        ),
        sortingField: "clientName",
    },
    {
        id: "cidr",
        header: "CIDR",
        cell: (item) => item.clientCidr,
        sortingField: "clientCidr",
    },
    {
        id: "dns",
        header: "DNS",
        cell: (item) => item.clientDns,
        sortingField: "clientDns",
    },
    {
        id: "allowedIps",
        header: "Allowed IPs",
        cell: (item) => item.allowedIps,
        sortingField: "allowedIps",
    },
    {
        id: "tag",
        header: "Tag",
        cell: (item) => item.clientTag,
        sortingField: "clientTag",
    },
    {
        id: "privateKey",
        header: "Private Key Name",
        cell: (item) => item.clientPrivateKeyName,
        sortingField: "clientPrivateKeyName",
    },
    {
        id: "publicKey",
        header: "Public Key Name",
        cell: (item) => item.clientPublicKeyName,
        sortingField: "clientPublicKeyName",
    },
]

export const CLIENTS_TABLE_FILTER_PROPS = {
    filteringPlaceholder: "Search clients",
    filteringAriaLabel: "Filter clients",
    filteringProperties: [
        {
            key: "clientName",
            propertyLabel: "Name",
            groupValuesLabel: "Name values",
            operators: [":", "!:", "=", "!="],
        },
        {
            key: "clientCidr",
            propertyLabel: "CIDR",
            groupValuesLabel: "CIDR values",
            operators: [":", "!:", "=", "!="],
        },
        {
            key: "clientDns",
            propertyLabel: "DNS",
            groupValuesLabel: "DNS values",
            operators: [":", "!:", "=", "!="],
        },
        {
            key: "allowedIps",
            propertyLabel: "Allowed IPs",
            groupValuesLabel: "Allowed IPs values",
            operators: [":", "!:", "=", "!="],
        },
        {
            key: "clientTag",
            propertyLabel: "Tag",
            groupValuesLabel: "Tag values",
            operators: [":", "!:", "=", "!="],
        },
    ],
}

export const CLIENTS_TABLE_PREFERENCES = {
    title: "Preferences",
    confirmLabel: "Confirm",
    cancelLabel: "Cancel",
    pageSizePreference: {
        title: "Page size",
        options: [
            { value: 10, label: "10 clients" },
            { value: 20, label: "20 clients" },
            { value: 50, label: "50 clients" },
            { value: 100, label: "100 clients" }
        ]
    },
    visibleContentPreference: {
        title: "Column visibility",
        options: [
            {
                label: "Client properties",
                options: [
                    { id: "name", label: "Name", editable: false },
                    { id: "cidr", label: "CIDR" },
                    { id: "dns", label: "DNS" },
                    { id: "allowedIps", label: "Allowed IPs" },
                    { id: "tag", label: "Tag" },
                    { id: "privateKey", label: "Private Key Name" },
                    { id: "publicKey", label: "Public Key Name" },
                ]
            }
        ]
    }
}
