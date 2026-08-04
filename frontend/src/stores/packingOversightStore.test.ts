import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { usePackingOversightStore } from '@/stores/packingOversightStore';
import { packingOversightService } from '@/features/fulfillment/services/packingOversightService';
import type { PackingTask } from '@/core/models/packingTask';

vi.mock('@/features/fulfillment/services/packingOversightService', () => ({
  packingOversightService: {
    getAll: vi.fn(),
  },
}));

describe('packingOversightStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  const fakeTask: PackingTask = {
    id: 'task-1',
    pickListId: 'pl-1',
    taskNumber: 'PT-20260804-00001',
    assignedTo: { id: 'u-1', employeeId: 'EMP-0001', name: 'Packer One', email: 'p1@x.com', role: 'PACKER' as any, status: 'ACTIVE' as any },
    startedBy: null,
    stationId: null,
    stationCode: null,
    status: 'PENDING',
    startedAt: null,
    completedAt: null,
  };

  it('starts with empty state', () => {
    const store = usePackingOversightStore();

    expect(store.tasks).toEqual([]);
    expect(store.loading).toBe(false);
    expect(store.error).toBeNull();
  });

  it('fetchAll populates tasks on success', async () => {
    vi.mocked(packingOversightService.getAll).mockResolvedValue([fakeTask]);
    const store = usePackingOversightStore();

    await store.fetchAll();

    expect(store.tasks).toEqual([fakeTask]);
    expect(store.error).toBeNull();
    expect(store.loading).toBe(false);
  });

  it('fetchAll passes the status filter through to the service', async () => {
    vi.mocked(packingOversightService.getAll).mockResolvedValue([]);
    const store = usePackingOversightStore();

    await store.fetchAll('COMPLETED');

    expect(packingOversightService.getAll).toHaveBeenCalledWith('COMPLETED');
  });

  it('fetchAll sets error and re-throws on failure', async () => {
    vi.mocked(packingOversightService.getAll).mockRejectedValue(new Error('network error'));
    const store = usePackingOversightStore();

    await expect(store.fetchAll()).rejects.toThrow('network error');

    expect(store.error).toBe('Failed to load packing tasks.');
    expect(store.loading).toBe(false);
  });

  it('loading is true during the fetch and false after', async () => {
    let resolvePromise: (value: PackingTask[]) => void;
    const pending = new Promise<PackingTask[]>((resolve) => { resolvePromise = resolve; });
    vi.mocked(packingOversightService.getAll).mockReturnValue(pending);
    const store = usePackingOversightStore();

    const fetchPromise = store.fetchAll();
    expect(store.loading).toBe(true);

    resolvePromise!([]);
    await fetchPromise;

    expect(store.loading).toBe(false);
  });
});