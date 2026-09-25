/**
 * Helpers of the map's search box: the filter a text becomes on a GeoServer layer,
 * and the place lookup (geocoder).
 */

/**
 * The CQL filter that keeps a WMS layer's features having `query` in any of `fields`
 * (case-insensitive, anywhere in the text), or null when there is nothing to filter
 * by: no text, or a layer without searchable fields (it stays as it is).
 * Only the quote needs escaping: the text ends up inside a CQL string literal.
 */
export function searchCqlFilter(fields, query) {
  const text = (query == null ? "" : String(query)).trim().toLowerCase();
  if (!text || !Array.isArray(fields) || fields.length === 0) return null;

  const literal = "'%" + text.replace(/'/g, "''") + "%'";
  return fields.map((f) => `strToLowerCase(${f}) LIKE ${literal}`).join(" OR ");
}

/** `[[south, west], [north, east]]` (Leaflet bounds) of a hit's `[xmin, ymin, xmax, ymax]`, or null. */
export function boundsFromBbox(bbox) {
  if (!Array.isArray(bbox) || bbox.length !== 4) return null;
  const [xmin, ymin, xmax, ymax] = bbox.map(Number);
  if (![xmin, ymin, xmax, ymax].every(Number.isFinite)) return null;
  return [
    [ymin, xmin],
    [ymax, xmax],
  ];
}

/**
 * Looks a place up in a Nominatim-style service (`/search?format=jsonv2`). Returns
 * `[{ label, bounds }]` (Leaflet bounds), at most `limit`. Rejects when the request
 * fails, so the caller can leave the places out.
 */
export async function geocode(query, { url, language = "en", limit = 5, signal } = {}) {
  const params = new URLSearchParams({
    format: "jsonv2",
    limit: String(limit),
    "accept-language": language,
    q: query,
  });
  const response = await fetch(`${url}?${params}`, { signal });
  if (!response.ok) throw new Error(`Geocoder answered ${response.status}`);

  const places = await response.json();
  return (Array.isArray(places) ? places : [])
    .map((place) => {
      // boundingbox is [south, north, west, east], as text
      const box = (place.boundingbox || []).map(Number);
      const bounds =
        box.length === 4 && box.every(Number.isFinite)
          ? [
              [box[0], box[2]],
              [box[1], box[3]],
            ]
          : null;
      const lat = Number(place.lat);
      const lon = Number(place.lon);
      return {
        label: place.display_name,
        bounds:
          bounds ||
          (Number.isFinite(lat) && Number.isFinite(lon)
            ? [
                [lat, lon],
                [lat, lon],
              ]
            : null),
      };
    })
    .filter((place) => place.label && place.bounds);
}
