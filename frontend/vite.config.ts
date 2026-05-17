import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))

function kebabCase(value: string) {
  return value.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()
}

function elementPlusComponentResolver() {
  const componentDirMap: Record<string, string> = {
    ElAvatarGroup: 'avatar',
    ElBreadcrumbItem: 'breadcrumb',
    ElButtonGroup: 'button',
    ElCarouselItem: 'carousel',
    ElCheckboxButton: 'checkbox',
    ElCheckboxGroup: 'checkbox',
    ElCol: 'col',
    ElContainer: 'container',
    ElAside: 'container',
    ElFooter: 'container',
    ElHeader: 'container',
    ElMain: 'container',
    ElDescriptionsItem: 'descriptions',
    ElDropdownItem: 'dropdown',
    ElDropdownMenu: 'dropdown',
    ElFormItem: 'form',
    ElMenuItem: 'menu',
    ElMenuItemGroup: 'menu',
    ElSubMenu: 'menu',
    ElOption: 'select',
    ElOptionGroup: 'select',
    ElRadioButton: 'radio',
    ElRadioGroup: 'radio',
    ElSkeletonItem: 'skeleton',
    ElStep: 'steps',
    ElTabPane: 'tabs',
    ElTableColumn: 'table',
    ElTimelineItem: 'timeline'
  }
  const sideEffects = (name: string) => [
    'element-plus/es/components/base/style/css',
    `element-plus/es/components/${name}/style/css`
  ]

  return [
    {
      type: 'component' as const,
      resolve(name: string) {
        if (!/^El[A-Z]/.test(name) || /^ElIcon.+/.test(name)) return undefined
        const componentName = componentDirMap[name] || kebabCase(name.slice(2))
        return {
          name,
          from: `element-plus/es/components/${componentName}/index.mjs`,
          sideEffects: sideEffects(componentName)
        }
      }
    },
    {
      type: 'directive' as const,
      resolve(name: string) {
        const directives: Record<string, { importName: string; componentName: string }> = {
          Loading: { importName: 'ElLoadingDirective', componentName: 'loading' },
          Popover: { importName: 'ElPopoverDirective', componentName: 'popover' },
          InfiniteScroll: { importName: 'ElInfiniteScroll', componentName: 'infinite-scroll' }
        }
        const directive = directives[name]
        if (!directive) return undefined
        return {
          name: directive.importName,
          from: `element-plus/es/components/${directive.componentName}/index.mjs`,
          sideEffects: sideEffects(directive.componentName)
        }
      }
    }
  ]
}

function vendorChunk(id: string) {
  const normalized = id.replace(/\\/g, '/')
  if (!normalized.includes('node_modules')) return undefined

  if (normalized.includes('@element-plus/icons-vue')) return 'vendor-element-icons'
  if (normalized.includes('element-plus/es/components/')) return undefined
  if (normalized.includes('element-plus/')) return 'vendor-ep-core'
  if (normalized.includes('vue-echarts')) return 'vendor-vue-echarts'
  if (normalized.includes('zrender')) return 'vendor-zrender'
  if (normalized.includes('echarts')) return 'vendor-echarts'
  if (normalized.includes('vue') || normalized.includes('vue-router') || normalized.includes('pinia')) return 'vendor-vue'
  if (normalized.includes('axios')) return 'vendor-axios'
  return 'vendor'
}

export default defineConfig({
  plugins: [
    vue(),
    Components({
      dts: false,
      resolvers: elementPlusComponentResolver()
    })
  ],
  base: './',
  // Modern desktop targets match Vue 3 + Element Plus support.
  build: {
    target: 'es2020',
    cssTarget: 'chrome107',
    rollupOptions: {
      output: {
        manualChunks(id) {
          return vendorChunk(id)
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
