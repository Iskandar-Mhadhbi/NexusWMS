import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from '@/router/index.ts';
import { useAuthStore } from './stores/authStore.ts';
import { useThemeStore } from './stores/themeStore.ts';
import './styles/tokens.scss';
import './styles/global.scss';

const app = createApp(App);
app.use(createPinia());
app.use(router);

useThemeStore().initialize(); // Apply persisted theme choice on boot

const auth = useAuthStore();
auth.initialize().finally(() => {
  app.mount('#app');
});