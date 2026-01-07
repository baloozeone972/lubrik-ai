import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Layout } from './components/layout/Layout'
import { ProtectedRoute } from './components/layout/ProtectedRoute'
import { LoginScreen } from './pages/Login'
import { RegisterScreen } from './pages/Register'
import { DashboardScreen } from './pages/Dashboard'
import { CompanionsScreen } from './pages/Companions'
import { CompanionFormScreen } from './pages/CompanionForm'
import { ChatScreen } from './pages/Chat'
import { ConversationsScreen } from './pages/Conversations'
import { ProfileScreen } from './pages/Profile'
import { SubscriptionScreen } from './pages/Subscription'
import { NotFoundScreen } from './pages/NotFound'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginScreen />} />
          <Route path="/register" element={<RegisterScreen />} />
          
          {/* Protected routes */}
          <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardScreen />} />
            <Route path="/companions" element={<CompanionsScreen />} />
            <Route path="/companions/new" element={<CompanionFormScreen />} />
            <Route path="/companions/:id/edit" element={<CompanionFormScreen />} />
            <Route path="/chat/:companionId" element={<ChatScreen />} />
            <Route path="/conversations" element={<ConversationsScreen />} />
            <Route path="/profile" element={<ProfileScreen />} />
            <Route path="/subscription" element={<SubscriptionScreen />} />
          </Route>
          
          {/* 404 */}
          <Route path="*" element={<NotFoundScreen />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
