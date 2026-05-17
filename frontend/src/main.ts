import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import '@/styles/index.scss'
import { registerElementIcons } from '@/plugins/element-icons'

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
registerElementIcons(app)

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

app.mount('#app')
