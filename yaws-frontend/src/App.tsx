import { useState } from 'react';
import reactLogo from './assets/react.svg';
import viteLogo from '/vite.svg';
import './App.css';

import { UserControllerApi, Configuration, User } from "@yaws/yaws-ts-api-client";


function App() {

    const handleCreateUserClick = async () => {
        const userClient = new UserControllerApi(new Configuration());
        const newUser: User = await userClient.createAdminUser({
            user: {
                userName: "yaws-admin",
                password: "gH1@#oKl2ff1"
            }
        })
        return newUser
    }

    const handleAuthenticateClick = async () => {
        const userClient = new UserControllerApi(new Configuration())
        await userClient.authenticateAndIssueToken({
            user: {
                userName: "yaws-admin",
                password: "gH1@#oKl2ff1"
            }
        })
    }


  const [count, setCount] = useState(0);


  return (
    <div className="App">
      <div>
        <a href="https://vitejs.dev" target="_blank" rel="noreferrer">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank" rel="noreferrer">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>Vite + React</h1>
      <div className="card">
        <button onClick={() => {setCount(count + 1)}}>
          count is {count}
        </button>
        <button onClick={handleCreateUserClick}>
            Create Admin User
        </button>
        <button onClick={handleAuthenticateClick}>
            Authenticate
        </button>
        <p>
          Edit <code>src/App.tsx</code> and save to test HMR
        </p>
      </div>
      <p className="read-the-docs">
        Click on the Vite and React logos to learn more
      </p>
    </div>
  );
}

export default App;