// core/models/zone.ts
/**
 * Zone/Aisle/Shelf domain models — the physical warehouse hierarchy.
 * Zone (functional area) -> Aisle (row within a zone) -> Shelf (specific
 * location on an aisle). Mirrors NexusWMS_Architecture_v2.md §10.
 *
 * Create*Schema below are the single source of truth for both the
 * TypeScript payload type AND the client-side validation rules — each
 * mirrors its backend *Request.java DTO's Bean Validation annotations
 * exactly (see comments on each field). Payload types are inferred from
 * the schema via z.infer, not hand-declared separately, so type and
 * validation rule can never drift apart.
 */
import { z } from 'zod';

export type ZoneType = 'RECEIVING' | 'STORAGE' | 'PICKING' | 'PACKING' | 'DISPATCH';
const ZONE_TYPES: ZoneType[] = ['RECEIVING', 'STORAGE', 'PICKING', 'PACKING', 'DISPATCH'];

export interface Zone {
  id: string;
  name: string;
  type: ZoneType;
  capacity: number;
  currentOccupancy: number;
  createdAt: string;
}

export interface Aisle {
  id: string;
  zoneId: string;
  code: string;
}

export interface Shelf {
  id: string;
  aisleId: string;
  level: string;  
  code: string;
  maxWeight: number | null; // no @NotNull on the entity — genuinely optional, "no limit"
  currentWeight: number;
}

/**
 * POST /api/v1/zones body.
 * Mirrors ZoneRequest.java: name/type @NotBlank, capacity @NotNull @Positive.
 */
export const CreateZoneSchema = z.object({
  name: z.string().trim().min(1, 'Zone name is required'),
  type: z.enum(ZONE_TYPES as [ZoneType, ...ZoneType[]]),
  capacity: z.number({ error: 'Capacity is required' }).positive('Capacity must be greater than zero'),
});
export type CreateZonePayload = z.infer<typeof CreateZoneSchema>;

/**
 * POST /api/v1/zones/aisles body.
 * Mirrors AisleRequest.java: zoneId presumed @NotNull, code @NotBlank.
 * Uniqueness itself (zone_id + code) is enforced server-side only — a
 * 409 is expected and correct on collision, not something to pre-validate.
 */
export const CreateAisleSchema = z.object({
  zoneId: z.uuid(),
  code: z.string().trim().min(1, 'Aisle code is required'),
});
export type CreateAislePayload = z.infer<typeof CreateAisleSchema>;

/**
 * POST /api/v1/zones/shelves body.
 * Mirrors ShelfRequest.java exactly: aisleId @NotNull, level/code @NotBlank,
 * maxWeight @PositiveOrZero and nullable (no @NotNull) — null means
 * "no configured weight limit", a real valid state, not missing data.
 */
export const CreateShelfSchema = z.object({
  aisleId: z.uuid(),
  level: z.string().trim().min(1, 'Level is required'),
  code: z.string().trim().min(1, 'Shelf code is required'),
  maxWeight: z
    .number({ error: 'Max weight must be a number' })
    .min(0, 'Max weight must be zero or greater')
    .nullable(),
});
export type CreateShelfPayload = z.infer<typeof CreateShelfSchema>;