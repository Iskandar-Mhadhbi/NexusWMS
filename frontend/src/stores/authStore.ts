import { defineStore } from 'pinia';
import { authService } from '@/core/services/authService';
import { tokenService } from '@/core/services/tokenService';
import type { User } from '@/core/models/user';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
    isAuthenticated: !!tokenService.get(),
  }),
  actions: {
    async login(identifier: string, password: string) {
      const { data } = await authService.login(identifier, password);
      tokenService.set(data.token);
      this.user = {
        employeeId: data.employeeId,
        email: data.email,
        name: data.name,
        role: data.role,
        status: data.status,
      };
      this.isAuthenticated = true;
    },

    // Rehydrates `user` from a token that survived a page refresh.
    // Without this, isAuthenticated is true on boot but user stays null,
    // which silently breaks roleGuard (it bails out early when !auth.user).
    async initialize() {
      const token = tokenService.get();
      if (token && !this.user) {
        try {
          const { data } = await authService.me();
          this.user = data;
          this.isAuthenticated = true;
        } catch {
          // Token expired, revoked, or invalid — clear and force re-login
          tokenService.clear();
          this.isAuthenticated = false;
        }
      }
    },

    logout() {
      tokenService.clear();
      this.user = null;
      this.isAuthenticated = false;
    },
  },
});