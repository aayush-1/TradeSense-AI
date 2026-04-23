import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  createChart,
} from 'lightweight-charts'
import type { Time } from 'lightweight-charts'
import { useEffect, useMemo, useRef, useState } from 'react'
import type { OhlcBar } from '../api/types'
import {
  CHART_RANGE_OPTIONS,
  DEFAULT_CHART_RANGE,
  filterBarsByChartRange,
  type ChartRangeId,
} from '../lib/chartRange'

type Props = {
  symbol: string
  bars: OhlcBar[]
  /** Taller chart for main detail pane */
  variant?: 'default' | 'detail'
}

/** Time / price scale labels — large for readability on dense charts */
const CHART_FONT_SIZE = 36
const CHART_FONT_FAMILY = "ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"

function readChartHeight(el: HTMLElement, variant: 'default' | 'detail') {
  const h = el.clientHeight
  if (h > 0) return h
  return variant === 'detail' ? 1312 : 380
}

export function PriceChart({ symbol, bars, variant = 'default' }: Props) {
  const ref = useRef<HTMLDivElement>(null)
  const v = variant === 'detail' ? 'detail' : 'default'
  const [range, setRange] = useState<ChartRangeId>(DEFAULT_CHART_RANGE)

  useEffect(() => {
    setRange(DEFAULT_CHART_RANGE)
  }, [symbol])

  const displayBars = useMemo(() => {
    if (v !== 'detail') return bars
    return filterBarsByChartRange(bars, range)
  }, [bars, range, v])

  useEffect(() => {
    if (!ref.current || displayBars.length === 0) return
    const el = ref.current
    const height = readChartHeight(el, v)
    const chart = createChart(el, {
      layout: {
        background: { type: ColorType.Solid, color: '#09090b' },
        textColor: '#f4f4f5',
        fontSize: CHART_FONT_SIZE,
        fontFamily: CHART_FONT_FAMILY,
        attributionLogo: false,
      },
      grid: {
        vertLines: { color: '#3f3f46' },
        horzLines: { color: '#3f3f46' },
      },
      crosshair: { mode: CrosshairMode.Magnet },
      width: el.clientWidth,
      height,
      rightPriceScale: {
        borderColor: '#52525b',
        scaleMargins: { top: 0.06, bottom: 0.14 },
      },
      timeScale: {
        borderColor: '#52525b',
        fixLeftEdge: true,
        fixRightEdge: true,
      },
    })
    const series = chart.addSeries(CandlestickSeries, {
      upColor: '#10b981',
      downColor: '#f43f5e',
      borderVisible: false,
      wickUpColor: '#10b981',
      wickDownColor: '#f43f5e',
    })
    const data = displayBars.map((b) => ({
      time: b.date as Time,
      open: b.open,
      high: b.high,
      low: b.low,
      close: b.close,
    }))
    series.setData(data)
    chart.timeScale().fitContent()

    const ro = new ResizeObserver(() => {
      chart.applyOptions({
        width: el.clientWidth,
        height: readChartHeight(el, v),
        layout: {
          fontSize: CHART_FONT_SIZE,
          fontFamily: CHART_FONT_FAMILY,
          textColor: '#f4f4f5',
        },
      })
    })
    ro.observe(el)
    return () => {
      ro.disconnect()
      chart.remove()
    }
  }, [symbol, displayBars, v])

  const heightClass =
    variant === 'detail'
      ? 'h-[1152px] sm:h-[1232px] lg:h-[1312px]'
      : 'h-[min(420px,40vh)] min-h-[260px] sm:min-h-[280px]'

  const outerClass =
    variant === 'detail'
      ? 'flex shrink-0 flex-col overflow-hidden rounded-xl border border-zinc-800 bg-zinc-950 shadow-inner ring-1 ring-black/30'
      : 'flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-zinc-800 bg-zinc-950 shadow-inner ring-1 ring-black/30'

  const showRangeBar = variant === 'detail' && bars.length > 0

  if (bars.length === 0) {
    return (
      <div className={outerClass} aria-hidden>
        <div className="flex shrink-0 flex-wrap items-center justify-between gap-3 border-b border-zinc-800 px-4 py-3">
          <span className="text-lg font-semibold uppercase tracking-wide text-zinc-600">Daily</span>
          <span className="font-mono text-xl font-medium text-zinc-600">{symbol}</span>
        </div>
        <div className={`min-h-0 w-full bg-zinc-950 ${heightClass}`} />
      </div>
    )
  }

  if (displayBars.length === 0) {
    return (
      <div className={outerClass} aria-hidden>
        <div className="flex shrink-0 flex-col gap-3 border-b border-zinc-800 px-4 py-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
          <span className="text-lg font-semibold uppercase tracking-wide text-zinc-500">
            Daily · 0 sessions in range
          </span>
          {showRangeBar && (
            <div className="flex flex-wrap items-center gap-2">
              <div className="flex rounded-lg border border-zinc-700 bg-zinc-900 p-1">
                {CHART_RANGE_OPTIONS.map((opt) => (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => setRange(opt.id)}
                    className={`rounded-md px-3 py-2 text-lg font-semibold transition ${
                      range === opt.id ? 'bg-zinc-600 text-white' : 'text-zinc-400 hover:text-zinc-200'
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
              <span className="font-mono text-xl font-medium text-zinc-500">{symbol}</span>
            </div>
          )}
        </div>
        <div className={`min-h-0 w-full bg-zinc-950 ${heightClass}`} />
      </div>
    )
  }

  return (
    <div className={outerClass}>
      <div className="flex shrink-0 flex-col gap-3 border-b border-zinc-800 px-4 py-3 lg:flex-row lg:flex-wrap lg:items-center lg:justify-between">
        <span className="text-lg font-semibold uppercase tracking-wide text-zinc-200">
          Daily · {displayBars.length} session{displayBars.length === 1 ? '' : 's'}
        </span>
        <div className="flex min-w-0 flex-1 flex-wrap items-center justify-end gap-3">
          {showRangeBar && (
            <div className="flex rounded-lg border border-zinc-700 bg-zinc-900 p-1">
              {CHART_RANGE_OPTIONS.map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => setRange(opt.id)}
                  className={`rounded-md px-3 py-2 text-lg font-semibold transition ${
                    range === opt.id ? 'bg-zinc-600 text-white' : 'text-zinc-400 hover:text-zinc-200'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          )}
          <span className="shrink-0 font-mono text-xl font-medium text-zinc-100">{symbol}</span>
        </div>
      </div>
      <div ref={ref} className={`min-h-0 w-full ${heightClass}`} />
    </div>
  )
}
