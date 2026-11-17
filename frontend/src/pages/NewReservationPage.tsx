// src/pages/NewReservationPage.tsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getRooms, type RoomSummary } from '../api/rooms';
import { getReservations, createReservation, type ReservationResponse } from '../api/reservations';
import { PERIODS, LAST_PERIOD_END, toMinutes, formatDuration, type Period } from '../config/periods';

function localDateTime(date: string, time: string): Date {
  const [y, mo, d] = date.split('-').map(Number);
  const [h, mi] = time.split(':').map(Number);
  return new Date(y, mo - 1, d, h, mi);
}

function overlapsWindow(start: Date, end: Date, windowStart: Date, windowEnd: Date): boolean {
  return start.getTime() < windowEnd.getTime() && end.getTime() > windowStart.getTime();
}

function reservationOverlapsPeriod(reservation: ReservationResponse, day: string, period: Period): boolean {
  const periodStart = localDateTime(day, period.start);
  const periodEnd = localDateTime(day, period.end);
  const resStart = new Date(reservation.startTime);
  const resEnd = new Date(reservation.endTime);
  if (Number.isNaN(resStart.getTime()) || Number.isNaN(resEnd.getTime())) return false;
  return overlapsWindow(resStart, resEnd, periodStart, periodEnd);
}

