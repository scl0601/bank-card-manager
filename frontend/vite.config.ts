import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [vue()],
  base: './',
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler'
      }
    }
  },
  server: {
    host: '0.0.0.0',   // 监听所有网络接口，允许手机访问
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:7878',
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  }
})
