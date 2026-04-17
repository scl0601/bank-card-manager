import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [vue()],
  base: './',
  // Win7 兼容：ES2019 目标，确保生成的代码在旧浏览器可运行
  build: {
    target: 'es2019',
    cssTarget: 'chrome49',
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        // 使用 legacy API，兼容 Win7 / 旧版 Node.js
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
