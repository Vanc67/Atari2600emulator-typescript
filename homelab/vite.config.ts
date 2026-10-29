import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  // Относительные пути нужны и для web-хостинга, и для WebView внутри APK (file:///android_asset)
  base: './',
  plugins: [react()],
  build: {
    outDir: 'dist',
    assetsInlineLimit: 4096,
    chunkSizeWarningLimit: 900,
    target: 'es2019',
  },
  server: {
    host: '0.0.0.0',
    port: 3100,
    strictPort: true,
    allowedHosts: true,
  },
  preview: {
    host: '0.0.0.0',
    port: 3100,
    strictPort: true,
    allowedHosts: true,
  },
});
