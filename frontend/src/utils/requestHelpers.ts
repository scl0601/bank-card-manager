export const CLOUD_PROD_API_BASE_URL = 'https://bank-admin-backend-239413-10-1411764939.sh.run.tcloudbase.com/api'

export function normalizeApiBaseUrl(envBaseUrl?: string, hostname?: string) {
  const trimmedEnvBaseUrl = envBaseUrl?.trim()
  if (trimmedEnvBaseUrl) {
    return trimmedEnvBaseUrl.replace(/\/+$/, '')
  }

  if (hostname?.endsWith('tcloudbaseapp.com')) {
    return CLOUD_PROD_API_BASE_URL
  }

  return '/api'
}

export function shouldAttachAuthToken(token: string | undefined, requestUrl: string) {
  return !!token && !requestUrl.includes('/auth/login')
}
