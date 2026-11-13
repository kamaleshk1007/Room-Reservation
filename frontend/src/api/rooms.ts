// src/api/rooms.ts
import api from './client';

export interface RoomSummary {
  id: string;
  name: string;
  code: string;
  capacity: number;
  roomType: string;
  status: string;
}

export async function getRooms(): Promise<RoomSummary[]> {
  return api.get<RoomSummary[]>('/api/rooms');
}

export async function getRoomById(id: string): Promise<RoomSummary> {
  return api.get<RoomSummary>(`/api/rooms/${id}`);
}
