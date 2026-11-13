// src/pages/ApprovalsPage.tsx
import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { getReservations, approveReservation, rejectReservation, type ReservationResponse } from '../api/reservations';
import { getRooms, type RoomSummary } from '../api/rooms';

export function ApprovalsPage() {
  const { user } = useAuth();
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [roomFilter, setRoomFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionId, setActionId] = useState<string | null>(null);
  const [confirmAction, setConfirmAction] = useState<{ id: string; type: 'approve' | 'reject' } | null>(null);

  async function load(pageNum: number) {
    setLoading(true);
    setError('');
    try {
      const data = await getReservations({
        status: statusFilter || undefined,
        roomId: roomFilter || undefined,
        page: pageNum,
        size: 10,
        sort: 'startTime,asc',
      });
      setReservations(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
      setPage(data.number);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load reservations');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let cancelled = false;
    getRooms()
      .then((data) => { if (!cancelled) setRooms(data); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    let cancelled = false;
    getReservations({
      status: statusFilter || undefined,
      roomId: roomFilter || undefined,
      page: 0,
      size: 10,
      sort: 'startTime,asc',
    })
      .then((data) => {
        if (!cancelled) {
          setReservations(data.content);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
          setPage(data.number);
        }
      })
      .catch((err) => { if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load reservations'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [statusFilter, roomFilter]);

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
      await load(page);
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
        <p className="text-gray-500">Loading reservations...</p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <h2 className="text-xl font-semibold text-gray-900 mb-6">Reservations</h2>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Filters */}
      <div className="flex gap-4 mb-6">
        <div>
          <label htmlFor="statusFilter" className="block text-sm font-medium text-gray-700 mb-1">
            Status
          </label>
          <select
            id="statusFilter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="REJECTED">Rejected</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
        <div>
          <label htmlFor="roomFilter" className="block text-sm font-medium text-gray-700 mb-1">
            Room
          </label>
          <select
            id="roomFilter"
            value={roomFilter}
            onChange={(e) => setRoomFilter(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All rooms</option>
            {rooms.map((room) => (
              <option key={room.id} value={room.id}>
                {room.name} ({room.code})
              </option>
            ))}
          </select>
        </div>
      </div>

      {reservations.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 p-8 text-center">
          <p className="text-gray-500">No reservations found.</p>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Requester</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Room</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Time Range</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Purpose</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
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
                      <span className={`text-xs font-medium px-2 py-1 rounded border ${
                        r.status === 'PENDING' ? 'bg-yellow-50 text-yellow-700 border-yellow-200' :
                        r.status === 'CONFIRMED' ? 'bg-green-50 text-green-700 border-green-200' :
                        r.status === 'REJECTED' ? 'bg-red-50 text-red-700 border-red-200' :
                        'bg-gray-50 text-gray-600 border-gray-200'
                      }`}>
                        {r.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {r.status === 'PENDING' && (
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
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-gray-500">
              Showing {reservations.length} of {totalElements} reservations
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => load(page - 1)}
                disabled={page === 0}
                className="px-3 py-1 text-sm border border-gray-300 rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Previous
              </button>
              <span className="px-3 py-1 text-sm text-gray-600">
                Page {page + 1} of {totalPages || 1}
              </span>
              <button
                onClick={() => load(page + 1)}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 text-sm border border-gray-300 rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Next
              </button>
            </div>
          </div>
        </>
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
