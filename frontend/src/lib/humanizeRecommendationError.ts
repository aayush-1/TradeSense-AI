/**
 * Formats backend `dataErrors` lines (`SYMBOL: exception message`) for the UI.
 * Hides raw HTTP codes / provider noise behind a short, user-facing sentence.
 */
export function humanizeDataErrorMessage(raw: string): string {
  const colon = raw.indexOf(':')
  if (colon <= 0 || colon >= raw.length - 1) {
    return raw
  }
  const symbol = raw.slice(0, colon).trim()
  const detail = raw.slice(colon + 1).trim()
  if (!symbol) return raw
  if (!detail) {
    return `${symbol}: Market data could not be refreshed for this symbol. Strategy signals for this stock may be incomplete.`
  }

  const looksTechnical =
    /\b(400|401|403|404|408|409|429|500|502|503)\b/.test(detail) ||
    /\bHTTP\b/i.test(detail) ||
    /Yahoo/i.test(detail) ||
    /IllegalStateException/i.test(detail) ||
    /RestClientException/i.test(detail) ||
    /Failed to parse/i.test(detail) ||
    /request failed/i.test(detail) ||
    /Connection/i.test(detail) ||
    /timed?\s*out/i.test(detail)

  if (looksTechnical) {
    return `${symbol}: Could not refresh market data for this symbol, so strategy evaluations may not have completed successfully.`
  }

  return raw
}
