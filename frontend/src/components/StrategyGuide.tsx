const STRATEGIES: {
  id: string
  title: string
  weightNote: string
  body: string[]
}[] = [
  {
    id: 'trend-ma-cross-v1',
    title: 'MA stack / golden trend',
    weightNote: 'Default weight 1.5 (configurable in backend YAML).',
    body: [
      'Long-horizon trend filter on daily closes. It computes the 50-day and 200-day simple moving averages (SMA).',
      'A buy signal requires both: the 50 SMA above the 200 SMA (bullish “stack”), and the latest close above the 50 SMA. That favours names in an established uptrend with price still participating on the faster average.',
      'Needs at least 200 daily bars; otherwise the strategy is skipped for scoring so it does not count as a sell.',
    ],
  },
  {
    id: 'breakout-volume-v1',
    title: 'Breakout + volume',
    weightNote: 'Default weight 1.2.',
    body: [
      'Looks for a close above the highest high of the prior 20 sessions (excluding today), combined with today’s volume above 1.5× the average volume of the prior 20 sessions (excluding today).',
      'It is a classic breakout confirmation: new relative highs with participation. If history is too short, the strategy is skipped.',
    ],
  },
  {
    id: 'rsi-volume-v1',
    title: 'RSI + volume stress',
    weightNote: 'Default weight 1.0.',
    body: [
      'Combines a 14-period RSI with volume, a short swing-low check, and a 200-day SMA trend filter. A buy requires RSI below 35 (stressed / oversold), volume above 1.2× the prior 20-day average volume, the close not below the minimum low of the prior 5 sessions (avoids “catching a knife” in a free fall), and the close above the 200-day simple moving average (long-term uptrend only).',
      'Useful for identifying stressed but potentially stabilising setups when liquidity shows up.',
    ],
  },
  {
    id: 'mean-revert-vwap-proxy-v1',
    title: 'Mean reversion (VWAP proxy)',
    weightNote: 'Default weight 0.9.',
    body: [
      'Uses a rolling volume-weighted typical price over 20 sessions as a VWAP-style anchor, plus 14-period ATR. A buy fires when price is stretched below that proxy by at least one ATR (mean-reversion opportunity), but only if a trend guard passes: either the 50 SMA is above the 200 SMA, or the close is above the 50 SMA.',
      'So dips toward the proxy are only considered when the broader picture is not structurally broken. Requires 200 bars for the trend guard and ATR context.',
    ],
  },
  {
    id: 'accumulation-pressure-v1',
    title: 'Accumulation / money flow',
    weightNote: 'Default weight 1.1.',
    body: [
      'Chaikin-style money flow over 20 sessions. A buy needs positive current money flow and an improvement versus the same measure computed on the series ending five sessions ago.',
      'It highlights names where buy-side pressure may be building or accelerating. Needs enough history for both windows.',
    ],
  },
]

export function StrategyGuide() {
  return (
    <div className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden overscroll-y-contain p-4 sm:p-6 lg:p-8">
      <h2 className="text-4xl font-bold tracking-tight text-white sm:text-5xl">Strategy guide</h2>

      <ul className="mt-6 space-y-10 sm:mt-8">
        {STRATEGIES.map((s) => (
          <li
            key={s.id}
            className="rounded-2xl border-2 border-zinc-700 bg-zinc-900/50 p-6 sm:p-8"
          >
            <div className="flex flex-wrap items-baseline justify-between gap-3">
              <h3 className="text-2xl font-bold text-white sm:text-3xl">{s.title}</h3>
              <span className="font-mono text-xl text-zinc-400">{s.id}</span>
            </div>
            <p className="mt-2 text-xl font-semibold text-emerald-200/95 sm:text-2xl">{s.weightNote}</p>
            <div className="mt-4 space-y-4 text-xl leading-relaxed text-zinc-100 sm:text-2xl sm:leading-relaxed">
              {s.body.map((p, i) => (
                <p key={i}>{p}</p>
              ))}
            </div>
          </li>
        ))}
      </ul>

      <p className="mt-10 text-xl text-zinc-400">
        Indicative daily data (e.g. Yahoo Finance). Not investment advice.
      </p>
    </div>
  )
}
