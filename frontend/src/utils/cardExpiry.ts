export type CardExpireStatus = 'normal' | 'soon' | 'expired' | 'unknown'

interface YearMonth {
  year: number
  month: number
}

function buildYearMonth(yearText: string, monthText: string): YearMonth | null {
  const rawYear = Number.parseInt(yearText, 10)
  const month = Number.parseInt(monthText, 10)
  if (!Number.isFinite(rawYear) || !Number.isFinite(month)) return null
  if (month < 1 || month > 12) return null

  const year = rawYear < 100 ? rawYear + 2000 : rawYear
  return { year, month }
}

export function parseCardExpireYearMonth(expireDate: string | null | undefined): YearMonth | null {
  const raw = String(expireDate || '').trim()
  if (!raw) return null

  const compact = raw.replace(/\D+/g, '')
  if (compact.length === 4) {
    return buildYearMonth(compact.slice(2), compact.slice(0, 2))
  }

  const parts = raw.split(/\D+/).filter(Boolean)
  if (parts.length < 2) return null

  if (parts[0].length === 4) {
    return buildYearMonth(parts[0], parts[1])
  }
  return buildYearMonth(parts[1], parts[0])
}

export function getCardExpireStatus(
  expireDate: string | null | undefined,
  today = new Date()
): CardExpireStatus {
  const expire = parseCardExpireYearMonth(expireDate)
  if (!expire) return 'unknown'

  const currentIndex = today.getFullYear() * 12 + today.getMonth() + 1
  const expireIndex = expire.year * 12 + expire.month
  const monthsLeft = expireIndex - currentIndex

  if (monthsLeft < 0) return 'expired'
  if (monthsLeft <= 1) return 'soon'
  return 'normal'
}
