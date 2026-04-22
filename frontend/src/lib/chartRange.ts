import type { OhlcBar } from '../api/types'

export type ChartRangeId = '5d' | '1m' | '1y' | '3y'

/** Initial range when opening a symbol chart. */
export const DEFAULT_CHART_RANGE: ChartRangeId = '1y'

export const CHART_RANGE_OPTIONS: { id: ChartRangeId; label: string }[] = [
  { id: '5d', label: '5D' },
  { id: '1m', label: '1M' },
  { id: '1y', label: '1Y' },
  { id: '3y', label: '3Y' },
]

function parseYmd(s: string): Date {
  const [y, m, d] = s.split('-').map(Number)
  if (!y || !m || !d) return new Date(NaN)
  return new Date(Date.UTC(y, m - 1, d, 12, 0, 0))
}

function formatYmd(d: Date): string {
  const y = d.getUTCFullYear()
  const m = String(d.getUTCMonth() + 1).padStart(2, '0')
  const day = String(d.getUTCDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const LAST_N_SESSIONS_FOR_5D = 5

/**
 * Filters OHLC for the chart. Last bar defines the window end (aligned with loaded series).
 * `5d` uses the last five **daily** bars (sessions), not five calendar days — a short calendar window often
 * spans weekends/holidays and produced only ~4 candles before.
 */
export function filterBarsByChartRange(bars: OhlcBar[], range: ChartRangeId): OhlcBar[] {
  if (bars.length === 0) return []
  const sorted = [...bars].sort((a, b) => a.date.localeCompare(b.date))
  if (range === '5d') {
    return sorted.slice(-Math.min(LAST_N_SESSIONS_FOR_5D, sorted.length))
  }

  const endStr = sorted[sorted.length - 1].date
  const end = parseYmd(endStr)
  if (Number.isNaN(end.getTime())) return sorted

  const start = new Date(end.getTime())
  if (range === '1m') {
    start.setUTCMonth(start.getUTCMonth() - 1)
  } else if (range === '1y') {
    start.setUTCFullYear(start.getUTCFullYear() - 1)
  } else {
    start.setUTCFullYear(start.getUTCFullYear() - 3)
  }

  const startStr = formatYmd(start)
  return sorted.filter((b) => b.date >= startStr && b.date <= endStr)
}