export function NewReservationPage() {
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [roomId, setRoomId] = useState('');
  const [date, setDate] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');
  const [purpose, setPurpose] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [roomsLoading, setRoomsLoading] = useState(true);
  const [slotLoading, setSlotLoading] = useState(false);
  const [slotError, setSlotError] = useState('');
  const [confirmed, setConfirmed] = useState<ReservationResponse[]>([]);
  const [pending, setPending] = useState<ReservationResponse[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    getRooms()
      .then(setRooms)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load rooms'))
      .finally(() => setRoomsLoading(false));
  }, []);

  async function loadSlots(selectedRoomId: string, selectedDate: string) {
    setSlotLoading(true);
    setSlotError('');
    setSelected([]);
    try {
      const from = `${selectedDate}T00:00`;
      const to = `${selectedDate}T23:59:59`;
      const [confirmedPage, pendingPage] = await Promise.all([
        getReservations({ roomId: selectedRoomId, status: 'CONFIRMED', from, to, size: 100 }),
        getReservations({ roomId: selectedRoomId, status: 'PENDING', from, to, size: 100 }),
      ]);
      setConfirmed(confirmedPage.content);
      setPending(pendingPage.content);
    } catch (err) {
      setSlotError(err instanceof Error ? err.message : 'Failed to load availability');
      setConfirmed([]);
      setPending([]);
    } finally {
      setSlotLoading(false);
    }
  }

  function periodState(period: Period): 'booked' | 'pending' | 'free' {
    if (confirmed.some((r) => reservationOverlapsPeriod(r, date, period))) return 'booked';
    if (pending.some((r) => reservationOverlapsPeriod(r, date, period))) return 'pending';
    return 'free';
  }

  function selectedIndices(): number[] {
    return selected
      .map((start) => PERIODS.findIndex((p) => p.start === start))
      .filter((i) => i >= 0)
      .sort((a, b) => a - b);
  }

  function togglePeriod(period: Period) {
    if (periodState(period) === 'booked') return;
    setSlotError('');
    setCustomStart('');
    setCustomEnd('');
    if (selected.includes(period.start)) {
      const indices = selectedIndices();
      const index = PERIODS.findIndex((p) => p.start === period.start);
      // Deselect only from the edges of the selected range so the range stays contiguous
      if (index === Math.min(...indices) || index === Math.max(...indices)) {
        setSelected(selected.filter((s) => s !== period.start));
      } else {
        setSlotError('Deselect only the first or last selected period, or clear the selection.');
      }
      return;
    }
    if (selected.length === 0) {
      setSelected([period.start]);
      return;
    }
    const indices = selectedIndices();
    const index = PERIODS.findIndex((p) => p.start === period.start);
    const min = Math.min(...indices);
    const max = Math.max(...indices);
    if (index === min - 1 || index === max + 1) {
      setSelected([...selected, period.start]);
    } else {
      setSlotError('Select consecutive periods only. A period must touch your current selection.');
    }
  }

  // Combined range from the grid selection (always contiguous)
  const selectedPeriods = PERIODS.filter((p) => selected.includes(p.start));
  const combinedStart = selectedPeriods.length > 0 ? selectedPeriods[0].start : null;
  const combinedEnd = selectedPeriods.length > 0 ? selectedPeriods[selectedPeriods.length - 1].end : null;
  const combinedMinutes =
    combinedStart && combinedEnd ? toMinutes(combinedEnd) - toMinutes(combinedStart) : 0;

  const customMinutes =
    customStart && customEnd ? toMinutes(customEnd) - toMinutes(customStart) : 0;
  const customRangeValid =
    customStart && customEnd && customMinutes > 0 &&
    toMinutes(customStart) >= toMinutes(PERIODS[0].start) &&
    toMinutes(customEnd) <= toMinutes(LAST_PERIOD_END);

  function validate(): string | null {
    if (!roomId) return 'Please select a room.';
    if (!date) return 'Please select a date.';

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const selectedDay = localDateTime(date, '00:00');
    if (selectedDay < today) return 'Please select a date in the future.';

    const usingCustom = Boolean(customStart || customEnd);
    if (usingCustom) {
      if (!customStart || !customEnd) return 'Enter both a custom start and end time, or clear them.';
      const minutes = toMinutes(customEnd) - toMinutes(customStart);
      if (minutes <= 0) return 'Custom end time must be after start time.';
      if (toMinutes(customStart) < toMinutes(PERIODS[0].start)) return `Custom time cannot start before ${PERIODS[0].start}.`;
      if (toMinutes(customEnd) > toMinutes(LAST_PERIOD_END)) return `Custom time cannot end after ${LAST_PERIOD_END}.`;
      const cs = localDateTime(date, customStart);
      const ce = localDateTime(date, customEnd);
      const clash = confirmed.find((r) => overlapsWindow(new Date(r.startTime), new Date(r.endTime), cs, ce));
      if (clash) {
        return `Custom time overlaps a confirmed booking (${new Date(clash.startTime).toLocaleString()}).`;
      }
      return null;
    }

    if (selected.length === 0) return 'Select at least one period, or enter a custom time below.';
    return null;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    const start = customStart || combinedStart;
    const end = customEnd || combinedEnd;
    if (!start || !end) {
      setError('Select a time range first.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      await createReservation({
        roomId,
        startTime: `${date}T${start}:00`,
        endTime: `${date}T${end}:00`,
        purpose: purpose || undefined,
      });
      navigate('/reservations');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create reservation');
    } finally {
      setLoading(false);
    }
  }

  if (roomsLoading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8">
        <p className="text-gray-500">Loading rooms...</p>
      </div>
    );
  }

  const allBooked = slotLoading === false && date !== '' && confirmed.length > 0 &&
    PERIODS.every((p) => periodState(p) === 'booked');

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <h2 className="text-xl font-semibold text-gray-900 mb-6">New Reservation</h2>
      <form onSubmit={handleSubmit} className="bg-white rounded-lg border border-gray-200 p-4 sm:p-6 space-y-5">
        <div>
          <label htmlFor="room" className="block text-sm font-medium text-gray-700 mb-1">
            Room
          </label>
          <select
            id="room"
            value={roomId}
            onChange={(e) => {
              const newRoomId = e.target.value;
              setRoomId(newRoomId);
              setError('');
              if (newRoomId && date) loadSlots(newRoomId, date);
            }}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            required
          >
            <option value="">Select a room</option>
            {rooms.map((room) => (
              <option key={room.id} value={room.id}>
                {room.name} ({room.code}) — Capacity: {room.capacity}
              </option>
            ))}
          </select>
          {rooms.length === 0 && (
            <p className="mt-2 text-sm text-gray-500">No rooms available.</p>
          )}
        </div>

        <div>
          <label htmlFor="date" className="block text-sm font-medium text-gray-700 mb-1">
            Date
          </label>
          <input
            id="date"
            type="date"
            value={date}
            onChange={(e) => {
              const newDate = e.target.value;
              setDate(newDate);
              setError('');
              if (roomId && newDate) loadSlots(roomId, newDate);
            }}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            required
          />
        </div>

        {roomId && date ? (
          <div>
            <span id="periods-label" className="block text-sm font-medium text-gray-700 mb-1">
              Time periods (08:00–17:00)
            </span>
            {slotLoading ? (
              <p className="text-gray-500">Loading availability...</p>
            ) : (
              <>
                {slotError && (
                  <div role="alert" className="mb-2 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
                    {slotError}
                  </div>
                )}
                <div aria-labelledby="periods-label" className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  {PERIODS.map((period) => {
                    const state = periodState(period);
                    const isSelected = selected.includes(period.start);
                    const disabled = state === 'booked';
                    return (
                      <button
                        key={period.start}
                        type="button"
                        disabled={disabled}
                        aria-pressed={isSelected}
                        aria-label={`${period.start} to ${period.end}${state === 'booked' ? ' (booked)' : state === 'pending' ? ' (pending)' : ''}`}
                        className={`rounded-md border p-2 text-left focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                          state === 'booked'
                            ? 'border-gray-200 bg-gray-100 text-gray-400 cursor-not-allowed'
                            : isSelected
                              ? 'border-blue-600 bg-blue-50 text-blue-800'
                              : state === 'pending'
                                ? 'border-amber-300 bg-amber-50 text-amber-800 hover:border-amber-400'
                                : 'border-gray-200 bg-white text-gray-800 hover:border-blue-400'
                        }`}
                        onClick={() => togglePeriod(period)}
                      >
                        <span className="block text-sm font-medium">
                          {period.start}–{period.end}
                        </span>
                        <span className="block text-xs mt-1">
                          {state === 'booked' ? 'Booked' : state === 'pending' ? 'Pending' : isSelected ? 'Selected' : 'Free'}
                        </span>
                      </button>
                    );
                  })}
                </div>
                {allBooked && (
                  <p className="mt-2 text-sm text-gray-500">All periods are booked for this date.</p>
                )}
                <div className="mt-3 text-sm text-gray-700">
                  {selected.length > 0 ? (
                    <p>
                      Selected: <span className="font-medium">{combinedStart}–{combinedEnd}</span>{' '}
                      ({formatDuration(combinedMinutes)})
                    </p>
                  ) : (
                    <p className="text-gray-500">No periods selected.</p>
                  )}
                </div>
              </>
            )}
          </div>
        ) : (
          <p className="text-sm text-gray-500">Select a room and a date to see available periods.</p>
        )}

        <div>
          <span className="block text-sm font-medium text-gray-700 mb-1">Custom time (irregular timings)</span>
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="flex-1">
              <label htmlFor="customStart" className="block text-xs text-gray-600 mb-1">From</label>
              <input
                id="customStart"
                type="time"
                min="08:00"
                max="17:00"
                value={customStart}
                onChange={(e) => {
                  setCustomStart(e.target.value);
                  setSelected([]);
                  setSlotError('');
                }}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex-1">
              <label htmlFor="customEnd" className="block text-xs text-gray-600 mb-1">To</label>
              <input
                id="customEnd"
                type="time"
                min="08:00"
                max="17:00"
                value={customEnd}
                onChange={(e) => {
                  setCustomEnd(e.target.value);
                  setSelected([]);
                  setSlotError('');
                }}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
          {customRangeValid ? (
            <p className="mt-2 text-sm text-gray-700">
              Custom range: <span className="font-medium">{customStart}–{customEnd}</span>{' '}
              ({formatDuration(customMinutes)})
            </p>
          ) : customStart || customEnd ? (
            <p className="mt-2 text-sm text-red-700">Custom range must be within 08:00–17:00 and end after start.</p>
          ) : null}
        </div>

        <div>
          <label htmlFor="purpose" className="block text-sm font-medium text-gray-700 mb-1">
            Purpose (optional)
          </label>
          <input
            id="purpose"
            type="text"
            value={purpose}
            onChange={(e) => setPurpose(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {error && (
          <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={loading}
            className="py-2 px-4 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Creating...' : 'Create Reservation'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/reservations')}
            className="py-2 px-4 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
