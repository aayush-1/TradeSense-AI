import type { SymbolRecommendation } from '../api/types'

/** Sum of weights where the strategy voted buy and counts toward aggregation. */
export function weightedBuyVoteTotal(row: SymbolRecommendation): number {
  return row.strategies
    .filter((s) => s.includedInAggregation && s.buy)
    .reduce((acc, s) => acc + s.weight, 0)
}

export function includedBuyCount(row: SymbolRecommendation): number {
  return row.strategies.filter((s) => s.includedInAggregation && s.buy).length
}

/**
 * Highest weighted score first; ties favor stronger bullish evidence (weighted buy votes, then count of buy
 * signals, then overall buy flag, then symbol).
 */
export function compareRecommendations(a: SymbolRecommendation, b: SymbolRecommendation): number {
  const ds = b.overall.weightedScore - a.overall.weightedScore
  if (ds !== 0) return ds

  const dw = weightedBuyVoteTotal(b) - weightedBuyVoteTotal(a)
  if (dw !== 0) return dw

  const dc = includedBuyCount(b) - includedBuyCount(a)
  if (dc !== 0) return dc

  if (a.overall.buy !== b.overall.buy) {
    return (b.overall.buy ? 1 : 0) - (a.overall.buy ? 1 : 0)
  }

  return a.symbol.localeCompare(b.symbol)
}

export function sortRecommendations(rows: SymbolRecommendation[]): SymbolRecommendation[] {
  return [...rows].sort(compareRecommendations)
}
