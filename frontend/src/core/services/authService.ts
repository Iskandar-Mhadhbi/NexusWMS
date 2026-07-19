import http from './http';
import type { AuthResponse, User } from '@/core/models/user';

export const authService = {
  login(identifier: string, password: string) {
    return http.post<AuthResponse>('/auth/login', { identifier, password });
  },
  logout() {
    return http.post('/auth/logout');
  },
  me() {
    return http.get<User>('/users/me');
  },
};