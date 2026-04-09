import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import '@/styles/index.scss'

// 通用组件
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import AmountDisplay from '@/components/AmountDisplay/index.vue'
import CardNumberDisplay from '@/components/CardNumberDisplay/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import CrudDialog from '@/components/CrudDialog/index.vue'
import ExportButton from '@/components/ExportButton/index.vue'

const app = createApp(App)

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册全局通用组件
app.component('SearchBar', SearchBar)
app.component('PageTable', PageTable)
app.component('AmountDisplay', AmountDisplay)
app.component('CardNumberDisplay', CardNumberDisplay)
app.component('StatusTag', StatusTag)
app.component('CrudDialog', CrudDialog)
app.component('ExportButton', ExportButton)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
