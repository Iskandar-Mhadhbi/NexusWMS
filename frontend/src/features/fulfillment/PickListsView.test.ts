import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import PickListsView from '@/features/fulfillment/PickListsView.vue';
import { pickListOversightService } from '@/features/fulfillment/services/pickListOversightService';
import type { PickList } from '@/core/models/pickList';

vi.mock('@/features/fulfillment/services/pickListOversightService', () => ({
  pickListOversightService: {
    getAll: vi.fn(),
  },
}));

const fakePickList: PickList = {
  id: 'pl-12345678-abcd',
  fulfillmentRequestId: 'fr-1',
  generatedBy: { id: 'u-1', employeeId: 'EMP-0001', name: 'Manager One', email: 'm1@x.com', role: 'MANAGER' as any, status: 'ACTIVE' as any },
  assignedTo: { id: 'u-2', employeeId: 'EMP-0002', name: 'Picker One', email: 'p1@x.com', role: 'PICKER' as any, status: 'ACTIVE' as any },
  status: 'GENERATED',
  generatedAt: '2026-08-04T10:00:00Z',
  completedAt: null,
  items: [
    {
      id: 'item-1',
      orderLineId: 'ol-1',
      skuId: 'sku-1',
      skuCode: 'SKU-001',
      shelfId: 'shelf-1',
      shelfCode: 'A1-G01',
      quantityToPick: 5,
      quantityPicked: 2,
      status: 'PENDING',
      pickedAt: null,
    },
  ],
};

describe('PickListsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('fetches and renders pick lists on mount', async () => {
    vi.mocked(pickListOversightService.getAll).mockResolvedValue([fakePickList]);

    const wrapper = mount(PickListsView);
    await flushPromises();

    expect(pickListOversightService.getAll).toHaveBeenCalledWith(undefined);
    expect(wrapper.text()).not.toContain('No pick lists found.');
  });

  it('renders assignedTo and generatedBy by employeeId, not raw UUID (regression: .slice() crash on UserSummary object)', async () => {
    vi.mocked(pickListOversightService.getAll).mockResolvedValue([fakePickList]);

    const wrapper = mount(PickListsView);
    await flushPromises();

    expect(wrapper.text()).toContain('EMP-0002'); // assignedTo
    expect(wrapper.text()).toContain('by EMP-0001'); // generatedBy
    expect(wrapper.text()).not.toContain('[object Object]');
  });

  it('does not render a generatedBy line when generatedBy is null', async () => {
    const pickListWithoutGenerator: PickList = { ...fakePickList, generatedBy: null };
    vi.mocked(pickListOversightService.getAll).mockResolvedValue([pickListWithoutGenerator]);

    const wrapper = mount(PickListsView);
    await flushPromises();

    expect(wrapper.find('.pick-lists-view__generated-by').exists()).toBe(false);
  });

  it('shows the loading state before the fetch resolves', async () => {
    let resolvePromise: (value: PickList[]) => void;
    const pending = new Promise<PickList[]>((resolve) => { resolvePromise = resolve; });
    vi.mocked(pickListOversightService.getAll).mockReturnValue(pending);

    const wrapper = mount(PickListsView);
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain('Loading…');

    resolvePromise!([]);
    await flushPromises();

    expect(wrapper.text()).not.toContain('Loading…');
  });

  it('shows the error state when the fetch fails', async () => {
    vi.mocked(pickListOversightService.getAll).mockRejectedValue(new Error('network error'));

    const wrapper = mount(PickListsView);
    await flushPromises();

    expect(wrapper.text()).toContain('Failed to load pick lists.');
  });

  it('shows the empty state when no pick lists are returned', async () => {
    vi.mocked(pickListOversightService.getAll).mockResolvedValue([]);

    const wrapper = mount(PickListsView);
    await flushPromises();

    expect(wrapper.text()).toContain('No pick lists found.');
  });

  it('expands a row to show its items on click, and collapses on second click', async () => {
    vi.mocked(pickListOversightService.getAll).mockResolvedValue([fakePickList]);

    const wrapper = mount(PickListsView);
    await flushPromises();

    expect(wrapper.find('.pick-lists-view__items').exists()).toBe(false);

    await wrapper.find('.pick-lists-view__row-header').trigger('click');
    expect(wrapper.find('.pick-lists-view__items').exists()).toBe(true);
    expect(wrapper.text()).toContain('A1-G01');
    expect(wrapper.text()).toContain('SKU-001');
    expect(wrapper.text()).toContain('2 / 5');

    await wrapper.find('.pick-lists-view__row-header').trigger('click');
    expect(wrapper.find('.pick-lists-view__items').exists()).toBe(false);
  });

  it('re-fetches with the selected status when the filter changes', async () => {
    vi.mocked(pickListOversightService.getAll).mockResolvedValue([]);

    const wrapper = mount(PickListsView);
    await flushPromises();

    const select = wrapper.find('.pick-lists-view__filter');
    await select.setValue('COMPLETED');

    expect(pickListOversightService.getAll).toHaveBeenLastCalledWith('COMPLETED');
  });
});