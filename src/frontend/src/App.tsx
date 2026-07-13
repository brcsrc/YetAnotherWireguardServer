import { Routes, Route, useNavigate } from "react-router";
import Login from "./pages/login/Login.tsx";
import Dashboard from "./pages/dashboard/Dashboard.tsx";
import Networks from "./pages/network/Networks.tsx";
import Network from "./pages/network/Network.tsx";
import CreateNetwork from "./pages/network/CreateNetwork.tsx";
import UpdateNetwork from "./pages/network/UpdateNetwork.tsx";
import Client from "./pages/client/Client.tsx";
import CreateClient from "./pages/client/CreateClient.tsx";
import { ThemeContextProvider } from "./context/ThemeContextProvider";
import { FlashbarContextProvider } from "./context/FlashbarContextProvider";
import { AuthContextProvider, useAuthContext } from "./context/AuthContextProvider";
import { useState } from "react";
import TopNavigationBar from "./components/layout/TopNavigation";
import { AppLayout, SideNavigation } from "@cloudscape-design/components";
import Breadcrumbs from "./components/layout/Breadcrumbs";
import Flashbar from "./components/layout/flashbar/Flashbar";

const UnauthenticatedRoutes = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
    </Routes>
  );
};

const AuthenticatedRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/networks" element={<Networks />} />
      <Route path="/networks/create" element={<CreateNetwork />} />
      <Route path="/networks/:networkName" element={<Network />} />
      <Route path="/networks/:networkName/update" element={<UpdateNetwork />} />
      <Route path="/networks/:networkName/clients/create" element={<CreateClient />} />
      <Route path="/networks/:networkName/clients/:clientName" element={<Client />} />
    </Routes>
  );
};

// this app content component wraps everything with AppLayout
// it conditionally shows/hides nav and breadcrumbs based on authentication
const AppContent = () => {
  const { username } = useAuthContext();
  const isAuthenticated = username !== null;
  const navigate = useNavigate();

  // persist side nav preferences in session storage so it doesnt open/close
  // on renders from other pages/components etc
  const [navigationOpen, setNavigationOpen] = useState(() => {
    const saved = sessionStorage.getItem("navigationOpen");
    return saved !== null ? JSON.parse(saved) : true;
  });
  const handleNavigationChange = ({ detail }) => {
    setNavigationOpen(detail.open);
    sessionStorage.setItem("navigationOpen", JSON.stringify(detail.open));
  };

  return (
    <>
      <TopNavigationBar />
      <Flashbar />
      <AppLayout
        toolsHide={true}
        navigationHide={!isAuthenticated}
        navigation={
          isAuthenticated ? (
            <SideNavigation
              items={[
                { type: "link", text: "Dashboard", href: "/" },
                { type: "link", text: "Networks", href: "/networks" },
              ]}
              // without onFollow, side nav links do hard reload of the destination when clicked
              onFollow={(event) => {
                if (!event.detail.external) {
                  event.preventDefault();
                  navigate(event.detail.href);
                }
              }}
            />
          ) : undefined
        }
        navigationOpen={navigationOpen}
        onNavigationChange={handleNavigationChange}
        navigationWidth={175}
        breadcrumbs={isAuthenticated ? <Breadcrumbs /> : undefined}
        content={isAuthenticated ? <AuthenticatedRoutes /> : <UnauthenticatedRoutes />}
      />
    </>
  );
};

const App = () => {
  return (
    <ThemeContextProvider>
      <FlashbarContextProvider>
        <AuthContextProvider>
          <AppContent />
        </AuthContextProvider>
      </FlashbarContextProvider>
    </ThemeContextProvider>
  );
};

export default App;
