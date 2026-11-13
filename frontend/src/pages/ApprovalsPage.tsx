// src/pages/ApprovalsPage.tsx
import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { getReservations, approveReservation, rejectReservation, type ReservationResponse } from '../api/reservations';

export function ApprovalsPage() {
  const { user } = useAuth();
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionId, setActionId] = useState<string | null>(null);
  const [confirmAction, setConfirmAction] = useState<{ id: string; type: 'approve' | 'reject' } | null>(null);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await getReservations({ status: 'PENDING', page: 0, size: 50, sort: 'startTime,asc' });
      setReservations(data.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load reservations');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let cancelled = false;
    getReservations({ status: 'PENDING', page: 0, size: 50, sort: 'startTime,asc' })
      .then((data) => { if (!cancelled) setReservations(data.content); })
      .catch((err) => { if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load reservations'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  async function handleAction(id: string, type: 'approve' | 'reject') {
    setActionId(id);
    setError('');
    try {
      if (type === 'approve') {
        await approveReservation(id);
      } else {
        await rejectReservation(id);
      }
      setConfirmAction(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Action failed');
    } finally {
      setActionId(null);
    }
  }

  if (user?.role !== 'HOD') {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="p-4 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
          Access denied. This page is only available to HODs.
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <p className="text-gray-500">Loading pending reservations...</p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <h2 className="text-xl font-semibold text-gray-900 mb-6">Pending Approvals</h2>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
          {error}
        </div>
      )}

      {reservations.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 p-8 text-center">
          <p className="text-gray-500">No pending reservations.</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Requester</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Room</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Time Range</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Purpose</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {reservations.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    {r.requestedBy.name} ({r.requestedBy.portalStaffId})
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {r.room.name} ({r.room.code})
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {new Date(r.startTime).toLocaleString()} — {new Date(r.endTime).toLocaleString()}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {r.purpose || '—'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        onClick={() => setConfirmAction({ id: r.id, type: 'approve' })}
                        disabled={actionId === r.id}
                        className="text-sm text-green-600 hover:text-green-800 disabled:opacity-50"
                      >
                        {actionId === r.id ? '...' : 'Approve'}
                      </button>
                      <button
                        onClick={() => setConfirmAction({ id: r.id, type: 'reject' })}
                        disabled={actionId === r.id}
                        className="text-sm text-red-600 hover:text-red-800 disabled:opacity-50"
                      >
                        {actionId === r.id ? '...' : 'Reject'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {confirmAction && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-sm w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              {confirmAction.type === 'approve' ? 'Approve Reservation' : 'Reject Reservation'}
            </h3>
            <p className="text-sm text-gray-600 mb-4">
              Are you sure you want to {confirmAction.type} this reservation?
            </p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setConfirmAction(null)}
                className="px-3 py-2 text-sm bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200"
              >
                Cancel
              </button>
              <button
                onClick={() => handleAction(confirmAction.id, confirmAction.type)}
                disabled={actionId === confirmAction.id}
                className={`px-3 py-2 text-sm text-white rounded-md disabled:opacity-50 ${
                  confirmAction.type === 'approve' ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'
                }`}
              >
                {actionId === confirmAction.id ? 'Processing...' : confirmAction.type === 'approve' ? 'Approve' : 'Reject'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
