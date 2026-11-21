// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { ErrorBoundary } from './components/ErrorBoundary';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { RoomsPage } from './pages/RoomsPage';
import { NewReservationPage } from './pages/NewReservationPage';
import { MyReservationsPage } from './pages/MyReservationsPage';
import { ApprovalsPage } from './pages/ApprovalsPage';
import { ManageRoomsPage } from './pages/ManageRoomsPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { SsoPage } from './pages/SsoPage';

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/sso" element={<SsoPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<Layout />}>
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/rooms" element={<RoomsPage />} />
                <Route path="/rooms/manage" element={<ManageRoomsPage />} />
                <Route path="/reservations" element={<MyReservationsPage />} />
                <Route path="/reservations/new" element={<NewReservationPage />} />
                <Route path="/approvals" element={<ApprovalsPage />} />
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
              </Route>
            </Route>
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ErrorBoundary>
  );
}
