import type { MarketSegment, OhlcSeriesResponse, RecommendationResponse } from './types'

const base = () => (import.meta.env.VITE_API_BASE as string | undefined)?.replace(/\/$/, '') ?? '/api/v1'

export async function fetchRecommendations(segment: MarketSegment): Promise<RecommendationResponse> {
  const r = await fetch(`${base()}/recommendations?segment=${encodeURIComponent(segment)}`)
  if (!r.ok) {
    throw new Error(`Recommendations failed: ${r.status} ${r.statusText}`)
  }
  return r.json() as Promise<RecommendationResponse>
}

export async function fetchOhlcSeries(
  symbol: string,
  segment: MarketSegment,
): Promise<OhlcSeriesResponse> {
  const enc = encodeURIComponent(symbol)
  const r = await fetch(
    `${base()}/symbols/${enc}/ohlc?segment=${encodeURIComponent(segment)}`,
  )
  if (!r.ok) {
    throw new Error(`OHLC failed for ${symbol}: ${r.status} ${r.statusText}`)
  }
  return r.json() as Promise<OhlcSeriesResponse>
}
