import { useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
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
  if (s < 0.2) return 'bg-zinc-600'
  if (s < 0.5) return 'bg-amber-600'
  return 'bg-amber-500'
}

/** Overall only: Buy, Watch (weighted buy fraction ≥ 20% but below buy threshold), or Ignore (< 20%). */
function overallVerdictChip(row: SymbolRecommendation): { label: string; className: string } {
  const { overall } = row
  if (overall.buy) {
    return { label: 'Buy', className: 'bg-emerald-500/25 text-emerald-200' }
  }
  if (overall.weightedScore < 0.2) {
    return { label: 'Ignore', className: 'bg-zinc-600/80 text-zinc-200' }
  }
  return { label: 'Watch', className: 'bg-amber-500/25 text-amber-100' }
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
  const verdict = overallVerdictChip(row)
  const includedN = row.strategies.filter((s) => s.includedInAggregation).length

  return (
    <button
      type="button"
      data-list-symbol={row.symbol}
      onClick={onSelect}
      className={`w-full rounded-xl border px-4 py-4 text-left text-lg transition outline-none ${
        active
          ? 'border-emerald-400/60 bg-zinc-800 ring-2 ring-emerald-400/40'
          : 'border-zinc-700/80 bg-zinc-900/60 hover:border-zinc-600 hover:bg-zinc-800/70 focus-visible:ring-2 focus-visible:ring-zinc-500/80'
      }`}
    >
      <div className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1">
        <span className="shrink-0 font-mono text-3xl font-bold leading-none tracking-tight text-white">{row.symbol}</span>
        <span className="shrink-0 font-mono text-2xl font-semibold tabular-nums leading-none text-zinc-100">
          {pct(overall.weightedScore)}
        </span>
        <span
          className={`ml-auto shrink-0 rounded-lg px-3 py-1.5 text-lg font-bold uppercase leading-none ${verdict.className}`}
        >
          {verdict.label}
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
            {included ? (row.buy ? 'Buy' : 'No') : 'Skip'}
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
      <div className="flex h-[1152px] shrink-0 items-center justify-center rounded-xl border border-zinc-800 bg-zinc-900/30 text-xl font-medium text-zinc-200 sm:h-[1232px] lg:h-[1312px]">
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

function TradeLevelsPanel({ row }: { row: SymbolRecommendation }) {
  const methods = row.tradeLevels ?? []
  if (!row.overall.buy || methods.length === 0) return null
  return (
    <div className="shrink-0 rounded-2xl border-2 border-emerald-800/50 bg-emerald-950/20 p-5 sm:p-6">
      <h3 className="text-xl font-bold uppercase tracking-wide text-emerald-200">Suggested placement (overall buy)</h3>
      <div className="mt-4 space-y-4">
        {methods.map((t) => (
          <div
            key={t.methodId}
            className="rounded-xl border border-emerald-900/50 bg-zinc-950/50 ring-1 ring-black/20"
          >
            <div className="flex flex-wrap items-baseline justify-between gap-2 border-b border-zinc-800/80 px-4 py-3 sm:px-5 sm:py-3.5">
              <span className="text-lg font-bold text-white sm:text-xl">{t.methodLabel}</span>
              <span className="font-mono text-base text-zinc-500 sm:text-lg">({t.methodId})</span>
            </div>
            <dl className="grid gap-3 p-4 sm:grid-cols-2 sm:gap-4 sm:p-5 lg:grid-cols-4">
              <div className="rounded-xl border border-zinc-700/80 bg-zinc-900/50 px-4 py-3">
                <dt className="text-sm font-semibold uppercase tracking-wide text-zinc-500">Entry (ref)</dt>
                <dd className="mt-1 font-mono text-2xl font-bold tabular-nums text-white">{inr.format(t.entryPrice)}</dd>
              </div>
              <div className="rounded-xl border border-rose-800/40 bg-rose-950/20 px-4 py-3">
                <dt className="text-sm font-semibold uppercase tracking-wide text-rose-300/90">Stop loss</dt>
                <dd className="mt-1 font-mono text-2xl font-bold tabular-nums text-rose-100">{inr.format(t.stopLoss)}</dd>
              </div>
              <div className="rounded-xl border border-emerald-800/40 bg-emerald-950/25 px-4 py-3">
                <dt className="text-sm font-semibold uppercase tracking-wide text-emerald-200/90">Target</dt>
                <dd className="mt-1 font-mono text-2xl font-bold tabular-nums text-emerald-100">{inr.format(t.takeProfit)}</dd>
              </div>
              <div className="rounded-xl border border-zinc-700/80 bg-zinc-900/50 px-4 py-3">
                <dt className="text-sm font-semibold uppercase tracking-wide text-zinc-500">Risk / reward (share)</dt>
                <dd className="mt-1 font-mono text-xl font-semibold tabular-nums text-zinc-200">
                  {inr.format(t.riskPerShare)} → {inr.format(t.rewardPerShare)}
                </dd>
              </div>
            </dl>
            <details className="group border-t border-zinc-800/80">
              <summary className="cursor-pointer list-none px-4 py-3 text-base font-semibold text-emerald-200/90 marker:content-none sm:px-5 [&::-webkit-details-marker]:hidden">
                <span className="underline-offset-2 group-open:underline">Method detail and formulas</span>
                <span className="ml-2 inline-block text-zinc-500 transition-transform duration-200 group-open:rotate-180">
                  ▼
                </span>
              </summary>
              <div className="space-y-4 px-4 pb-4 sm:px-5 sm:pb-5">
                {t.methodDescription ? (
                  <p className="text-base leading-relaxed text-zinc-300 sm:text-lg">{t.methodDescription}</p>
                ) : null}
                <ul className="space-y-2 border-t border-zinc-800/80 pt-4 text-base leading-relaxed text-zinc-300 sm:text-lg">
                  {t.detailLines.map((line, j) => (
                    <li key={j}>{line}</li>
                  ))}
                </ul>
              </div>
            </details>
          </div>
        ))}
      </div>
    </div>
  )
}

function DetailPane({ row }: { row: SymbolRecommendation }) {
  const { overall } = row
  const verdict = overallVerdictChip(row)
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
              overall.buy
                ? 'bg-emerald-500/25 text-emerald-100 ring-2 ring-emerald-400/50'
                : verdict.label === 'Ignore'
                  ? 'bg-zinc-600/80 text-zinc-100 ring-2 ring-zinc-500/40'
                  : 'bg-amber-500/20 text-amber-50 ring-2 ring-amber-400/40'
            }`}
          >
            {verdict.label}
          </span>
          <div className="text-right">
            <div className="text-6xl font-bold tabular-nums leading-none text-white">{pct(overall.weightedScore)}</div>
            <div className="mt-1 text-lg font-medium text-zinc-300">Weighted score</div>
          </div>
        </div>
      </div>

      <TradeLevelsPanel row={row} />

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

function scrollListToSymbol(
  navEl: HTMLElement | null,
  sym: string,
  options?: { focusButton?: boolean },
) {
  if (!navEl) return
  const el = Array.from(navEl.querySelectorAll<HTMLElement>('[data-list-symbol]')).find(
    (e) => e.getAttribute('data-list-symbol') === sym,
  )
  if (!el) return
  el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  if (options?.focusButton && el instanceof HTMLButtonElement) {
    queueMicrotask(() => el.focus({ preventScroll: true }))
  }
}

export default function App() {
  const [selectedSymbol, setSelectedSymbol] = useState<string | null>(null)
  const [symbolListFilter, setSymbolListFilter] = useState('')
  const symbolListNavRef = useRef<HTMLElement | null>(null)
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

  const filteredRows = useMemo(() => {
    const q = symbolListFilter.trim().toLowerCase()
    if (!q) return sortedRows
    return sortedRows.filter((r) => r.symbol.toLowerCase().includes(q))
  }, [sortedRows, symbolListFilter])

  useEffect(() => {
    const q = symbolListFilter.trim()
    if (!q) return
    if (filteredRows.length === 0) {
      if (selectedSymbol != null) setSelectedSymbol(null)
      return
    }
    if (!selectedSymbol || !filteredRows.some((r) => r.symbol === selectedSymbol)) {
      setSelectedSymbol(filteredRows[0].symbol)
    }
  }, [symbolListFilter, filteredRows, selectedSymbol])

  const selected = useMemo(() => {
    if (!sortedRows.length) return null
    const q = symbolListFilter.trim()
    const pool = q ? filteredRows : sortedRows
    if (!pool.length) return null
    if (selectedSymbol && pool.some((r) => r.symbol === selectedSymbol)) {
      return pool.find((r) => r.symbol === selectedSymbol)!
    }
    return pool[0]
  }, [sortedRows, filteredRows, selectedSymbol, symbolListFilter])

  useEffect(() => {
    if (!rec.isSuccess || sortedRows.length === 0 || rightPaneView !== 'symbol') return

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return
      const target = e.target as HTMLElement | null
      if (target?.closest('input, textarea, select, [contenteditable="true"]')) return
      if (filteredRows.length === 0) return

      const sym = selected?.symbol
      if (!sym) return
      const idx = filteredRows.findIndex((r) => r.symbol === sym)
      if (idx < 0) return

      if (e.key === 'ArrowDown') {
        if (idx >= filteredRows.length - 1) return
        const next = filteredRows[idx + 1].symbol
        setSelectedSymbol(next)
        queueMicrotask(() =>
          scrollListToSymbol(symbolListNavRef.current, next, { focusButton: true }),
        )
        e.preventDefault()
        return
      }
      if (idx <= 0) return
      const prev = filteredRows[idx - 1].symbol
      setSelectedSymbol(prev)
      queueMicrotask(() =>
        scrollListToSymbol(symbolListNavRef.current, prev, { focusButton: true }),
      )
      e.preventDefault()
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [rec.isSuccess, sortedRows.length, rightPaneView, filteredRows, selected?.symbol])

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
            <div className="shrink-0 border-b border-zinc-800/90 bg-zinc-950 px-3 py-3 lg:px-4">
              <div className="text-xs font-bold uppercase tracking-wider text-zinc-500">Search symbols</div>
              <div className="mt-2 flex gap-2">
                <label className="sr-only" htmlFor="symbol-list-filter">
                  Filter symbol list
                </label>
                <input
                  id="symbol-list-filter"
                  type="search"
                  autoComplete="off"
                  spellCheck={false}
                  value={symbolListFilter}
                  onChange={(e) => setSymbolListFilter(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key !== 'Enter') return
                    const sym =
                      (symbolListFilter.trim()
                        ? filteredRows[0]?.symbol
                        : selected?.symbol ?? sortedRows[0]?.symbol) ?? null
                    if (sym) scrollListToSymbol(symbolListNavRef.current, sym)
                  }}
                  placeholder="e.g. HDFC, TATA…"
                  className="min-w-0 flex-1 rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2.5 font-mono text-lg text-white placeholder:text-zinc-500 focus:border-emerald-600/60 focus:outline-none focus:ring-2 focus:ring-emerald-500/30"
                />
                <button
                  type="button"
                  onClick={() => {
                    const sym =
                      (symbolListFilter.trim()
                        ? filteredRows[0]?.symbol
                        : selected?.symbol ?? sortedRows[0]?.symbol) ?? null
                    if (sym) scrollListToSymbol(symbolListNavRef.current, sym)
                  }}
                  disabled={
                    symbolListFilter.trim() ? filteredRows.length === 0 : sortedRows.length === 0
                  }
                  className="shrink-0 rounded-lg border border-zinc-600 bg-zinc-800 px-4 py-2.5 text-lg font-bold text-white transition hover:bg-zinc-700 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Search
                </button>
              </div>
            </div>
            <nav
              ref={symbolListNavRef}
              className="min-h-0 flex-1 space-y-3 overflow-y-auto overflow-x-hidden overscroll-y-contain px-3 py-4 lg:px-4 lg:py-5"
            >
              {filteredRows.map((row) => (
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
              ) : symbolListFilter.trim() && filteredRows.length === 0 ? (
                <div className="flex flex-1 flex-col items-center justify-center gap-3 p-8 text-center text-2xl font-medium text-zinc-300">
                  <p>No symbols match &ldquo;{symbolListFilter.trim()}&rdquo;.</p>
                  <button
                    type="button"
                    onClick={() => setSymbolListFilter('')}
                    className="rounded-xl border border-zinc-600 bg-zinc-800 px-5 py-2.5 text-lg font-semibold text-white hover:bg-zinc-700"
                  >
                    Clear filter
                  </button>
                </div>
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
