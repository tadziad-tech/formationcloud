import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev: proxy /api to Spring Boot
// Prod: build to /app/ and copy dist into src/main/resources/static/app
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  base: mode === 'production' ? '/app/' : '/',
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static/app',
    emptyOutDir: true
  }
}))
