/* Tests of the pure helpers of the generated client (no Vue, no browser): they are plain ES
   modules without imports, so they are loaded from their source text. Run with `npm test`. */
import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
  "src",
  "platform",
  "code",
  "client",
  "src",
  "components",
  "map-viewer"
);

const load = (relative) => {
  const source = fs.readFileSync(path.join(root, relative), "utf-8");
  return import("data:text/javascript;base64," + Buffer.from(source).toString("base64"));
};

const day = (text) => Date.parse(text + "T00:00:00Z");

test("time filter: parse and format days", async () => {
  const { parseDay, formatDay } = await load("time-slider/time-filter.js");
  assert.equal(parseDay("2015-07-30"), day("2015-07-30"));
  assert.equal(parseDay("2015-07-30T12:00:00"), day("2015-07-30"));
  /* the API's own shape for a date */
  assert.equal(parseDay([2015, 7, 30]), day("2015-07-30"));
  assert.equal(parseDay([1985, 3, 14]), day("1985-03-14"));
  for (const bad of [null, undefined, "", "30/07/2015", "soon", 5, [2015, 7], ["a", 1, 2], []]) {
    assert.equal(parseDay(bad), null, String(bad));
  }
  assert.equal(formatDay(day("1985-03-14")), "1985-03-14");
});

test("time filter: one time field is filtered by the window", async () => {
  const { timeCql } = await load("time-slider/time-filter.js");
  assert.equal(
    timeCql({ start: { column: "founded" } }, day("2001-01-01"), day("2010-12-31")),
    "founded >= '2001-01-01' AND founded <= '2010-12-31'"
  );
});

test("time filter: a period overlaps the window, an open end counts as still going", async () => {
  const { timeCql } = await load("time-slider/time-filter.js");
  assert.equal(
    timeCql({ start: { column: "opened" }, end: { column: "closed" } }, day("2001-01-01"), day("2010-12-31")),
    "opened <= '2010-12-31' AND (closed IS NULL OR closed >= '2001-01-01')"
  );
  assert.equal(timeCql(null, 0, 1), null);
  assert.equal(timeCql({}, 0, 1), null);
});

test("time filter: it is combined with a search filter", async () => {
  const { combineCql } = await load("time-slider/time-filter.js");
  assert.equal(combineCql("a LIKE 'x'", "d >= '2001-01-01'"), "(a LIKE 'x') AND (d >= '2001-01-01')");
  assert.equal(combineCql("a LIKE 'x'", null), "a LIKE 'x'");
  assert.equal(combineCql(null, "d > 1"), "d > 1");
  assert.equal(combineCql(null, null), null);
});

test("time filter: play moves the window on and stops at the end", async () => {
  const { nextWindow, ONE_DAY_MS } = await load("time-slider/time-filter.js");
  const max = day("2020-01-01");
  const first = nextWindow(day("2000-01-01"), day("2005-01-01"), max);
  assert.equal(first.to - first.from, day("2005-01-01") - day("2000-01-01")); /* same width */
  assert.ok(first.to > day("2005-01-01") && !first.done);
  const last = nextWindow(day("2018-01-01"), day("2019-12-30"), max);
  assert.equal(last.to, max);
  assert.equal(last.done, true);
  /* a one-day window still advances by a day */
  const tiny = nextWindow(day("2001-01-01"), day("2001-01-01"), max);
  assert.equal(tiny.to - day("2001-01-01"), ONE_DAY_MS);
});

test("search: the text becomes a case-insensitive filter over the text fields", async () => {
  const { searchCqlFilter } = await load("common/search-common.js");
  assert.equal(
    searchCqlFilter(["name", "descriptio"], "  Arnoia "),
    "strToLowerCase(name) LIKE '%arnoia%' OR strToLowerCase(descriptio) LIKE '%arnoia%'"
  );
  assert.equal(searchCqlFilter(["name"], "O'Neil"), "strToLowerCase(name) LIKE '%o''neil%'");
  for (const [fields, text] of [[[], "x"], [null, "x"], [["a"], ""], [["a"], null], [["a"], "   "]]) {
    assert.equal(searchCqlFilter(fields, text), null);
  }
});

test("search: hit boxes and place boxes become Leaflet bounds", async () => {
  const { boundsFromBbox } = await load("common/search-common.js");
  assert.deepEqual(boundsFromBbox([-8.9, 42.8, -8.6, 43.0]), [
    [42.8, -8.9],
    [43.0, -8.6],
  ]);
  for (const bad of [null, [1, 2, 3], ["a", 1, 2, 3], undefined]) assert.equal(boundsFromBbox(bad), null);
});

test("legend: a layer is listed only inside its zoom range", async () => {
  const { inZoomRange } = await load("common/legend-common.js");
  const layer = (params) => ({ options: { params } });
  assert.equal(inZoomRange(layer({ minZoom: 10, maxZoom: 14 }), 12), true);
  assert.equal(inZoomRange(layer({ minZoom: 10, maxZoom: 14 }), 9), false);
  assert.equal(inZoomRange(layer({ minZoom: 10, maxZoom: 14 }), 15), false);
  assert.equal(inZoomRange(layer({ minZoom: 10 }), 30), true);
  assert.equal(inZoomRange(layer({}), 3), true);
  assert.equal(inZoomRange({ options: {} }, 3), true);
});

test("legend: a layer without a legend source has no request", async () => {
  const { legendSourceUrl } = await load("common/legend-common.js");
  assert.equal(legendSourceUrl({}), null);
  assert.equal(legendSourceUrl({ getIconUrl: () => null }), null);
  assert.equal(legendSourceUrl({ getIconUrl: (h, w) => `http://x/wms?height=${h}&width=${w}` }, 20), "http://x/wms?height=20&width=20");
  assert.equal(legendSourceUrl({ getIconUrl: () => { throw new Error("no style"); } }), null);
});
