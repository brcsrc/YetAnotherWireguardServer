import { useState } from 'react'

function App() {
  const [response, setResponse] = useState<string | null>(null)

  const testApi = async () => {
    try {
      const res = await fetch('/api/v1/networks') // Example API endpoint
      if (!res.ok) throw new Error(`Error: ${res.status}`)
      const data = await res.json()
      setResponse(JSON.stringify(data, null, 2))
    } catch (error) {
      // Narrow the type of error to access its message
      if (error instanceof Error) {
        setResponse(error.message)
      } else {
        setResponse('An unknown error occurred')
      }
    }
  }

  return (
    <div>
      <h1>Test API</h1>
      <button onClick={testApi}>Call API</button>
      <pre>{response}</pre>
    </div>
  )
}

export default App