/**
 * User/roster API service — Employees pillar.
 * Wraps UserController: list, status update, role update (all ADMIN-gated
 * on the mutation endpoints; list is ADMIN/MANAGER).
 */

import   http   from '@/core/services/http';
import type {  UserResponse, UpdateStatusRequest, UpdateRoleRequest } from '@/core/models/user';

export const userService = {
  async getAll(): Promise<UserResponse[]> {
    const { data } = await http.get<UserResponse[]>('/users');
    return data;
  },

  async updateStatus(employeeId: string, payload: UpdateStatusRequest): Promise<UserResponse> {
    const { data } = await http.patch<UserResponse>(`/users/${employeeId}/status`, payload);
    return data;
  },

  async updateRole(employeeId: string, payload: UpdateRoleRequest): Promise<UserResponse> {
    const { data } = await http.patch<UserResponse>(`/users/${employeeId}/role`, payload);
    return data;
  },
};