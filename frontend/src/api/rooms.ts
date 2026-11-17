// src/api/rooms.ts
import api from './client';

export interface RoomSummary {
  id: string;
  name: string;
  code: string;
  capacity: number;
  roomType: string;
  status: string;
  location?: string | null;
}

export interface RoomCreateRequest {
  code: string;
  name: string;
  roomType: string;
  capacity: number;
  location?: string;
}

export interface RoomUpdateRequest {
  name: string;
  capacity: number;
  location?: string;
}

export async function getRooms(): Promise<RoomSummary[]> {
  return api.get<RoomSummary[]>('/api/rooms');
}

export async function getRoomById(id: string): Promise<RoomSummary> {
  return api.get<RoomSummary>(`/api/rooms/${id}`);
}

export async function createRoom(req: RoomCreateRequest): Promise<RoomSummary> {
  return api.post<RoomSummary>('/api/rooms', req);
}

export async function updateRoom(id: string, req: RoomUpdateRequest): Promise<RoomSummary> {
  return api.put<RoomSummary>(`/api/rooms/${id}`, req);
}

export async function deleteRoom(id: string): Promise<{ message: string }> {
  return api.delete<{ message: string }>(`/api/rooms/${id}`);
}
