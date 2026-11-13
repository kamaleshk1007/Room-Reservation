// src/pages/NotFoundPage.tsx
import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <h1 className="text-4xl font-semibold text-gray-900 mb-2">404</h1>
        <p className="text-gray-600 mb-6">Page not found.</p>
        <Link to="/dashboard" className="text-blue-600 hover:text-blue-700">
          Go to Dashboard
        </Link>
      </div>
    </div>
  );
}
