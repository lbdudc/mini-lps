/*% if (feature.MV_T_F_BasicSearch) { %*/
<template>
  <v-card
    v-if="open"
    class="search-results"
    elevation="8"
    data-test="search-results"
  >
    <v-progress-linear
      v-if="loading"
      indeterminate
      height="2"
    ></v-progress-linear>

    <v-list dense v-for="group in groups" :key="group.key" class="py-0">
      <v-subheader>{{ group.label }}</v-subheader>
      <v-list-item
        v-for="(hit, index) in group.hits"
        :key="group.key + index"
        data-test="search-hit"
        @click="goTo(hit.bounds)"
      >
        <v-list-item-content>
          <v-list-item-title>{{ hit.label }}</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
    </v-list>

    <div v-if="!loading && groups.length === 0" class="pa-3 grey--text">
      {{ $t("mapViewer.search.empty") }}
    </div>
    /*% if (feature.MV_T_F_Geocoder) { %*/
    <div v-if="placesShown" class="px-3 py-1 caption grey--text">
      {{ $t("mapViewer.search.attribution") }}
    </div>
    /*% } %*/
  </v-card>
</template>

<script>
import RepositoryFactory from "@/repositories/RepositoryFactory";
/*% if (feature.MV_T_F_Geocoder) { %*/
import properties from "@/properties";
/*% } %*/
import { boundsFromBbox/*% if (feature.MV_T_F_Geocoder) { %*/, geocode/*% } %*/ } from "../common/search-common";

const MIN_CHARS = 2;
const LAYER_DELAY = 350;
/* the place service allows one request per second: wait for the typing to stop */
const PLACES_DELAY = 900;
const HITS_PER_LAYER = 8;
const HIGHLIGHT_MS = 4000;

/* Suggestions under the search box: the features matching the text, per layer, and
   the places matching it. Choosing one takes the map there. */
export default {
  name: "SearchResults",
  props: {
    query: { type: String, default: null },
    /* the map-viewer Map */
    map: { type: Object, default: null },
    /* the layers of the map shown: layers.json entries */
    overlays: { type: Array, default: () => [] },
  },
  data() {
    return {
      open: false,
      loading: false,
      layerGroups: [],
      placeGroup: null,
      placesShown: false,
      token: 0,
      layerTimer: null,
      placesTimer: null,
      highlight: null,
      highlightTimer: null,
    };
  },
  computed: {
    groups() {
      return [...this.layerGroups, ...(this.placeGroup ? [this.placeGroup] : [])];
    },
  },
  watch: {
    query(text) {
      this.schedule(text);
    },
  },
  mounted() {
    document.addEventListener("mousedown", this.onOutsideClick);
    document.addEventListener("keydown", this.onKey);
  },
  beforeDestroy() {
    document.removeEventListener("mousedown", this.onOutsideClick);
    document.removeEventListener("keydown", this.onKey);
    clearTimeout(this.layerTimer);
    clearTimeout(this.placesTimer);
    this.clearHighlight();
  },
  methods: {
    schedule(text) {
      clearTimeout(this.layerTimer);
      clearTimeout(this.placesTimer);
      const query = (text || "").trim();
      this.token += 1;
      if (query.length < MIN_CHARS) {
        this.open = false;
        this.loading = false;
        return;
      }
      const token = this.token;
      this.open = true;
      this.loading = true;
      this.layerGroups = [];
      this.placeGroup = null;
      this.placesShown = false;
      this.layerTimer = setTimeout(() => this.searchLayers(query, token), LAYER_DELAY);
      /*% if (feature.MV_T_F_Geocoder) { %*/
      this.placesTimer = setTimeout(() => this.searchPlaces(query, token), PLACES_DELAY);
      /*% } else { %*/
      this.placesTimer = null;
      /*% } %*/
    },
    async searchLayers(query, token) {
      const searches = this.overlays
        .filter((json) => json.entity)
        .map(async (json) => {
          const repository = RepositoryFactory.get(json.entity + "EntityRepository");
          if (!repository || !repository.searchHits) return null;
          try {
            const hits = await repository.searchHits(encodeURIComponent(query), HITS_PER_LAYER);
            return {
              key: "layer-" + json.name,
              label: this.$t("mapViewer.layer-label." + json.name.replace(".", "-")),
              hits: hits
                .map((hit) => ({ label: hit.displayString, bounds: boundsFromBbox(hit.bbox) }))
                .filter((hit) => hit.bounds),
            };
          } catch (e) {
            return null; /* the HTTP client already told the user */
          }
        });
      const groups = (await Promise.all(searches)).filter((g) => g && g.hits.length > 0);
      if (token !== this.token) return;
      this.layerGroups = groups;
      /*% if (!feature.MV_T_F_Geocoder) { %*/
      this.loading = false;
      /*% } %*/
    },
    /*% if (feature.MV_T_F_Geocoder) { %*/
    async searchPlaces(query, token) {
      try {
        const places = await geocode(query, {
          url: properties.GEOCODER_URL,
          language: this.$i18n.locale,
        });
        if (token !== this.token) return;
        this.placesShown = true;
        this.placeGroup = places.length
          ? { key: "places", label: this.$t("mapViewer.search.places"), hits: places }
          : null;
      } catch (e) {
        /* no places this time: the features found are still shown */
      }
      if (token === this.token) this.loading = false;
    },
    /*% } %*/
    goTo(bounds) {
      const leafletMap = this.map && this.map.getLeafletMap();
      if (!leafletMap || !bounds) return;
      const [[south, west], [north, east]] = bounds;
      const isPoint = south === north && west === east;
      if (isPoint) {
        leafletMap.setView([south, west], 16);
      } else {
        leafletMap.fitBounds(bounds, { maxZoom: 17, padding: [30, 30] });
      }
      this.showHighlight(leafletMap, bounds, isPoint);
      this.open = false;
    },
    showHighlight(leafletMap, bounds, isPoint) {
      this.clearHighlight();
      const style = { color: "#e53935", weight: 3, fill: false, interactive: false };
      this.highlight = isPoint
        ? L.circleMarker(bounds[0], { ...style, radius: 20 })
        : L.rectangle(bounds, style);
      this.highlight.addTo(leafletMap);
      this.highlightTimer = setTimeout(this.clearHighlight, HIGHLIGHT_MS);
    },
    clearHighlight() {
      clearTimeout(this.highlightTimer);
      if (this.highlight) {
        this.highlight.remove();
        this.highlight = null;
      }
    },
    onOutsideClick(event) {
      if (!this.open) return;
      const insideResults = this.$el && this.$el.contains && this.$el.contains(event.target);
      const insideSearchBox = event.target.closest && event.target.closest(".map-search-bar");
      if (!insideResults && !insideSearchBox) this.open = false;
    },
    onKey(event) {
      if (event.key === "Escape") this.open = false;
    },
  },
};
</script>

<style scoped>
.search-results {
  position: absolute;
  top: 100%;
  left: 8px;
  width: min(420px, calc(100% - 16px));
  max-height: 60vh;
  overflow-y: auto;
  z-index: 1100;
}
</style>
/*% } %*/
