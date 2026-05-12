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
