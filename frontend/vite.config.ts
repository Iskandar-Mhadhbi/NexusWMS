/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  define: {
    global: 'globalThis', // sockjs-client assumes a Node-style `global`; browsers have no such thing. This tells the bundler to replace
                          // every reference to `global` with `globalThis` at build   time, which does exist in browsers.
  },
  test: {
    environment: 'jsdom',
    globals: true, // lets you use describe/it/expect without importing them in every file
  },
});