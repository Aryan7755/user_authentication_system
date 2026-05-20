import { useState } from 'react'

import './App.css'

function App() {
  const [count, setCount] = useState(0)

  return (
    <div className='p-10'>
      <h1 className="text-3xl font-bold">Hello</h1>
    </div>
  )
}

export default App
