// src/pages/RoomsPage.tsx
import { useEffect, useState } from 'react';
import { getRooms, type RoomSummary } from '../api/rooms';

export function RoomsPage() {
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    getRooms()
      .then(setRooms)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load rooms'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = rooms.filter((r) =>
    r.name.toLowerCase().includes(search.toLowerCase()) ||
    r.code.toLowerCase().includes(search.toLowerCase())
  );

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <p className="text-gray-500">Loading rooms...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="p-4 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">Rooms</h2>
        <input
          type="text"
          placeholder="Search by name or code"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="px-3 py-2 border border-gray-300 rounded-md text-sm w-64 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {filtered.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 p-8 text-center">
          <p className="text-gray-500">
            {search ? 'No rooms match your search.' : 'No rooms available.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((room) => (
            <div key={room.id} className="bg-white rounded-lg border border-gray-200 p-5">
              <div className="flex items-start justify-between mb-3">
                <h3 className="font-medium text-gray-900">{room.name}</h3>
                <span className="text-xs font-medium px-2 py-1 rounded bg-green-50 text-green-700 border border-green-200">
                  {room.status}
                </span>
              </div>
              <p className="text-sm text-gray-500 mb-1">Code: {room.code}</p>
              <p className="text-sm text-gray-500 mb-1">Capacity: {room.capacity}</p>
              <p className="text-sm text-gray-500">Type: {room.roomType.replace(/_/g, ' ')}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
