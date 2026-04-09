import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '首页看板', icon: 'House' }
        },
        {
          path: 'owners',
          name: 'Owners',
          component: () => import('@/views/owner/OwnerList.vue'),
          meta: { title: '持卡人管理', icon: 'User' }
        },
        {
          path: 'cards',
          name: 'Cards',
          component: () => import('@/views/card/CardList.vue'),
          meta: { title: '银行卡管理', icon: 'CreditCard' }
        },
        {
          path: 'transactions',
          name: 'Transactions',
          component: () => import('@/views/transaction/TransactionList.vue'),
          meta: { title: '流水管理', icon: 'List' }
        },
        {
          path: 'books',
          name: 'Books',
          component: () => import('@/views/book/BookList.vue'),
          meta: { title: '个人记账', icon: 'Wallet' }
        },
        {
          path: 'bills',
          name: 'Bills',
          component: () => import('@/views/bill/BillList.vue'),
          meta: { title: '账单管理', icon: 'Document' }
        },
        {
          path: 'reminders',
          name: 'Reminders',
          component: () => import('@/views/reminder/ReminderList.vue'),
          meta: { title: '提醒中心', icon: 'Bell' }
        },
        {
          path: 'logs',
          name: 'Logs',
          component: () => import('@/views/log/LogList.vue'),
          meta: { title: '系统日志', icon: 'Tickets' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/NotFound.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

// 路由守卫
router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth !== false && !authStore.token) {
    return '/login'
  }
  if (to.path === '/login' && authStore.token) {
    return '/'
  }
})

export default router
