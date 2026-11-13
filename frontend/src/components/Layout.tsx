// src/components/Layout.tsx
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  if (!user) return null;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <h1 className="text-xl font-semibold text-gray-900">Room Reserve</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">{user.name} ({user.role})</span>
            <button
              onClick={handleLogout}
              className="text-sm text-gray-600 hover:text-gray-900"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>
      <nav className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 flex gap-6">
          <Link to="/dashboard" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
            Dashboard
          </Link>
          <Link to="/rooms" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
            Rooms
          </Link>
          <Link to="/reservations" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
            My Reservations
          </Link>
          <Link to="/reservations/new" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
            New Reservation
          </Link>
          {user.role === 'HOD' && (
            <Link to="/approvals" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
              Approvals
            </Link>
          )}
        </div>
      </nav>
      <main>
        <Outlet />
      </main>
    </div>
  );
}
