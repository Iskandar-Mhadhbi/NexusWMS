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