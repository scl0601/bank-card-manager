import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    // ==================== 桌面端路由 ====================
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
        },
        {
          path: 'calendar',
          name: 'Calendar',
          component: () => import('@/views/calendar/CalendarView.vue'),
          meta: { title: '日历计划', icon: 'Calendar' }
        }
      ]
    },


    // ==================== 移动端路由 ====================
    {
      path: '/m/login',
      name: 'MobileLogin',
      component: () => import('@/mobile/layout/MobileLayout.vue'),
      meta: { requiresAuth: false, isMobile: true, mobileTitle: '登录' },
      children: [
        {
          path: '',
          name: 'MobileLoginPage',
          component: () => import('@/mobile/views/MobileLoginView.vue')
        }
      ]
    },
    {
      path: '/m',
      component: () => import('@/mobile/layout/MobileLayout.vue'),
      meta: { requiresAuth: true, isMobile: true, mobileTitle: '日历计划' },
      redirect: '/m/calendar',
      children: [
        {
          path: 'calendar',
          name: 'MobileCalendar',
          component: () => import('@/mobile/views/calendar/MobileCalendarView.vue'),
          meta: { showBack: false, mobileTitle: '日历计划' }
        },
        {
          path: 'event/:id?',
          name: 'MobileEventDetail',
          component: () => import('@/mobile/views/event/MobileEventDetail.vue'),
          meta: { showBack: true, mobileTitle: '日程详情' }
        }
      ]
    },

    // ==================== 404 ====================
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
  const isMobileRoute = !!to.meta?.isMobile
  const loginPath = isMobileRoute ? '/m/login' : '/login'

  if (to.meta.requiresAuth !== false && !authStore.token) {
    return loginPath
  }
  if ((to.path === '/login' || to.path === '/m/login') && authStore.token) {
    return isMobileRoute ? '/m/calendar' : '/'
  }
})

export default router
