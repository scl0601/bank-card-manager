const BILL_DETAIL_PREFETCH_CACHE_KEY = 'bill:detail-prefetch:v1'
const BILL_DETAIL_PREFETCH_TTL = 30_000
const BILL_DETAIL_PREFETCH_MAX_ITEMS = 30

export interface PrefetchedBillDetail {
  id: number
  billId: number
  detailDate: string
  description: string
  amount: number | string
  detailType: number
  remark?: string
}

interface PrefetchCache {
  billId: number
  details: PrefetchedBillDetail[]
  cachedAt: number
}

interface PrefetchCacheStore {
  items: Record<string, PrefetchCache>
}

interface ActivePrefetch {
  billId: number
  startedAt: number
  promise: Promise<PrefetchedBillDetail[]>
}

declare global {
  interface Window {
    __billDetailPrefetch?: ActivePrefetch
    __billDetailPrefetchMap?: Record<string, ActivePrefetch>
  }
}

function isFresh(timestamp: number) {
  return Number.isFinite(timestamp) && Date.now() - timestamp <= BILL_DETAIL_PREFETCH_TTL
}

function normalizeCacheStore(raw: string | null): PrefetchCacheStore {
  if (!raw) return { items: {} }
  const parsed = JSON.parse(raw)
  if (parsed?.items && typeof parsed.items === 'object') {
    return { items: parsed.items }
  }
  if (Number(parsed?.billId) > 0 && Array.isArray(parsed?.details)) {
    return { items: { [String(parsed.billId)]: parsed } }
  }
  return { items: {} }
}

function readCacheStore() {
  if (typeof window === 'undefined') return null
  try {
    const raw = window.sessionStorage.getItem(BILL_DETAIL_PREFETCH_CACHE_KEY)
    return normalizeCacheStore(raw)
  } catch {
    return { items: {} }
  }
}

function writeCacheStore(store: PrefetchCacheStore) {
  if (typeof window === 'undefined') return
  try {
    window.sessionStorage.setItem(BILL_DETAIL_PREFETCH_CACHE_KEY, JSON.stringify(store))
  } catch {
    // Storage is only an acceleration path; ignore quota/private-mode failures.
  }
}

function readCache(billId: number) {
  if (typeof window === 'undefined') return null
  try {
    const store = readCacheStore()
    const cache = store?.items[String(billId)]
    if (!cache) return null
    if (Number(cache.billId) !== Number(billId) || !isFresh(Number(cache.cachedAt)) || !Array.isArray(cache.details)) {
      if (store) {
        delete store.items[String(billId)]
        writeCacheStore(store)
      }
      return null
    }
    return cache.details
  } catch {
    return null
  }
}

function writeCache(billId: number, details: PrefetchedBillDetail[]) {
  if (typeof window === 'undefined') return
  try {
    const store = readCacheStore() || { items: {} }
    store.items[String(billId)] = {
      billId,
      details,
      cachedAt: Date.now()
    }
    const entries = Object.values(store.items)
      .sort((a, b) => Number(b.cachedAt || 0) - Number(a.cachedAt || 0))
      .slice(0, BILL_DETAIL_PREFETCH_MAX_ITEMS)
    writeCacheStore({
      items: Object.fromEntries(entries.map(item => [String(item.billId), item]))
    })
  } catch {
    // Storage is only an acceleration path; ignore quota/private-mode failures.
  }
}

export function startBillDetailPrefetch(billId: number, loader: () => Promise<PrefetchedBillDetail[]>) {
  if (!billId || typeof window === 'undefined') return null
  const cached = readCache(billId)
  if (cached) return Promise.resolve(cached)

  const prefetchMap = window.__billDetailPrefetchMap || (window.__billDetailPrefetchMap = {})
  const existing = prefetchMap[String(billId)]
  if (existing && Number(existing.billId) === Number(billId) && isFresh(existing.startedAt)) {
    return existing.promise
  }

  const promise = loader().then(details => {
    writeCache(billId, details)
    return details
  }).catch(error => {
    const activeMap = window.__billDetailPrefetchMap
    if (activeMap && Number(activeMap[String(billId)]?.billId) === Number(billId)) {
      delete activeMap[String(billId)]
    }
    throw error
  })

  const activePrefetch = {
    billId,
    startedAt: Date.now(),
    promise
  }
  prefetchMap[String(billId)] = activePrefetch
  window.__billDetailPrefetch = activePrefetch

  void promise.catch(() => {})
  return promise
}

export async function consumeBillDetailPrefetch(billId: number) {
  if (!billId || typeof window === 'undefined') return null
  const active = window.__billDetailPrefetchMap?.[String(billId)] || window.__billDetailPrefetch
  if (active && Number(active.billId) === Number(billId) && isFresh(active.startedAt)) {
    try {
      return await active.promise
    } catch {
      return readCache(billId)
    }
  }
  return readCache(billId)
}

export function peekBillDetailPrefetch(billId: number) {
  if (!billId || typeof window === 'undefined') return null
  return readCache(billId)
}
