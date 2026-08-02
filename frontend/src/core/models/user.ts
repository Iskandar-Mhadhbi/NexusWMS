export type Role =
  | 'ADMIN' | 'MANAGER' | 'RECEIVER' | 'PICKER'
  | 'PACKER' | 'DISPATCHER' | 'INVENTORY_CONTROLLER' | 'FINANCE';

export type UserStatus = 'PENDING' | 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE' | 'TERMINATED';

export interface User {
  employeeId: string;
  name: string;
  email: string;
  role: Role;
  status: UserStatus;
}

// Matches com.nexuswms.auth.dto.response.AuthResponse exactly — flat, not nested
export interface AuthResponse {
  token: string;
  employeeId: string;
  email: string;
  name: string;
  role: Role;
  status: UserStatus;
}

export interface UserResponse {
  employeeId: string;
  email: string;
  name: string;
  role: Role;
  status: UserStatus;
}

export interface UpdateStatusRequest {
  status: UserStatus;
}

export interface UpdateRoleRequest {
  role: Role;
}

// Matches com.nexuswms.user.dto.response.UserSummaryResponse.
// Distinct from User/UserResponse above: this one includes the raw id
// (UUID) alongside employeeId, since it's used as an enrichment payload
// for actor fields (assignedTo, createdBy, etc.) across other entities —
// the id is needed for keying/linking, employeeId is what's displayed.
export interface UserSummary {
  id: string;
  employeeId: string;
  name: string;
  email: string;
  role: Role;
  status: UserStatus;
}