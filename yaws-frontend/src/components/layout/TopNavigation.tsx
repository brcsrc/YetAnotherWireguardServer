import TopNavigation from "@cloudscape-design/components/top-navigation";
import Toggle from "@cloudscape-design/components/toggle";
import { useThemeContext } from "../../context/ThemeContextProvider";
import { useAuthContext } from "../../context/AuthContextProvider";
import { SpaceBetween } from "@cloudscape-design/components";

const TopNavigationBar = (): JSX.Element => {
  const { theme, toggleTheme } = useThemeContext();
  const { username, logout } = useAuthContext();

  const utilities = [
    {
      type: "menu-dropdown" as const,
      text: "Documentation",
      iconName: "folder-open",
      ariaLabel: "Links",
      items: [
        {
          id: "wireguard",
          text: "WireGuard Docs",
          href: "https://www.wireguard.com/",
          external: true,
        },
        {
          id: "github",
          text: "GitHub Repository",
          href: "https://github.com/brcsrc/YetAnotherWireguardServer",
          external: true,
        },
      ],
    },
  ];

  if (username) {
    // @ts-ignore
    utilities.push({
      type: "menu-dropdown" as const,
      text: username,
      iconName: "user-profile",
      items: [{ id: "logout", text: "Logout", iconName: "undo" }],
      onItemClick: ({ detail }) => {
        if (detail.id === "logout") logout();
      },
    });
  }

  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        background: "#161d26", // nav bar OEM color
      }}
    >
      <div style={{ flex: 1 }}>
        <TopNavigation
          identity={{
            title: "YAWS",
            href: "/",
            logo: { src: "/favicon.ico", alt: "YAWS Logo" },
          }}
          utilities={utilities}
          i18nStrings={{ overflowMenuTriggerText: "More" }}
        />
      </div>
      {/*<div*/}
      {/*    style={{*/}
      {/*        width: "1px",*/}
      {/*        backgroundColor: "#424650", // match nav utility divider color*/}
      {/*        alignSelf: "stretch", // make it full height of wrapper*/}
      {/*    }}*/}
      {/*/>*/}
      <div
        style={{
          display: "flex",
          alignSelf: "stretch",
          paddingRight: "1rem",
          borderBottom: "solid #424650 1px",
        }}
      >
        <SpaceBetween size={"xs"} direction={"horizontal"} alignItems={"center"}>
          🌙
          <Toggle
            checked={theme === "dark"}
            onChange={() => toggleTheme()}
            ariaLabel="Toggle dark mode"
          />
        </SpaceBetween>
      </div>
    </div>
  );
};

export default TopNavigationBar;
