// src/pages/DashboardPage.tsx
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { getReservations, type ReservationResponse } from '../api/reservations';
import { getRooms, type RoomSummary } from '../api/rooms';

const MINUTES_PER_DAY = 24 * 60;

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

function toLocalDateTimeInputValue(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function startOfTodayInput(): string {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T00:00`;
}

function endOfTodayInput(): string {
  const now = new Date();
  now.setHours(23, 59, 59, 999);
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T23:59`;
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function bookedMinutesForRoom(reservations: ReservationResponse[], roomId: string): number {
  const dayStart = new Date();
  dayStart.setHours(0, 0, 0, 0);
  const dayEnd = new Date(dayStart);
  dayEnd.setDate(dayEnd.getDate() + 1);
  const dayStartMs = dayStart.getTime();
  const dayEndMs = dayEnd.getTime();
  let minutes = 0;
  for (const reservation of reservations) {
    if (reservation.room.id !== roomId) continue;
    const start = new Date(reservation.startTime).getTime();
    const end = new Date(reservation.endTime).getTime();
    const overlapStart = Math.max(start, dayStartMs);
    const overlapEnd = Math.min(end, dayEndMs);
    if (overlapEnd > overlapStart) {
      minutes += (overlapEnd - overlapStart) / 60000;
    }
  }
  return minutes;
}

export function DashboardPage() {
  const { user } = useAuth();
  const isHod = user?.role === 'HOD';

  const [pendingCount, setPendingCount] = useState(0);
  const [confirmedCount, setConfirmedCount] = useState(0);
  const [upcoming, setUpcoming] = useState<ReservationResponse[]>([]);
  const [upcomingCount, setUpcomingCount] = useState(0);
  const [todayConfirmed, setTodayConfirmed] = useState<ReservationResponse[]>([]);
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      setLoading(true);
      setError('');
      try {
        if (isHod) {
          const pendingPage = await getReservations({ status: 'PENDING', size: 1 });
          if (cancelled) return;
          setPendingCount(pendingPage.totalElements);

          const todayPage = await getReservations({
            status: 'CONFIRMED',
            from: startOfTodayInput(),
            to: endOfTodayInput(),
            size: 100,
            sort: 'startTime,asc',
          });
          if (cancelled) return;
          setTodayConfirmed(todayPage.content);

          const roomList = await getRooms();
          if (cancelled) return;
          setRooms(roomList);
        } else {
          const [pendingResult, confirmedResult, upcomingResult] = await Promise.all([
            getReservations({ status: 'PENDING', size: 1 }),
            getReservations({ status: 'CONFIRMED', size: 1 }),
            getReservations({ status: 'CONFIRMED', from: toLocalDateTimeInputValue(new Date()), size: 3, sort: 'startTime,asc' }),
          ]);
          if (cancelled) return;
          setPendingCount(pendingResult.totalElements);
          setConfirmedCount(confirmedResult.totalElements);
          setUpcoming(upcomingResult.content);
          setUpcomingCount(upcomingResult.totalElements);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load dashboard');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadDashboard();
    return () => {
      cancelled = true;
    };
  }, [isHod]);

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <p className="text-gray-500">Loading dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div role="alert" className="p-4 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
          {error}
        </div>
      </div>
    );
  }

  if (isHod) {
    const utilisation = rooms.map((room) => ({
      room,
      bookedMinutes: bookedMinutesForRoom(todayConfirmed, room.id),
    }));

    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-gray-900">Dashboard</h2>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-6 mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-gray-500">Pending approvals</p>
            <p className="text-3xl font-semibold text-gray-900 mt-2">{pendingCount}</p>
          </div>
          {pendingCount > 0 && (
            <Link
              to="/approvals"
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Review approvals
            </Link>
          )}
        </div>

        <h3 className="text-lg font-semibold text-gray-900 mb-4">Confirmed today</h3>
        {todayConfirmed.length === 0 ? (
          <div className="bg-white rounded-lg border border-gray-200 p-8 text-center mb-6">
            <p className="text-gray-500">No confirmed reservations for today yet.</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg border border-gray-200 overflow-x-auto mb-8">
            <table className="w-full min-w-[640px]">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Requester</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Room</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Time Range</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {todayConfirmed.map((r) => (
                  <tr key={r.id}>
                    <td className="px-4 py-3 text-sm text-gray-900">{r.requestedBy.name}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{r.room.name}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {formatDateTime(r.startTime)} — {formatDateTime(r.endTime)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{r.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <h3 className="text-lg font-semibold text-gray-900 mb-4">Room utilisation today</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {utilisation.map(({ room, bookedMinutes }) => {
            const utilisationPercent = Math.round((bookedMinutes / MINUTES_PER_DAY) * 1000) / 10;
            return (
              <div key={room.id} className="bg-white rounded-lg border border-gray-200 p-5">
                <h4 className="font-medium text-gray-900">{room.name}</h4>
                <p className="text-sm text-gray-500 mt-1">{room.code}</p>
                <p className="text-sm text-gray-600 mt-3">
                  {bookedMinutes > 0 ? `${bookedMinutes} minutes booked` : 'No bookings today'}
                </p>
                <p className="text-sm font-medium text-gray-900 mt-1">{utilisationPercent}% of the day booked</p>
              </div>
            );
          })}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">Dashboard</h2>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <p className="text-sm text-gray-500">Pending requests</p>
          <p className="text-3xl font-semibold text-gray-900 mt-2">{pendingCount}</p>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <p className="text-sm text-gray-500">Confirmed reservations</p>
          <p className="text-3xl font-semibold text-gray-900 mt-2">{confirmedCount}</p>
        </div>
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <p className="text-sm text-gray-500">Upcoming reservations</p>
          <p className="text-3xl font-semibold text-gray-900 mt-2">{upcomingCount}</p>
        </div>
      </div>

      <h3 className="text-lg font-semibold text-gray-900 mb-4">Next upcoming reservations</h3>
      {upcoming.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 p-8 text-center">
          <p className="text-gray-500">No upcoming reservations. Create one from the New Reservation page.</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg border border-gray-200 overflow-x-auto">
          <table className="w-full min-w-[640px]">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Room</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Start</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">End</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {upcoming.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    {r.room.name} ({r.room.code})
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{formatDateTime(r.startTime)}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{formatDateTime(r.endTime)}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{r.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
