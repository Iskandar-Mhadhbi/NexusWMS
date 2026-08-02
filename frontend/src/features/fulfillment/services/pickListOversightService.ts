
import  http  from '@/core/services/http';
import type { PickList, PickListStatus } from '@/core/models/pickList';

export const pickListOversightService = {
  async getAll(status?: PickListStatus): Promise<PickList[]> {
    const params = status ? { status } : undefined;
    const { data } = await http.get<PickList[]>('/pick-lists', { params });
    return data;
  },

  async getById(id: string): Promise<PickList> {
    const { data } = await http.get<PickList>(`/pick-lists/${id}`);
    return data;
  },
};