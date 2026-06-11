import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import PendingTechniciansPage from './pages/PendingTechniciansPage';
import TechniciansPage from './pages/TechniciansPage';
import UsersPage from './pages/UsersPage';
import BookingsPage from './pages/BookingsPage';
import LogsPage from './pages/LogsPage';

// Everything except /login requires a logged-in admin.
function RequireAuth({ children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<Navigate to="/technicians/pending" replace />} />
        <Route path="/technicians/pending" element={<PendingTechniciansPage />} />
        <Route path="/technicians" element={<TechniciansPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/bookings" element={<BookingsPage />} />
        <Route path="/logs" element={<LogsPage />} />
        <Route path="*" element={<Navigate to="/technicians/pending" replace />} />
      </Route>
    </Routes>
  );
}
