import { BrowserRouter } from 'react-router-dom'
import { AppRouter } from './router'
import { ToastContainer } from '@/components/ui/toast-container'

function App() {
  return (
    <BrowserRouter>
      <AppRouter />
      <ToastContainer />
    </BrowserRouter>
  )
}

export default App
