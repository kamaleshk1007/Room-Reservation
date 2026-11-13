// src/api/reservations.ts
import api from './client';

export interface ReservationRequest {
  roomId: string;
  startTime: string;
  endTime: string;
  purpose?: string;
}

export interface ReservationResponse {
  id: string;
  room: {
    id: string;
    name: string;
    code: string;
    capacity: number;
    roomType: string;
    status: string;
  };
  requestedBy: {
    id: string;
    portalStaffId: string;
    name: string;
    role: string;
  };
  startTime: string;
  endTime: string;
  purpose: string;
  status: string;
  approvedBy?: {
    id: string;
    portalStaffId: string;
    name: string;
    role: string;
  };
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReservationPage {
  content: ReservationResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export async function getReservations(params?: {
  status?: string;
  roomId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
}): Promise<ReservationPage> {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set('status', params.status);
  if (params?.roomId) searchParams.set('roomId', params.roomId);
  if (params?.from) searchParams.set('from', params.from);
  if (params?.to) searchParams.set('to', params.to);
  if (params?.page !== undefined) searchParams.set('page', String(params.page));
  if (params?.size !== undefined) searchParams.set('size', String(params.size));
  if (params?.sort) searchParams.set('sort', params.sort);
  const qs = searchParams.toString();
  return api.get<ReservationPage>(`/api/reservations${qs ? '?' + qs : ''}`);
}

export async function createReservation(req: ReservationRequest): Promise<ReservationResponse> {
  return api.post<ReservationResponse>('/api/reservations', req);
}

export async function approveReservation(id: string): Promise<ReservationResponse> {
  return api.patch<ReservationResponse>(`/api/reservations/${id}/approve`);
}

export async function rejectReservation(id: string): Promise<ReservationResponse> {
  return api.patch<ReservationResponse>(`/api/reservations/${id}/reject`);
}

export async function cancelReservation(id: string): Promise<ReservationResponse> {
  return api.patch<ReservationResponse>(`/api/reservations/${id}/cancel`);
}
