import { useQuery } from '@tanstack/react-query'
import { useMemo, useState, type ReactNode } from 'react'
import { fetchOhlcSeries, fetchRecommendations } from './api/client'
import type { SymbolRecommendation } from './api/types'
import { PriceChart } from './components/PriceChart'
import { StrategyGuide } from './components/StrategyGuide'
import { humanizeDataErrorMessage } from './lib/humanizeRecommendationError'
import { sortRecommendations } from './lib/recommendationSort'

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
})

function pct(x: number) {
  return `${(x * 100).toFixed(1)}%`
}

function PageGutter({ children }: { children: ReactNode }) {
  return <div className="w-full px-4 sm:px-6 md:px-8">{children}</div>
}

function MiniScoreBar({ score, className = '' }: { score: number; className?: string }) {
  const p = Math.min(100, Math.max(0, score * 100))
  return (
    <div className={`h-3 min-w-0 flex-1 overflow-hidden rounded-full bg-zinc-700 ${className}`}>
      <div
        className="h-full rounded-full bg-gradient-to-r from-amber-600 to-emerald-500"
        style={{ width: `${p}%` }}
      />
    </div>
  )
}

function verdictStripClass(row: SymbolRecommendation) {
  const s = row.overall.weightedScore
  if (row.overall.buy) return 'bg-emerald-500'
  if (s >= 0.45) return 'bg-amber-500'
  if (s >= 0.25) return 'bg-amber-700'
  return 'bg-rose-600'
}

function StockListItem({
  row,
  active,
  onSelect,
}: {
  row: SymbolRecommendation
  active: boolean
  onSelect: () => void
}) {
  const { overall } = row
  const includedN = row.strategies.filter((s) => s.includedInAggregation).length

  return (
    <button
      type="button"
      onClick={onSelect}
      className={`w-full rounded-xl border px-4 py-4 text-left text-lg transition ${
        active
          ? 'border-emerald-400/60 bg-zinc-800 ring-2 ring-emerald-400/40'
          : 'border-zinc-700/80 bg-zinc-900/60 hover:border-zinc-600 hover:bg-zinc-800/70'
      }`}
    >
      <div className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1">
        <span className="shrink-0 font-mono text-3xl font-bold leading-none tracking-tight text-white">{row.symbol}</span>
        <span className="shrink-0 font-mono text-2xl font-semibold tabular-nums leading-none text-zinc-100">
          {pct(overall.weightedScore)}
        </span>
        <span
          className={`ml-auto shrink-0 rounded-lg px-3 py-1.5 text-lg font-bold uppercase leading-none ${
            overall.buy ? 'bg-emerald-500/25 text-emerald-200' : 'bg-zinc-700 text-zinc-100'
          }`}
        >
          {overall.buy ? 'Buy' : 'Hold'}
        </span>
      </div>
      <div className="mt-3 flex min-w-0 items-center gap-3">
        <MiniScoreBar score={overall.weightedScore} />
        <span
          className={`h-3 w-10 shrink-0 rounded-md ${verdictStripClass(row)}`}
          title="Signal strength"
          aria-hidden
        />
      </div>
      <div className="mt-2 text-xl font-semibold leading-snug text-zinc-200 sm:text-2xl">
        {includedN}/{row.strategies.length} strategies in aggregate
      </div>
    </button>
  )
}

