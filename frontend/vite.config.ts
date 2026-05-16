import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [vue()],
  base: './',
  // Modern desktop targets match Vue 3 + Element Plus support.
  build: {
    target: 'es2020',
    cssTarget: 'chrome107',
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element-plus'
          if (id.includes('echarts') || id.includes('zrender') || id.includes('vue-echarts')) return 'vendor-echarts'
          if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) return 'vendor-vue'
          if (id.includes('axios')) return 'vendor-axios'
          return 'vendor'
        }
      }
    }
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        // Keep default Sass options; project styles rely on standard compiler behavior.
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
