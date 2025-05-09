import { Routes, Route } from "react-router";
import Login from "./pages/Login.tsx";
import Dashboard from "./pages/Dashboard.tsx";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/dashboard" element={<Dashboard />} />
    </Routes>
  );
}

export default App;
