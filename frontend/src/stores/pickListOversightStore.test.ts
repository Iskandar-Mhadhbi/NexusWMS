import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { usePickListOversightStore } from '@/stores/pickListOversightStore';
import { pickListOversightService } from '@/features/fulfillment/services/pickListOversightService';
import type { PickList } from '@/core/models/pickList';

// Replaces the real service module — getAll() becomes a controllable mock function.
vi.mock('@/features/fulfillment/services/pickListOversightService', () => ({
  pickListOversightService: {
    getAll: vi.fn(),
  },
}));

describe('pickListOversightStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  const fakePickList: PickList = {
    id: 'pl-1',
    fulfillmentRequestId: 'fr-1',
    generatedBy: { id: 'u-1', employeeId: 'EMP-0001', name: 'Manager One', email: 'm1@x.com', role: 'MANAGER' as any, status: 'ACTIVE' as any },
    assignedTo: { id: 'u-2', employeeId: 'EMP-0002', name: 'Picker One', email: 'p1@x.com', role: 'PICKER' as any, status: 'ACTIVE' as any },
    status: 'GENERATED',
    generatedAt: '2026-08-04T10:00:00Z',
    completedAt: null,
    items: [],
  };

  it('starts with empty state', () => {
    const store = usePickListOversightStore();

    expect(store.pickLists).toEqual([]);
    expect(store.loading).toBe(false);
    expect(store.error).toBeNull();
  });

  it('fetchAll populates pickLists on success', async () => {
    vi.mocked(pickListOversightService.getAll).mockResolvedValue([fakePickList]);
    const store = usePickListOversightStore();

    await store.fetchAll();

    expect(store.pickLists).toEqual([fakePickList]);
    expect(store.error).toBeNull();
    expect(store.loading).toBe(false);
  });

  it('fetchAll passes the status filter through to the service', async () => {
    vi.mocked(pickListOversightService.getAll).mockResolvedValue([]);
    const store = usePickListOversightStore();

    await store.fetchAll('COMPLETED');

    expect(pickListOversightService.getAll).toHaveBeenCalledWith('COMPLETED');
  });

  it('fetchAll sets error and re-throws on failure', async () => {
    vi.mocked(pickListOversightService.getAll).mockRejectedValue(new Error('network error'));
    const store = usePickListOversightStore();

    await expect(store.fetchAll()).rejects.toThrow('network error');

    expect(store.error).toBe('Failed to load pick lists.');
    expect(store.loading).toBe(false);
  });

  it('loading is true during the fetch and false after', async () => {
    let resolvePromise: (value: PickList[]) => void;
    const pending = new Promise<PickList[]>((resolve) => { resolvePromise = resolve; });
    vi.mocked(pickListOversightService.getAll).mockReturnValue(pending);
    const store = usePickListOversightStore();

    const fetchPromise = store.fetchAll();
    expect(store.loading).toBe(true);

    resolvePromise!([]);
    await fetchPromise;

    expect(store.loading).toBe(false);
  });
});