import { Routes, Route, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuthStore } from '@/store/authStore'
import { ProtectedRoute } from '@/components/layout'
import {
  Login,
  Register,
  Dashboard,
  Chat,
  Companions,
  CompanionForm,
  Conversations,
} from '@/pages'

export default function App() {
  const { isAuthenticated, setLoading } = useAuthStore()

  useEffect(() => {
    // Check if we have stored auth on mount
    setLoading(false)
  }, [setLoading])

  return (
    <Routes>
      {/* Public routes */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/dashboard" /> : <Login />}
      />
      <Route
        path="/register"
        element={isAuthenticated ? <Navigate to="/dashboard" /> : <Register />}
      />

      {/* Protected routes */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/companions"
        element={
          <ProtectedRoute>
            <Companions />
          </ProtectedRoute>
        }
      />
      <Route
        path="/companions/new"
        element={
          <ProtectedRoute>
            <CompanionForm />
          </ProtectedRoute>
        }
      />
      <Route
        path="/companions/:id/edit"
        element={
          <ProtectedRoute>
            <CompanionForm />
          </ProtectedRoute>
        }
      />
      <Route
        path="/conversations"
        element={
          <ProtectedRoute>
            <Conversations />
          </ProtectedRoute>
        }
      />
      <Route
        path="/chat/:conversationId"
        element={
          <ProtectedRoute>
            <Chat />
          </ProtectedRoute>
        }
      />

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/dashboard" />} />
      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  )
}
