/**
 * zoneService — thin HTTP layer for Zones/Aisles/Shelves.
 * Endpoints per NexusWMS_Architecture_v2.md §15 ("Inventory — Zones/Aisles/Shelves").
 */
import http from '@/core/services/http';
import type {
  Zone, Aisle, Shelf,
  CreateZonePayload, CreateAislePayload, CreateShelfPayload,
} from '@/core/models/zone';

export const zoneService = {
  async getZones(): Promise<Zone[]> {
    const { data } = await http.get<Zone[]>('/zones');
    return data;
  },

  async createZone(payload: CreateZonePayload): Promise<Zone> {
    const { data } = await http.post<Zone>('/zones', payload);
    return data;
  },

  async getAisles(zoneId: string): Promise<Aisle[]> {
    const { data } = await http.get<Aisle[]>(`/zones/${zoneId}/aisles`);
    return data;
  },

  async createAisle(payload: CreateAislePayload): Promise<Aisle> {
    const { data } = await http.post<Aisle>('/zones/aisles', payload);
    return data;
  },

  async getShelves(zoneId: string): Promise<Shelf[]> {
    const { data } = await http.get<Shelf[]>(`/zones/${zoneId}/shelves`);
    return data;
  },

  async createShelf(payload: CreateShelfPayload): Promise<Shelf> {
    const { data } = await http.post<Shelf>('/zones/shelves', payload);
    return data;
  },
};