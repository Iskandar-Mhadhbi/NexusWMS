/**
 * Roster state — Employees pillar.
 * Top-level store; other pillars may eventually want to resolve a UUID
 * actor field to a name (see the assignedTo/dispatchedBy TODO from the
 * Fulfillment session) once that backend enrichment work happens.
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { userService } from '@/features/employees/services/userService';
import type { UserResponse, Role, UserStatus } from '@/core/models/user';

export const useUserStore = defineStore('user', () => {
  const users = ref<UserResponse[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const pendingUsers = computed(() => users.value.filter((u) => u.status === 'PENDING'));

  async function fetchAll() {
    loading.value = true;
    error.value = null;
    try {
      users.value = await userService.getAll();
    } catch (e) {
      error.value = 'Failed to load users.';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  /** Update status and patch the roster in place. */
  async function updateStatus(employeeId: string, status: UserStatus) {
    error.value = null;
    try {
      const updated = await userService.updateStatus(employeeId, { status });
      const idx = users.value.findIndex((u) => u.employeeId === employeeId);
      if (idx !== -1) users.value[idx] = updated;
      return updated;
    } catch (e) {
      error.value = 'Failed to update status.';
      throw e;
    }
  }

  /** Update role and patch the roster in place. */
  async function updateRole(employeeId: string, role: Role) {
    error.value = null;
    try {
      const updated = await userService.updateRole(employeeId, { role });
      const idx = users.value.findIndex((u) => u.employeeId === employeeId);
      if (idx !== -1) users.value[idx] = updated;
      return updated;
    } catch (e) {
      error.value = 'Failed to update role.';
      throw e;
    }
  }

  return { users, loading, error, pendingUsers, fetchAll, updateStatus, updateRole };
});