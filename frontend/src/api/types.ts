export type OverallRecommendation = {
  buy: boolean
  weightedScore: number
  aggregationMethod: string
  rationale: string[]
}

export type StrategyRecommendation = {
  strategyId: string
  displayName: string
  buy: boolean
  weight: number
  rationale: string[]
  includedInAggregation: boolean
}

export type SymbolRecommendation = {
  symbol: string
  referencePrice: number | null
  overall: OverallRecommendation
  strategies: StrategyRecommendation[]
  notes: string[]
}

export type RecommendationResponse = {
  universe: string[]
  recommendations: SymbolRecommendation[]
  dataErrors: string[]
}

export type OhlcBar = {
  date: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export type OhlcSeriesResponse = {
  symbol: string
  bars: OhlcBar[]
}
