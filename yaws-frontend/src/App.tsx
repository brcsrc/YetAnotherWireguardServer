import { Routes, Route } from "react-router";
import Login from "./pages/login/Login.tsx";
import Dashboard from "./pages/dashboard/Dashboard.tsx";
import { ThemeContextProvider } from "./context/ThemeContextProvider";
import {FlashbarContextProvider, useFlashbarContext} from "./context/FlashbarContextProvider";
import { AuthContextProvider, useAuthContext } from "./context/AuthContextProvider";
import {useState} from "react";
import TopNavigationBar from "./components/layout/TopNavigation";
import {AppLayout, Flashbar, SideNavigation} from "@cloudscape-design/components";
import Breadcrumbs from "./components/layout/Breadcrumbs";

const UnauthenticatedRoutes = () => {
    return <Routes>
        <Route path="/login" element={<Login />} />
    </Routes>
}

const AuthenticatedRoutes = () => {
    return <Routes>
        <Route path="/" element={<Dashboard />} />
    </Routes>
}

// this app content component wraps everything with AppLayout
// it conditionally shows/hides nav and breadcrumbs based on authentication
const AppContent = () => {
    const { username } = useAuthContext();
    const isAuthenticated = username !== null;

    // persist side nav preferences in session storage so it doesnt open/close
    // on renders from other pages/components etc
    const [navigationOpen, setNavigationOpen] = useState(() => {
        const saved = sessionStorage.getItem('navigationOpen')
        return saved !== null ? JSON.parse(saved) : true
    })
    const handleNavigationChange = ({detail}) => {
        setNavigationOpen(detail.open)
        sessionStorage.setItem('navigationOpen', JSON.stringify(detail.open))
    }

    // any other component that adds a flashbar item will be collected in this array
    const { flashbarItems } = useFlashbarContext()

    return (
        <>
            <TopNavigationBar/>
            <AppLayout
                toolsHide={true}
                navigationHide={!isAuthenticated}
                navigation={
                    isAuthenticated ? (
                        <SideNavigation
                            items={[
                                {type: 'link', text: 'foo', href: '/foo'},
                            ]}
                        />
                    ) : undefined
                }
                navigationOpen={navigationOpen}
                onNavigationChange={handleNavigationChange}
                navigationWidth={175}
                notifications={<Flashbar items={flashbarItems} stackItems={true}/>}
                breadcrumbs={isAuthenticated ? <Breadcrumbs/> : undefined}
                content={isAuthenticated ? <AuthenticatedRoutes/> : <UnauthenticatedRoutes/>}
            />
        </>
    )
}

const App = () => {
  return (
    <ThemeContextProvider>
      <FlashbarContextProvider>
        <AuthContextProvider>
          <AppContent/>
        </AuthContextProvider>
      </FlashbarContextProvider>
    </ThemeContextProvider>
  );
}

export default App;
