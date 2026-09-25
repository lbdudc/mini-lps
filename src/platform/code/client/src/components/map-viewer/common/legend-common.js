/**
 * Legend helpers shared by the WMS legend dialog and the on-map legend control.
 *
 * A WMS layer's legend is a GeoServer GetLegendGraphic image; a GeoJSON layer has
 * no server to ask, so its legend is its style's colours. Legend images are turned
 * into blob URLs (POST, because a style sent as sld_body does not fit in a URL):
 * same-origin images, which the map export (html2canvas) can draw without tainting
 * the canvas.
 */

const LEGEND_OPTIONS = (fontSize) =>
  `forceLabels:on;fontAntiAliasing:true;fontSize:${fontSize};fontName:Arial;columnheight:350;`;

/**
 * The URL a layer's legend image comes from, or null when the layer has none (an
 * XYZ tile layer, say). Not the image itself: also the key to cache it by.
 */
export function legendSourceUrl(layer, size = 20) {
  if (!layer || typeof layer.getIconUrl !== "function") return null;
  try {
    return layer.getIconUrl(size, size) || null;
  } catch (e) {
    return null;
  }
}

/**
 * Fetches the legend image of `layer` as something an <img> can show (a blob URL, or
 * the data/plain URL the layer already gave), or null when there is none.
 */
export async function fetchLegendImage(
  layer,
  { locale = "en", size = 20, fontSize = 12 } = {}
) {
  const source = legendSourceUrl(layer, size);
  if (!source) return null;
  // A style icon (GeoJSON layers) or any plain image URL: nothing to request
  if (source.startsWith("data:") || !source.includes("?")) return source;

  const [url, query] = source.split("?");
  const params = new URLSearchParams(query);
  for (const key of [...params.keys()]) {
    if (["height", "width", "language", "legend_options"].includes(key.toLowerCase())) {
      params.delete(key);
    }
  }
  params.set("height", size);
  params.set("width", size);
  params.set("language", locale);
  params.set("legend_options", LEGEND_OPTIONS(fontSize));

  try {
    const response = await fetch(url, {
      method: "POST",
      body: params,
      headers: { "Content-type": "application/x-www-form-urlencoded" },
    });
    if (!response.ok) return null;
    const blob = await response.blob();
    return blob.type.startsWith("image/") ? URL.createObjectURL(blob) : null;
  } catch (e) {
    return null;
  }
}

/**
 * Whether `layer` is drawn at `zoom`: QGIS's scale-based visibility reaches the layer as
 * minZoom/maxZoom in its params (see map-layer-common's getZoomLimits). A layer without
 * limits is drawn at every zoom.
 */
export function inZoomRange(layer, zoom) {
  const params = (layer.options && layer.options.params) || {};
  const min = params.minZoom;
  const max = params.maxZoom;
  return (min == null || zoom >= min) && (max == null || zoom <= max);
}

/** `{ fill, stroke }` colours of a GeoJSON layer's style, or null. */
export function geoJSONSwatch(layer) {
  try {
    const style = layer.getStyle();
    if (style && (style.fillColor || style.strokeColor)) {
      return { fill: style.fillColor, stroke: style.strokeColor };
    }
  } catch (e) {
    // a layer without a style has no swatch
  }
  return null;
}

/**
 * The legend as a Leaflet control (bottom left): the visible overlays, top of the
 * list first, each with its legend image or colour swatch. It follows the map
 * (layers shown/hidden/restyled). Its container has the fixed class name
 * "gp-map-legend leaflet-control" so the map export can keep it in the picture.
 *
 * @param {Object} customMap the map-viewer Map (getVisibleOverlays, getLeafletMap)
 * @param {{ t: function(String): String, locale: String }} i18n
 */
export function buildLegendControl(customMap, { t, locale }) {
  const cache = new Map(); // source url -> Promise<image url|null>

  const imageFor = (layer) => {
    const key = legendSourceUrl(layer) || `swatch:${layer.getId()}`;
    if (!cache.has(key)) cache.set(key, fetchLegendImage(layer, { locale }));
    return cache.get(key);
  };

  const Legend = L.Control.extend({
    options: { position: "bottomleft" },

    onAdd(leafletMap) {
      this._leafletMap = leafletMap;
      const container = L.DomUtil.create("div", "gp-map-legend");
      this._collapsed = window.innerWidth < 600;

      this._toggle = L.DomUtil.create("button", "gp-map-legend__toggle", container);
      this._toggle.type = "button";
      this._body = L.DomUtil.create("div", "gp-map-legend__body", container);

      L.DomEvent.disableClickPropagation(container);
      L.DomEvent.disableScrollPropagation(container);
      L.DomEvent.on(this._toggle, "click", () => {
        this._collapsed = !this._collapsed;
        this._applyCollapsed();
      });

      this._refresh = () => {
        clearTimeout(this._timer);
        this._timer = setTimeout(() => this._render(), 60);
      };
      leafletMap.on("layerchange zoomend", this._refresh);

      this._applyCollapsed();
      this._render();
      return container;
    },

    onRemove(leafletMap) {
      clearTimeout(this._timer);
      leafletMap.off("layerchange zoomend", this._refresh);
    },

    _applyCollapsed() {
      this._body.style.display = this._collapsed ? "none" : "";
      this._toggle.textContent = this._collapsed
        ? `${t("mapViewer.legend.title")} ▸`
        : `${t("mapViewer.legend.title")} ▾`;
      this._toggle.title = t(
        this._collapsed ? "mapViewer.legend.show" : "mapViewer.legend.hide"
      );
    },

    async _render() {
      let layers = [];
      try {
        layers = customMap.getVisibleOverlays();
      } catch (e) {
        layers = [];
      }
      const items = await Promise.all(
        layers.map(async (layer) => ({
          layer,
          image: await imageFor(layer),
        }))
      );
      // The map may have changed while the images loaded: draw the current state
      const stillVisible = new Set(
        customMap.getVisibleOverlays().map((l) => l.getId())
      );

      this._body.textContent = "";
      const zoom = this._leafletMap.getZoom();
      const shown = items.filter(
        ({ layer }) => stillVisible.has(layer.getId()) && inZoomRange(layer, zoom)
      );
      if (shown.length === 0) {
        const empty = L.DomUtil.create("div", "gp-map-legend__empty", this._body);
        empty.textContent = t("mapViewer.legend.empty");
        return;
      }
      for (const { layer, image } of shown) {
        const row = L.DomUtil.create("div", "gp-map-legend__item", this._body);
        row.setAttribute("data-layer", layer.getId());
        const title = L.DomUtil.create("div", "gp-map-legend__title", row);
        title.textContent = layer.getLabel();
        if (image) {
          const img = L.DomUtil.create("img", "gp-map-legend__image", row);
          img.src = image;
          img.alt = layer.getLabel();
        } else {
          const swatch = geoJSONSwatch(layer);
          if (swatch) {
            const box = L.DomUtil.create("span", "gp-map-legend__swatch", row);
            box.style.background = swatch.fill || "transparent";
            box.style.borderColor = swatch.stroke || swatch.fill || "#666";
          }
        }
      }
    },
  });

  return new Legend();
}
