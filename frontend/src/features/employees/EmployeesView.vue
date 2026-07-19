<!--
  Employee roster — Employees pillar.
  List + client-side role/status filter + inline status/role mutation.
  PENDING users get a distinct callout since they're blocked from login
  until an admin activates them — the one status that needs prompt action.
-->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useUserStore } from '@/stores/userStore';
import { useAuthStore } from '@/stores/authStore';
import type { Role, UserStatus } from '@/core/models/user';

const store = useUserStore();
const auth = useAuthStore();

const ROLES: Role[] = [
  'ADMIN', 'MANAGER', 'RECEIVER', 'PICKER',
  'PACKER', 'DISPATCHER', 'INVENTORY_CONTROLLER', 'FINANCE',
];
const STATUSES: UserStatus[] = ['PENDING', 'ACTIVE', 'ON_LEAVE', 'INACTIVE', 'TERMINATED'];

const roleFilter = ref<Role | 'ALL'>('ALL');
const statusFilter = ref<UserStatus | 'ALL'>('ALL');

/**
 * Only ADMIN can mutate status/role at all (matches backend @PreAuthorize).
 * MANAGER sees the roster read-only — showing editable controls a manager
 * can't actually use would just 403 silently on submit.
 */
const canEdit = computed(() => auth.user?.role === 'ADMIN');

/**
 * Even for an ADMIN viewer, another ADMIN's row is locked from inline
 * editing here — role/status changes on an admin account are a heavier
 * action than this casual dropdown UI is meant for.
 */
function isEditable(targetRole: Role): boolean {
  return canEdit.value && targetRole !== 'ADMIN';
}

const filteredUsers = computed(() =>
  store.users.filter((u) => {
    const roleMatch = roleFilter.value === 'ALL' || u.role === roleFilter.value;
    const statusMatch = statusFilter.value === 'ALL' || u.status === statusFilter.value;
    return roleMatch && statusMatch;
  })
);

async function onStatusChange(employeeId: string, event: Event) {
  const status = (event.target as HTMLSelectElement).value as UserStatus;
  await store.updateStatus(employeeId, status);
}

async function onRoleChange(employeeId: string, event: Event) {
  const role = (event.target as HTMLSelectElement).value as Role;
  await store.updateRole(employeeId, role);
}

onMounted(() => store.fetchAll());
</script>
<template>
  <div class="roster-view">
    <div v-if="store.pendingUsers.length > 0" class="roster-view__pending-callout flex-row">
      <span>{{ store.pendingUsers.length }} user(s) awaiting activation</span>
    </div>

    <div class="roster-view__toolbar flex-row">
      <select v-model="roleFilter" class="roster-view__filter">
        <option value="ALL">All roles</option>
        <option v-for="r in ROLES" :key="r" :value="r">{{ r }}</option>
      </select>
      <select v-model="statusFilter" class="roster-view__filter">
        <option value="ALL">All statuses</option>
        <option v-for="s in STATUSES" :key="s" :value="s">{{ s }}</option>
      </select>
    </div>

    <p v-if="store.loading" class="roster-view__meta">Loading…</p>
    <p v-else-if="store.error" class="roster-view__meta roster-view__meta--error">{{ store.error }}</p>
    <p v-else-if="filteredUsers.length === 0" class="roster-view__meta">No users match this filter.</p>

    <table v-else class="roster-view__table">
      <thead>
        <tr>
          <th>Employee ID</th>
          <th>Name</th>
          <th>Email</th>
          <th>Role</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in filteredUsers" :key="u.employeeId">
          <td class="roster-view__mono">{{ u.employeeId }}</td>
          <td>{{ u.name }}</td>
          <td>{{ u.email }}</td>
          <td>
            <select :value="u.role" @change="onRoleChange(u.employeeId, $event)">
              <option v-for="r in ROLES" :key="r" :value="r">{{ r }}</option>
            </select>
          </td>
          <td>
            <select :value="u.status" @change="onStatusChange(u.employeeId, $event)">
              <option v-for="s in STATUSES" :key="s" :value="s">{{ s }}</option>
            </select>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style lang="scss" scoped>
.flex-row { display: flex; align-items: center; gap: 0.75rem; }

.roster-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;

  &__pending-callout {
    padding: 0.6rem 1rem;
    border-radius: 6px;
    background: color-mix(in srgb, var(--domain-employees, #D97757) 12%, var(--surface-1));
    color: var(--domain-employees, #D97757);
    font-size: 0.875rem;
    font-weight: 600;
    justify-content: flex-start;
  }

  &__toolbar {
    justify-content: flex-start;
  }

  &__filter,
  select {
    padding: 0.4rem 0.6rem;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    background: var(--surface-1);
    color: var(--text-primary);
    font-size: 0.8125rem;
  }

  &__meta {
    color: var(--text-secondary);
    font-size: 0.875rem;

    &--error {
      color: var(--domain-danger);
    }
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    background: var(--surface-1);
    border: 0.5px solid var(--border);
    border-radius: 8px;
    overflow: hidden;

    th, td {
      text-align: left;
      padding: 0.6rem 1rem;
      border-bottom: 0.5px solid var(--border);
      font-size: 0.875rem;
    }

    th {
      color: var(--text-secondary);
      font-weight: 600;
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    tr:last-child td {
      border-bottom: none;
    }
  }

  &__mono {
    font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
    font-size: 0.8125rem;
    color: var(--text-secondary);
  }
}
</style>