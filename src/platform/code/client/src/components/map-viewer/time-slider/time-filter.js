/**
 * The time filter of the map's time slider: what a date window becomes on a WMS layer.
 * Pure, so it is easy to reason about (and to test without a map).
 */

const DAY_MS = 24 * 60 * 60 * 1000;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}/;

/**
 * Milliseconds (UTC midnight) of a day, or null when it isn't one. The server sends a
 * date as `[year, month, day]` (a LocalDate), GeoServer as "YYYY-MM-DD..." text.
 */
export function parseDay(value) {
  if (Array.isArray(value)) {
    const [year, month, day] = value;
    if (![year, month, day].every(Number.isInteger)) return null;
    const ms = Date.UTC(year, month - 1, day);
    return Number.isFinite(ms) ? ms : null;
  }
  if (typeof value !== "string" || !ISO_DATE.test(value)) return null;
  const ms = Date.parse(value.slice(0, 10) + "T00:00:00Z");
  return Number.isFinite(ms) ? ms : null;
}

/** "YYYY-MM-DD" of milliseconds since the epoch (UTC). */
export function formatDay(ms) {
  return new Date(ms).toISOString().slice(0, 10);
}

/**
 * The CQL filter that keeps the features in the window `[fromMs, toMs]`, by the layer's
 * time columns (`temporal`: `{ start: {column}, end?: {column} }` as in layers.json).
 * A feature with one time field is in the window when its time is; one with a start and
 * an end when its period overlaps the window (an open end counts as still going).
 */
export function timeCql(temporal, fromMs, toMs) {
  if (!temporal || !temporal.start) return null;
  const from = formatDay(fromMs);
  const to = formatDay(toMs);
  const start = temporal.start.column;
  if (!temporal.end) {
    return `${start} >= '${from}' AND ${start} <= '${to}'`;
  }
  const end = temporal.end.column;
  return `${start} <= '${to}' AND (${end} IS NULL OR ${end} >= '${from}')`;
}

/** A layer's own filter (a search) and the time filter as one filter, or null when neither. */
export function combineCql(base, time) {
  if (base && time) return `(${base}) AND (${time})`;
  return base || time || null;
}

/**
 * The window one "play" step further: the same width, moved on by a fifth of it (at
 * least a day). Stops at the end of the range: the returned `done` says so.
 */
export function nextWindow(fromMs, toMs, maxMs) {
  const width = toMs - fromMs;
  const step = Math.max(DAY_MS, Math.round(width / 5));
  const to = Math.min(toMs + step, maxMs);
  return { from: to - width, to, done: to >= maxMs };
}

export const ONE_DAY_MS = DAY_MS;
