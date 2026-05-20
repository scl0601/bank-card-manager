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
      redirect: () => {
        const authStore = useAuthStore()
        return authStore.role === 'MONITOR' ? '/monitor' : '/dashboard'
      },
      children: [
        {
          path: 'monitor',
          name: 'Monitor',
          component: () => import('@/views/monitor/MonitorView.vue'),
          meta: { title: '监控列表', icon: 'Monitor', roles: ['ADMIN', 'MONITOR'] }
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '首页看板', icon: 'House' }
        },
        {
          path: 'cards',
          name: 'Cards',
          component: () => import('@/views/card/CardList.vue'),
          meta: { title: '卡务管理', icon: 'CreditCard' }
        },
        {
          path: 'users',
          name: 'CardUsers',
          component: () => import('@/views/card/CardUserList.vue'),
          meta: { title: '用户信息', icon: 'UserFilled' }
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
          meta: { title: '账单信息', icon: 'Document' }
        },
        {
          path: 'profits',
          name: 'ProfitStats',
          component: () => import('@/views/profit/ProfitStatsView.vue'),
          meta: { title: '收益统计', icon: 'TrendCharts' }
        },
        {
          path: 'special',
          name: 'SpecialChannel',
          component: () => import('@/views/special/SpecialChannelView.vue'),
          meta: { title: '特殊通道', icon: 'Connection' }
        },
        {
          path: 'reminders',
          name: 'Reminders',
          component: () => import('@/views/reminder/ReminderList.vue'),
          meta: { title: '提醒中心', icon: 'Bell' }
        },
        {
          path: 'feedbacks',
          name: 'Feedbacks',
          component: () => import('@/views/feedback/FeedbackList.vue'),
          meta: { title: '用户反馈', icon: 'ChatDotRound' }
        },
        {
          path: 'logs',
          name: 'Logs',
          component: () => import('@/views/log/LogList.vue'),
          meta: { title: '系统日志', icon: 'Tickets' }
        },
        {
          path: 'announcements',
          name: 'Announcements',
          component: () => import('@/views/announcement/AnnouncementManage.vue'),
          meta: { title: '公告管理', icon: 'Bell', roles: ['MONITOR'] }
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
  const routeRoles = to.meta.roles as string[] | undefined
  if (routeRoles?.length && !routeRoles.includes(authStore.role)) {
    return authStore.role === 'MONITOR' ? '/monitor' : '/'
  }
  if ((to.path === '/login' || to.path === '/m/login') && authStore.token) {
    if (authStore.role === 'MONITOR') {
      return '/monitor'
    }
    return isMobileRoute ? '/m/calendar' : '/'
  }
  if (authStore.role === 'MONITOR' && isMobileRoute) {
    return '/monitor'
  }
  if (authStore.role === 'MONITOR' && !isMobileRoute) {
    const allowedPaths = new Set(['/monitor', '/logs', '/announcements'])
    if (!allowedPaths.has(to.path)) {
      return '/monitor'
    }
  }
})

export default router
