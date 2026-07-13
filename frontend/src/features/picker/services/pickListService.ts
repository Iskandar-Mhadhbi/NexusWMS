/**
 * pickListService.ts
 * Thin HTTP layer over /pick-lists, scoped to what the picker terminal
 * needs. Mirrors PickListController — see phase4/5_progress.md.
 */
import http from '@/core/services/http';
import type { PickList } from '@/core/models/pickList';

export const pickListService = {
  /** The current worker's active pick list, resolved server-side from the JWT. */
  getMyActivePickList() {
    return http.get<PickList>('/pick-lists/my');
  },

  /**
   * Confirms a pick for one item. Always sends the item's full
   * quantityToPick — this UI doesn't expose partial-quantity picking.
   */
  pickItem(pickListId: string, itemId: string, quantityPicked: number) {
    return http.post(`/pick-lists/${pickListId}/items/${itemId}/pick`, { quantityPicked });
  },
};