// src/config/periods.ts
// College bell periods for the reservation slot picker.
// Edit this list directly to change the period timings.

export interface Period {
  start: string;
  end: string;
}

export const PERIODS: Period[] = [
  { start: '08:00', end: '08:40' },
  { start: '08:40', end: '09:20' },
  { start: '09:20', end: '09:30' },
  { start: '09:30', end: '10:10' },
  { start: '10:10', end: '10:50' },
  { start: '10:50', end: '11:30' },
  { start: '11:30', end: '12:10' },
  { start: '12:10', end: '12:50' },
  { start: '12:50', end: '13:30' },
  { start: '13:30', end: '14:10' },
  { start: '14:10', end: '14:50' },
  { start: '14:50', end: '15:30' },
  { start: '15:30', end: '16:10' },
  { start: '16:10', end: '16:50' },
  { start: '16:50', end: '17:00' },
];

export const FIRST_PERIOD_START = PERIODS[0].start;
export const LAST_PERIOD_END = PERIODS[PERIODS.length - 1].end;

export function toMinutes(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

export function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hours === 0) return `${mins} min`;
  if (mins === 0) return `${hours} hr`;
  return `${hours} hr ${mins} min`;
}
