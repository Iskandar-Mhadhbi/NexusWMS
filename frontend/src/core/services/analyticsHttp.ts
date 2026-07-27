/**
 * Axios instance for the FastAPI analytics service (port 8001), separate
 * from the Spring Boot backend's http.ts instance.
 *
 * Same JWT is reused across both services (shared secret — see
 * finance-dashboard-dotnet-full-status.md's shared-secret microservices
 * pattern, and NexusWMS's own Phase 8 notes: FastAPI decodes the same
 * token Spring Boot issues). Auth-header attachment and 401 handling
 * mirror http.ts exactly rather than diverging into a second pattern.
 *
 * Analytics endpoints are ADMIN/MANAGER-gated by FastAPI's
 * require_manager dependency — a non-manager role would get a 403 here,
 * not a 401. Not handled specially: Reports is only reachable through
 * the manager shell, already gated by roleGuard, so this shouldn't
 * occur in practice.
 */

import axios from 'axios';
import { tokenService } from './tokenService';

const analyticsHttp = axios.create({
  baseURL: import.meta.env.VITE_ANALYTICS_BASE_URL,
});

analyticsHttp.interceptors.request.use((config) => {
  const token = tokenService.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

analyticsHttp.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      tokenService.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default analyticsHttp;