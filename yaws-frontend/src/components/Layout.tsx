import { useState, useEffect } from "react";
import {
  AppLayout,
  TopNavigation,
  SideNavigation,
  BreadcrumbGroup,
} from "@cloudscape-design/components";
import { useNavigate } from "react-router";
import { getTheme, toggleTheme, Mode } from "../utils/theme";

interface LayoutProps {
  children: React.ReactNode;
  breadcrumbs: { text: string; href: string }[];
}

export default function Layout({ children, breadcrumbs }: LayoutProps) {
  const navigate = useNavigate();
  const [theme, setTheme] = useState<Mode>(getTheme());
  const [navigationOpen, setNavigationOpen] = useState(false);

  useEffect(() => {
    setTheme(getTheme());
  }, []);

  return (
    <div style={{ minHeight: "100vh" }}>
      <TopNavigation
        identity={{
          title: "YAWS",
          href: "<placeholder>",
          logo: { src: "/favicon.ico", alt: "YAWS Logo" },
        }}
        utilities={[
          {
            type: "button",
            text: theme === "dark" ? "Light Mode" : "Dark Mode",
            iconName: theme === "dark" ? "face-sad-filled" : "face-happy",
            onClick: () => setTheme(toggleTheme()),
          },
        ]}
      />
      <AppLayout
        headerSelector="#top-nav"
        navigation={
          <SideNavigation
            header={{ text: "Navigation", href: "/dashboard" }}
            items={[
              { type: "link", text: "<PlaceholderText>", href: "<placeholder>" },
              { type: "link", text: "Back to Login Page", href: "/", external: true },
            ]}
            activeHref={window.location.pathname}
            onFollow={(event) => {
              if (!event.detail.external) {
                event.preventDefault();
                navigate(event.detail.href);
              }
            }}
          />
        }
        navigationOpen={navigationOpen}
        onNavigationChange={({ detail }) => setNavigationOpen(detail.open)}
        breadcrumbs={
          <BreadcrumbGroup
            items={breadcrumbs}
            onFollow={(event) => {
              event.preventDefault();
              navigate(event.detail.href);
            }}
          />
        }
        content={children}
        toolsHide={true}
      />
    </div>
  );
}
