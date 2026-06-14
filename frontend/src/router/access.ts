export interface RouteAccessInput {
  path: string
  requiresAuth?: boolean
  roles?: string[]
  isMobileRoute?: boolean
  token?: string
  role?: string
}

export function resolveRouteRedirect(input: RouteAccessInput): string | undefined {
  const isMobileRoute = !!input.isMobileRoute
  const loginPath = isMobileRoute ? '/m/login' : '/login'
  const role = input.role || ''

  if (input.requiresAuth !== false && !input.token) {
    return loginPath
  }
  if (input.roles?.length && !input.roles.includes(role)) {
    return role === 'MONITOR' ? '/monitor' : '/'
  }
  if ((input.path === '/login' || input.path === '/m/login') && input.token) {
    if (role === 'MONITOR') {
      return '/monitor'
    }
    return isMobileRoute ? '/m/calendar' : '/'
  }
  if (role === 'MONITOR' && isMobileRoute) {
    return '/monitor'
  }
  if (role === 'MONITOR' && !isMobileRoute) {
    const allowedPaths = new Set(['/monitor', '/logs', '/announcements'])
    if (!allowedPaths.has(input.path)) {
      return '/monitor'
    }
  }
  return undefined
}
