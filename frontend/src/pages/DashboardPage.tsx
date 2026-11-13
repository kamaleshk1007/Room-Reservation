// src/pages/DashboardPage.tsx
import { useAuth } from '../auth/useAuth';
import { useNavigate } from 'react-router-dom';

export function DashboardPage() {
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
          <a href="/dashboard" className="py-3 text-sm font-medium text-blue-600 border-b-2 border-blue-600">
            Dashboard
          </a>
          <a href="/rooms" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
            Rooms
          </a>
          <a href="/reservations" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
            My Reservations
          </a>
          <a href="/reservations/new" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
            New Reservation
          </a>
          {user.role === 'HOD' && (
            <a href="/approvals" className="py-3 text-sm font-medium text-gray-600 hover:text-gray-900">
              Approvals
            </a>
          )}
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 py-8">
        <div className="bg-white rounded-lg border border-gray-200 p-8 text-center">
          <p className="text-gray-500">Dashboard content coming soon.</p>
        </div>
      </main>
    </div>
  );
}