function StrategyCard({ row }: { row: SymbolRecommendation['strategies'][number] }) {
  const included = row.includedInAggregation
  return (
    <div
      className={`rounded-xl border-2 p-6 ${
        included ? 'border-zinc-600 bg-zinc-900/70' : 'border-zinc-700 bg-zinc-950/80'
      }`}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="text-2xl font-semibold text-white">{row.displayName}</div>
          <div className="mt-1.5 font-mono text-lg text-zinc-300">{row.strategyId}</div>
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-3">
          <span
            className={`rounded-lg px-3 py-2 text-lg font-bold uppercase ${
              included
                ? row.buy
                  ? 'bg-emerald-500/25 text-emerald-200'
                  : 'bg-rose-500/20 text-rose-200'
                : 'bg-zinc-700 text-zinc-200'
            }`}
          >
            {included ? (row.buy ? 'Buy' : 'Hold') : 'Skip'}
          </span>
          <span className="text-xl font-semibold tabular-nums text-zinc-200 sm:text-2xl">×{row.weight.toFixed(2)}</span>
        </div>
      </div>
      {!included && (
        <p className="mt-4 text-xl font-medium text-amber-100 sm:text-2xl">
          Excluded from aggregate (insufficient data or error).
        </p>
      )}
      <ul className="mt-5 space-y-4 border-t-2 border-zinc-700 pt-5 text-xl leading-relaxed text-zinc-100 sm:text-2xl sm:leading-relaxed">
        {row.rationale.map((line, i) => (
          <li key={i} className="flex gap-3.5">
            <span className="mt-3 h-2.5 w-2.5 shrink-0 rounded-full bg-zinc-500" aria-hidden />
            <span>{line}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function DetailOhlc({ symbol }: { symbol: string }) {
  const q = useQuery({
    queryKey: ['ohlc', symbol],
    queryFn: () => fetchOhlcSeries(symbol),
  })
  if (q.isLoading) {
    return (
      <div className="flex h-[576px] shrink-0 items-center justify-center rounded-xl border border-zinc-800 bg-zinc-900/30 text-xl font-medium text-zinc-200 sm:h-[616px] lg:h-[656px]">
        Loading chart…
      </div>
    )
  }
  if (q.isError) {
    return (
      <div className="rounded-xl border border-rose-800/60 bg-rose-950/30 p-5 text-xl text-rose-50">
        {(q.error as Error).message}
      </div>
    )
  }
  if (q.isSuccess) {
    return (
      <div className="w-full shrink-0">
        <PriceChart variant="detail" symbol={symbol} bars={q.data.bars} />
      </div>
    )
  }
  return null
}

function DetailPane({ row }: { row: SymbolRecommendation }) {
  const { overall } = row
  return (
    <div className="flex min-h-0 flex-1 flex-col gap-8 overflow-y-auto overflow-x-hidden overscroll-y-contain p-4 sm:p-6 lg:p-8">
      <div className="flex flex-wrap items-end justify-between gap-6 border-b-2 border-zinc-700 pb-6">
        <div>
          <h2 className="font-mono text-5xl font-bold tracking-tight text-white lg:text-6xl">{row.symbol}</h2>
          <p className="mt-2 text-4xl font-bold tabular-nums text-zinc-100">
            {row.referencePrice != null ? inr.format(row.referencePrice) : '—'}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-4">
          <span
            className={`rounded-xl px-6 py-3 text-2xl font-bold uppercase ${
              overall.buy ? 'bg-emerald-500/25 text-emerald-100 ring-2 ring-emerald-400/50' : 'bg-zinc-700 text-zinc-100'
            }`}
          >
            {overall.buy ? 'Buy' : 'Hold'}
          </span>
          <div className="text-right">
            <div className="text-6xl font-bold tabular-nums leading-none text-white">{pct(overall.weightedScore)}</div>
            <div className="mt-1 text-lg font-medium text-zinc-300">Weighted score</div>
          </div>
        </div>
      </div>

      <div className="shrink-0">
        <h3 className="mb-3 text-xl font-bold uppercase tracking-wide text-zinc-200">Price chart</h3>
        <DetailOhlc symbol={row.symbol} />
      </div>

      <div>
        <h3 className="mb-4 text-xl font-bold uppercase tracking-wide text-zinc-200">Strategies</h3>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {row.strategies.map((s) => (
            <StrategyCard key={s.strategyId} row={s} />
          ))}
        </div>
      </div>
    </div>
  )
}

export default function App() {
  const [selectedSymbol, setSelectedSymbol] = useState<string | null>(null)
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null)
  /** When set, global data warnings are hidden until the server returns a different `dataErrors` list. */
  const [dismissedDataErrorsKey, setDismissedDataErrorsKey] = useState<string | null>(null)
  /**
   * After "Refresh", only errors not present in this snapshot are shown (same persistent failures are not repeated).
   * `null` = initial load / never refreshed — show all errors from the latest response.
   */
  const [manualRefreshErrorSnapshot, setManualRefreshErrorSnapshot] = useState<string[] | null>(null)
  const [rightPaneView, setRightPaneView] = useState<'symbol' | 'guide'>('symbol')

  const rec = useQuery({
    queryKey: ['recommendations'],
    queryFn: async () => {
      const data = await fetchRecommendations()
      setUpdatedAt(new Date())
      return data
    },
  })

  const sortedRows = useMemo(() => {
    const list = rec.data?.recommendations ?? []
    return sortRecommendations(list)
  }, [rec.data])

  const selected = useMemo(() => {
    if (!sortedRows.length) return null
    if (selectedSymbol && sortedRows.some((r) => r.symbol === selectedSymbol)) {
      return sortedRows.find((r) => r.symbol === selectedSymbol)!
    }
    return sortedRows[0]
  }, [sortedRows, selectedSymbol])

  const universeCount = rec.data?.universe.length

  const dataErrorsFingerprint = rec.data?.dataErrors?.join('\u0001') ?? ''

  const visibleDataErrors = useMemo(() => {
    const raw = rec.data?.dataErrors ?? []
    if (manualRefreshErrorSnapshot === null) return raw
    return raw.filter((e) => !manualRefreshErrorSnapshot.includes(e))
  }, [dataErrorsFingerprint, manualRefreshErrorSnapshot, rec.data])

  const visibleDataErrorsKey = visibleDataErrors.join('\u0001')
  const showDataWarnings =
    visibleDataErrors.length > 0 && dismissedDataErrorsKey !== visibleDataErrorsKey

  return (
    <div className="flex h-dvh max-h-dvh flex-col overflow-hidden bg-gradient-to-b from-zinc-950 via-zinc-950 to-[#0a0a0c] text-zinc-100">
      <header className="shrink-0 border-b border-zinc-800/90 bg-zinc-950/95 shadow-md backdrop-blur-md">
        <PageGutter>
          <div className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-baseline gap-3">
                <h1 className="text-4xl font-bold tracking-tight text-white sm:text-5xl">TradeSense</h1>
                {universeCount != null && (
                  <span className="rounded-full bg-zinc-700 px-4 py-2 text-lg font-semibold text-zinc-100 ring-2 ring-zinc-600">
                    {universeCount} symbols
                  </span>
                )}
              </div>
              <p className="mt-2 whitespace-nowrap text-lg font-medium leading-snug text-zinc-400 sm:text-xl overflow-x-auto">
                Multi-strategy, data-backed signals on Indian equities — weighted, explainable, chart-ready.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-3">
              {updatedAt && (
                <time className="text-lg font-medium text-zinc-300" dateTime={updatedAt.toISOString()}>
                  {updatedAt.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}
                </time>
              )}
              <button
                type="button"
                onClick={() => {
                  setManualRefreshErrorSnapshot(rec.data?.dataErrors ? [...rec.data.dataErrors] : [])
                  setDismissedDataErrorsKey(null)
                  void rec.refetch()
                }}
                disabled={rec.isFetching}
                className="rounded-xl bg-white px-6 py-3 text-xl font-bold text-zinc-900 shadow transition hover:bg-zinc-100 disabled:opacity-50"
              >
                {rec.isFetching ? 'Refreshing…' : 'Refresh'}
              </button>
            </div>
          </div>
        </PageGutter>
      </header>

      {rec.isError && (
        <PageGutter>
          <div className="mt-4 rounded-xl border border-rose-800/70 bg-rose-950/35 p-5 text-rose-50">
            <strong className="text-2xl">Could not load recommendations.</strong>
            <p className="mt-3 text-xl">{(rec.error as Error).message}</p>
            <p className="mt-3 text-xl text-rose-100">
              <code className="rounded-lg bg-black/40 px-3 py-2 font-mono text-lg">cd backend && mvn spring-boot:run</code>
            </p>
          </div>
        </PageGutter>
      )}

      {showDataWarnings && (
        <PageGutter>
          <div className="relative mt-4 rounded-xl border-2 border-amber-600/50 bg-amber-950/35 p-5 pr-14 text-xl text-amber-50">
            <button
              type="button"
              onClick={() => setDismissedDataErrorsKey(visibleDataErrorsKey)}
              className="absolute right-3 top-3 flex h-11 w-11 items-center justify-center rounded-lg border border-amber-500/40 bg-amber-950/80 text-2xl font-light leading-none text-amber-100 hover:bg-amber-900/90"
              aria-label="Dismiss data warnings"
            >
              ×
            </button>
            <strong className="text-2xl">Data warnings</strong> ({visibleDataErrors.length})
            <ul className="mt-3 list-inside list-disc space-y-2 text-lg text-amber-100">
              {visibleDataErrors.map((e, i) => (
                <li key={i}>{humanizeDataErrorMessage(e)}</li>
              ))}
            </ul>
          </div>
        </PageGutter>
      )}

      {rec.isLoading && (
        <div className="flex min-h-0 flex-1 flex-col items-center justify-center gap-3 py-20">
          <div className="h-14 w-14 animate-spin rounded-full border-[3px] border-zinc-600 border-t-emerald-400" />
          <p className="text-xl font-medium text-zinc-300">Loading…</p>
        </div>
      )}

      {rec.isSuccess && sortedRows.length > 0 && (
        <div className="flex min-h-0 flex-1 flex-col overflow-hidden lg:flex-row">
          <aside className="flex h-[min(40dvh,28rem)] min-h-0 shrink-0 flex-col border-zinc-700 bg-zinc-950 lg:h-auto lg:min-w-[22rem] lg:max-w-[40%] lg:w-[34%] lg:shrink-0 lg:border-b-0 lg:border-r">
            <nav className="min-h-0 flex-1 space-y-3 overflow-y-auto overflow-x-hidden overscroll-y-contain px-3 py-4 lg:px-4 lg:py-5">
              {sortedRows.map((row) => (
                <StockListItem
                  key={row.symbol}
                  row={row}
                  active={selected?.symbol === row.symbol && rightPaneView === 'symbol'}
                  onSelect={() => {
                    setSelectedSymbol(row.symbol)
                    setRightPaneView('symbol')
                  }}
                />
              ))}
            </nav>
          </aside>

          <section className="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden bg-[#070708]">
            <div className="flex shrink-0 gap-2 border-b-2 border-zinc-800 bg-zinc-950/90 px-3 py-2 sm:gap-3 sm:px-4 sm:py-2.5">
              <button
                type="button"
                onClick={() => setRightPaneView('symbol')}
                className={`rounded-xl px-4 py-2.5 text-xl font-bold transition sm:px-6 sm:py-3 sm:text-2xl ${
                  rightPaneView === 'symbol'
                    ? 'bg-zinc-700 text-white ring-2 ring-zinc-500'
                    : 'text-zinc-400 hover:bg-zinc-800/80 hover:text-zinc-200'
                }`}
              >
                Symbol & signals
              </button>
              <button
                type="button"
                onClick={() => setRightPaneView('guide')}
                className={`rounded-xl px-4 py-2.5 text-xl font-bold transition sm:px-6 sm:py-3 sm:text-2xl ${
                  rightPaneView === 'guide'
                    ? 'bg-zinc-700 text-white ring-2 ring-zinc-500'
                    : 'text-zinc-400 hover:bg-zinc-800/80 hover:text-zinc-200'
                }`}
              >
                Strategy guide
              </button>
            </div>
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
              {rightPaneView === 'guide' ? (
                <StrategyGuide />
              ) : selected ? (
                <DetailPane row={selected} />
              ) : (
                <div className="flex flex-1 items-center justify-center p-8 text-2xl font-medium text-zinc-300">
                  Select a symbol
                </div>
              )}
            </div>
          </section>
        </div>
      )}

      {rec.isSuccess && sortedRows.length === 0 && (
        <div className="flex flex-1 items-center justify-center p-8 text-2xl font-medium text-zinc-300">No recommendations returned.</div>
      )}

      <footer className="shrink-0 border-t border-zinc-900 py-3">
        <PageGutter>
          <p className="text-center text-lg text-zinc-400">
            Indicative data (Yahoo Finance). Not investment advice.
          </p>
        </PageGutter>
      </footer>
    </div>
  )
}
