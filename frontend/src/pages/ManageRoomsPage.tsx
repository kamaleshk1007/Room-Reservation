// src/pages/ManageRoomsPage.tsx
import { useEffect, useState, useRef } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { useDialog } from '../hooks/useDialog';
import { ApiError } from '../api/client';
import {
  getRooms, createRoom, updateRoom, deleteRoom,
  type RoomSummary,
} from '../api/rooms';

const ROOM_TYPES: { value: string; label: string }[] = [
  { value: 'AV_HALL', label: 'AV hall' },
  { value: 'AUDITORIUM', label: 'Auditorium' },
  { value: 'STUDIO_LAB', label: 'Studio lab' },
  { value: 'INNOVATION_LAB', label: 'Innovation lab' },
  { value: 'TRAINING', label: 'Training room' },
];

const EMPTY_FORM = { code: '', name: '', roomType: 'AV_HALL', capacity: '', location: '' };

export function ManageRoomsPage() {
  const { user } = useAuth();
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const toastTimer = useRef<number | null>(null);
  const [dialogRoom, setDialogRoom] = useState<RoomSummary | null>(null);
  const [deleting, setDeleting] = useState(false);

  const [editing, setEditing] = useState<RoomSummary | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const dialogRef = useDialog({ open: dialogRoom !== null, onClose: () => setDialogRoom(null) });

  async function loadRooms() {
    setLoading(true);
    setError('');
    try {
      const data = await getRooms();
      setRooms(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rooms');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let cancelled = false;
    getRooms()
      .then((data) => { if (!cancelled) setRooms(data); })
      .catch((err) => { if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load rooms'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  function showToast(message: string) {
    if (toastTimer.current) {
      window.clearTimeout(toastTimer.current);
    }
    setToast(message);
    toastTimer.current = window.setTimeout(() => setToast(''), 6000);
  }

  useEffect(() => () => {
    if (toastTimer.current) window.clearTimeout(toastTimer.current);
  }, []);

  function resetForm() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setFieldErrors({});
    setFormError('');
  }

  function startEdit(room: RoomSummary) {
    setEditing(room);
    setFieldErrors({});
    setFormError('');
    setForm({
      code: room.code,
      name: room.name,
      roomType: room.roomType,
      capacity: String(room.capacity),
      location: room.location ?? '',
    });
  }

  function setField(field: keyof typeof EMPTY_FORM, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
    setFieldErrors((e) => {
      const rest = { ...e };
      delete rest[field];
      return rest;
    });
    setFormError('');
  }

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};
    if (!editing && !form.code.trim()) errors.code = 'Code is required.';
    if (!editing && form.code.trim().length > 50) errors.code = 'Code must be at most 50 characters.';
    if (!form.name.trim()) errors.name = 'Name is required.';
    const capacity = Number(form.capacity);
    if (!form.capacity || Number.isNaN(capacity) || capacity < 1) errors.capacity = 'Capacity must be at least 1.';
    if (form.location.length > 255) errors.location = 'Location must be at most 255 characters.';
    return errors;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    setFormError('');
    if (Object.keys(errors).length > 0) return;

    const capacity = Number(form.capacity);
    const location = form.location.trim() || undefined;

    setSubmitting(true);
    try {
      if (editing) {
        await updateRoom(editing.id, { name: form.name.trim(), capacity, location });
      } else {
        await createRoom({
          code: form.code.trim(),
          name: form.name.trim(),
          roomType: form.roomType,
          capacity,
          location,
        });
      }
      resetForm();
      await loadRooms();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.details && Object.keys(err.details).length > 0) {
          setFieldErrors(err.details);
        } else if (err.status === 409 && !editing) {
          setFieldErrors({ code: err.message });
        } else {
          setFormError(err.message);
        }
      } else {
        setFormError(err instanceof Error ? err.message : 'Request failed. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function confirmDelete() {
    if (!dialogRoom) return;
    setDeleting(true);
    try {
      await deleteRoom(dialogRoom.id);
      setDialogRoom(null);
      await loadRooms();
    } catch (err) {
      setDialogRoom(null);
      showToast(err instanceof Error ? err.message : 'Failed to delete room.');
    } finally {
      setDeleting(false);
    }
  }

  if (user?.role !== 'HOD') {
    return <Navigate to="/dashboard" replace />;
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <p className="text-gray-500">Loading rooms...</p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {toast && (
        <div
          role="alert"
          className="fixed top-4 right-4 z-50 max-w-sm rounded-md bg-red-700 px-4 py-3 text-sm text-white shadow-lg flex items-start gap-3"
        >
          <span className="flex-1">{toast}</span>
          <button
            type="button"
            onClick={() => setToast('')}
            aria-label="Dismiss notification"
            className="font-semibold hover:text-red-100 focus:outline-none focus:ring-2 focus:ring-white rounded"
          >
            ×
          </button>
        </div>
      )}

      <h2 className="text-xl font-semibold text-gray-900 mb-6">Manage Rooms</h2>

      {error && (
        <div role="alert" className="mb-4 p-4 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Room list */}
        <section aria-label="Room list" className="bg-white rounded-lg border border-gray-200 p-4 sm:p-6">
          <h3 className="text-base font-semibold text-gray-900 mb-4">Active rooms ({rooms.length})</h3>
          {rooms.length === 0 ? (
            <p className="text-gray-500">No active rooms. Add one using the form.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] text-sm">
                <thead className="text-left text-gray-500 border-b border-gray-200">
                  <tr>
                    <th scope="col" className="py-2 pr-4 font-medium">Code</th>
                    <th scope="col" className="py-2 pr-4 font-medium">Name</th>
                    <th scope="col" className="py-2 pr-4 font-medium">Type</th>
                    <th scope="col" className="py-2 pr-4 font-medium">Capacity</th>
                    <th scope="col" className="py-2 pr-4 font-medium">Location</th>
                    <th scope="col" className="py-2 font-medium sr-only">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {rooms.map((room) => (
                    <tr key={room.id}>
                      <td className="py-2 pr-4 font-medium text-gray-900">{room.code}</td>
                      <td className="py-2 pr-4 text-gray-700">{room.name}</td>
                      <td className="py-2 pr-4 text-gray-600">{room.roomType.replace(/_/g, ' ')}</td>
                      <td className="py-2 pr-4 text-gray-600">{room.capacity}</td>
                      <td className="py-2 pr-4 text-gray-600">{room.location ?? '—'}</td>
                      <td className="py-2 text-right whitespace-nowrap">
                        <button
                          type="button"
                          onClick={() => startEdit(room)}
                          className="text-sm text-blue-600 hover:text-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded mr-4"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => setDialogRoom(room)}
                          className="text-sm text-red-600 hover:text-red-800 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
                        >
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Add / edit form */}
        <section aria-label={editing ? 'Edit room' : 'Add room'} className="bg-white rounded-lg border border-gray-200 p-4 sm:p-6">
          <h3 className="text-base font-semibold text-gray-900 mb-4">{editing ? `Edit ${editing.code}` : 'Add room'}</h3>
          {formError && (
            <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
              {formError}
            </div>
          )}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="code" className="block text-sm font-medium text-gray-700 mb-1">
                Code
              </label>
              <input
                id="code"
                type="text"
                value={form.code}
                readOnly={!!editing}
                aria-invalid={Boolean(fieldErrors.code)}
                aria-describedby={fieldErrors.code ? 'code-error' : undefined}
                onChange={(e) => setField('code', e.target.value)}
                className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  editing ? 'bg-gray-50 text-gray-500 border-gray-200' : 'border-gray-300'
                }`}
              />
              {fieldErrors.code && (
                <p id="code-error" role="alert" className="mt-1 text-sm text-red-600">{fieldErrors.code}</p>
              )}
            </div>
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                Name
              </label>
              <input
                id="name"
                type="text"
                value={form.name}
                aria-invalid={Boolean(fieldErrors.name)}
                aria-describedby={fieldErrors.name ? 'name-error' : undefined}
                onChange={(e) => setField('name', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {fieldErrors.name && (
                <p id="name-error" role="alert" className="mt-1 text-sm text-red-600">{fieldErrors.name}</p>
              )}
            </div>
            <div>
              <label htmlFor="roomType" className="block text-sm font-medium text-gray-700 mb-1">
                Type
              </label>
              <select
                id="roomType"
                value={form.roomType}
                disabled={!!editing}
                onChange={(e) => setField('roomType', e.target.value)}
                className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  editing ? 'bg-gray-50 text-gray-500' : ''
                }`}
              >
                {ROOM_TYPES.map((type) => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="capacity" className="block text-sm font-medium text-gray-700 mb-1">
                Capacity
              </label>
              <input
                id="capacity"
                type="number"
                min={1}
                value={form.capacity}
                aria-invalid={Boolean(fieldErrors.capacity)}
                aria-describedby={fieldErrors.capacity ? 'capacity-error' : undefined}
                onChange={(e) => setField('capacity', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {fieldErrors.capacity && (
                <p id="capacity-error" role="alert" className="mt-1 text-sm text-red-600">{fieldErrors.capacity}</p>
              )}
            </div>
            <div>
              <label htmlFor="location" className="block text-sm font-medium text-gray-700 mb-1">
                Location (optional)
              </label>
              <input
                id="location"
                type="text"
                value={form.location}
                aria-invalid={Boolean(fieldErrors.location)}
                aria-describedby={fieldErrors.location ? 'location-error' : undefined}
                onChange={(e) => setField('location', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {fieldErrors.location && (
                <p id="location-error" role="alert" className="mt-1 text-sm text-red-600">{fieldErrors.location}</p>
              )}
            </div>
            <div className="flex gap-3">
              <button
                type="submit"
                disabled={submitting}
                className="py-2 px-4 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? 'Saving...' : editing ? 'Save changes' : 'Add room'}
              </button>
              {editing && (
                <button
                  type="button"
                  onClick={resetForm}
                  className="py-2 px-4 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </section>
      </div>

      {dialogRoom && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div
            ref={dialogRef}
            tabIndex={-1}
            role="dialog"
            aria-modal="true"
            aria-labelledby="remove-room-title"
            className="bg-white rounded-lg p-6 max-w-sm w-full mx-4 focus:outline-none"
          >
            <h3 id="remove-room-title" className="text-lg font-semibold text-gray-900 mb-2">Remove Room</h3>
            <p className="text-sm text-gray-600 mb-4">
              Remove room {dialogRoom.code} ({dialogRoom.name})? Rooms with upcoming confirmed or pending
              reservations cannot be removed.
            </p>
            <div className="flex gap-3 justify-end">
              <button
                type="button"
                onClick={() => setDialogRoom(null)}
                className="px-3 py-2 text-sm bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                Keep it
              </button>
              <button
                type="button"
                onClick={confirmDelete}
                disabled={deleting}
                className="px-3 py-2 text-sm bg-red-600 text-white rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
              >
                {deleting ? 'Removing...' : 'Remove'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
