import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { ThemeProvider } from "@/components/theme-provider"

//import Amplify
import {Amplify} from 'aws-amplify'

//Cấu hình kết nối
Amplify.configure({
  Auth: {
    Cognito: {
      userPoolId: 'ap-southeast-1_x2pfx7M9N', // <-- Copy từ SAM Output
      userPoolClientId: '7c2fmbi1gmgeef3bk9th7qvaud', // <-- Copy từ SAM Output
      loginWith: {
        email: true,
      }
    }
  }
});

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
        <App />
    </ThemeProvider>
  </React.StrictMode>,
)
