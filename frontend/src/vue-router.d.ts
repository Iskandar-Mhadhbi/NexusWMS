/**
 * Augments Vue Router's RouteMeta so route.meta.domain and route.meta.roles
 * type-check without `any`. Domain values match the CSS custom property
 * suffixes in styles/tokens.scss (e.g. "procurement" → --domain-procurement).
 */
import 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    domain?: string;
    roles?: string[];
  }
}