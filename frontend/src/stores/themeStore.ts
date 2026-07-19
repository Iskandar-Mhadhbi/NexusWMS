import { defineStore } from 'pinia';

export type Theme = 'light' | 'dim' | 'dark';
const STORAGE_KEY = 'nexus_theme';

export const useThemeStore = defineStore('theme', {
  state: () => ({
    theme: (localStorage.getItem(STORAGE_KEY) as Theme) || 'light',
  }),
  actions: {
    setTheme(theme: Theme) {
      this.theme = theme;
      localStorage.setItem(STORAGE_KEY, theme);
      document.documentElement.setAttribute('data-theme', theme);
    },
    // Applies the persisted choice on boot — same pattern as auth's initialize()
    initialize() {
      document.documentElement.setAttribute('data-theme', this.theme);
    },
  },
});