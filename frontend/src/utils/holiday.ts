export interface HolidayInfo {
  name: string
  shortName: string
}

type HolidayInput = Date | string | { year: number; month: number; day: number } | number

const SOLAR_HOLIDAY_MAP: Record<string, HolidayInfo> = {
  '01-01': { name: '元旦', shortName: '元旦' },
  '05-01': { name: '劳动节', shortName: '劳动' },
  '10-01': { name: '国庆节', shortName: '国庆' },
}

const LUNAR_HOLIDAY_MAP: Record<string, HolidayInfo> = {
  '01-01': { name: '春节', shortName: '春节' },
  '05-05': { name: '端午节', shortName: '端午' },
  '08-15': { name: '中秋节', shortName: '中秋' },
}

const QINGMING_INFO: HolidayInfo = { name: '清明节', shortName: '清明' }
const CHUXI_INFO: HolidayInfo = { name: '除夕', shortName: '除夕' }

const chineseCalendarFormatter = createChineseCalendarFormatter()

function createChineseCalendarFormatter(): Intl.DateTimeFormat | null {
  try {
    return new Intl.DateTimeFormat('en-u-ca-chinese', {
      month: 'numeric',
      day: 'numeric',
    })
  } catch {
    return null
  }
}

function normalizeHolidayInput(input: HolidayInput, month?: number, day?: number): { year: number; month: number; day: number } | null {
  if (typeof input === 'number') {
    if (typeof month !== 'number' || typeof day !== 'number') return null
    return { year: input, month, day }
  }

  if (input instanceof Date) {
    return {
      year: input.getFullYear(),
      month: input.getMonth() + 1,
      day: input.getDate(),
    }
  }

  if (typeof input === 'string') {
    const match = input.match(/^(\d{4})-(\d{2})-(\d{2})$/)
    if (!match) return null
    return {
      year: Number(match[1]),
      month: Number(match[2]),
      day: Number(match[3]),
    }
  }

  return input
}

function getLunarMonthDay(date: Date): { month: number; day: number } | null {
  if (!chineseCalendarFormatter) return null

  try {
    const parts = chineseCalendarFormatter.formatToParts(date)
    const monthValue = parts.find((part) => part.type === 'month')?.value ?? ''
    const dayValue = parts.find((part) => part.type === 'day')?.value ?? ''
    const lunarMonth = Number.parseInt(monthValue.replace(/\D/g, ''), 10)
    const lunarDay = Number.parseInt(dayValue.replace(/\D/g, ''), 10)

    if (Number.isNaN(lunarMonth) || Number.isNaN(lunarDay)) return null

    return { month: lunarMonth, day: lunarDay }
  } catch {
    return null
  }
}

function getQingmingDay(year: number): number {
  const centuryYear = year % 100
  return Math.floor(centuryYear * 0.2422 + 4.81) - Math.floor(centuryYear / 4)
}

function isChineseNewYearsEve(date: Date): boolean {
  const nextDate = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1)
  const lunar = getLunarMonthDay(nextDate)
  return !!lunar && lunar.month === 1 && lunar.day === 1
}

export function getHolidayInfo(input: HolidayInput, month?: number, day?: number): HolidayInfo | null {
  const normalized = normalizeHolidayInput(input, month, day)
  if (!normalized) return null

  const { year, month: solarMonth, day: solarDay } = normalized
  const solarKey = `${String(solarMonth).padStart(2, '0')}-${String(solarDay).padStart(2, '0')}`
  if (SOLAR_HOLIDAY_MAP[solarKey]) return SOLAR_HOLIDAY_MAP[solarKey]

  if (solarMonth === 4 && solarDay === getQingmingDay(year)) return QINGMING_INFO

  const date = new Date(year, solarMonth - 1, solarDay)
  if (isChineseNewYearsEve(date)) return CHUXI_INFO

  const lunar = getLunarMonthDay(date)
  if (!lunar) return null

  const lunarKey = `${String(lunar.month).padStart(2, '0')}-${String(lunar.day).padStart(2, '0')}`
  return LUNAR_HOLIDAY_MAP[lunarKey] ?? null
}

export function getHolidayName(input: HolidayInput, month?: number, day?: number): string {
  return getHolidayInfo(input, month, day)?.shortName ?? ''
}
