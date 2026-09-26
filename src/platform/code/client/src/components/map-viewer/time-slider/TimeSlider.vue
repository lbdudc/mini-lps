/*% if (feature.MV_T_TimeSlider) { %*/
<template>
  <v-card
    v-if="range && open"
    class="time-slider"
    elevation="6"
    data-test="time-slider"
  >
    <div class="d-flex align-center">
      <v-btn
        icon
        small
        :title="playing ? $t('mapViewer.timeSlider.pause') : $t('mapViewer.timeSlider.play')"
        data-test="time-slider-play"
        @click="togglePlay"
      >
        <v-icon>{{ playing ? "mdi-pause" : "mdi-play" }}</v-icon>
      </v-btn>
      <div class="time-slider__title px-2">{{ $t("mapViewer.timeSlider.title") }}</div>
      <div class="time-slider__dates" data-test="time-slider-dates">
        {{ label(span[0]) }} – {{ label(span[1]) }}
      </div>
      <v-btn
        icon
        small
        :title="$t('mapViewer.timeSlider.all')"
        data-test="time-slider-reset"
        @click="reset"
      >
        <v-icon>mdi-backup-restore</v-icon>
      </v-btn>
      <v-btn
        icon
        small
        :title="$t('mapViewer.timeSlider.hide')"
        data-test="time-slider-close"
        @click="$emit('update:open', false)"
      >
        <v-icon>mdi-close</v-icon>
      </v-btn>
    </div>
    <v-range-slider
      v-model="span"
      :min="range[0]"
      :max="range[1]"
      :step="oneDay"
      hide-details
      dense
      class="mt-1"
      data-test="time-slider-range"
      @input="onSlide"
    ></v-range-slider>
  </v-card>
</template>

<script>
import RepositoryFactory from "@/repositories/RepositoryFactory";
import { parseDay, timeCql, combineCql, nextWindow, ONE_DAY_MS } from "./time-filter";

const PLAY_INTERVAL_MS = 900;
const APPLY_DELAY_MS = 250;
/* rows looked at to find the first (or last) feature that has a date at all */
const SAMPLE_SIZE = 200;

/* A window of dates over the map: it shows only the features in it, on every visible
   layer that has time fields in QGIS. It is there while such a layer is shown. */
export default {
  name: "TimeSlider",
  props: {
    /* the map-viewer Map */
    map: { type: Object, default: null },
    /* the layers of the map shown: layers.json entries */
    overlays: { type: Array, default: () => [] },
    /* whether the slider is shown (`.sync`): the map's right-hand button and the slider's own
       close button both change it. Hidden, it stops filtering: the layers show all their dates. */
    open: { type: Boolean, default: true },
  },
  data() {
    return {
      /* [min, max] day of the data of the visible time layers, or null when there are none */
      range: null,
      /* the window of dates shown: [from, to] in ms */
      span: [0, 0],
      playing: false,
      oneDay: ONE_DAY_MS,
      extents: {} /* layer name -> [min, max] found once */,
      visible: [] /* names of the visible time layers */,
      applyTimer: null,
      playTimer: null,
    };
  },
  computed: {
    timeLayers() {
      return this.overlays.filter((json) => json.temporal && json.entity);
    },
  },
  watch: {
    overlays() {
      this.refresh();
    },
    /* the map's button needs to know whether there is anything to show */
    range(value) {
      this.$emit("available", value !== null);
    },
    open(shown) {
      if (!shown) this.reset();
    },
    map() {
      this.bindMap();
    },
  },
  created() {
    this._loading = {};
  },
  mounted() {
    this.bindMap();
  },
  beforeDestroy() {
    this.$emit("available", false);
    clearTimeout(this.applyTimer);
    clearInterval(this.playTimer);
    this.unbindMap();
  },
  methods: {
    label(ms) {
      return new Date(ms).toLocaleDateString(this.$i18n.locale, { timeZone: "UTC" });
    },
    bindMap() {
      this.unbindMap();
      const leaflet = this.map && this.map.getLeafletMap();
      if (!leaflet) return;
      this._leaflet = leaflet;
      this._onLayerChange = () => this.refresh();
      leaflet.on("layerchange", this._onLayerChange);
      this.refresh();
    },
    unbindMap() {
      if (this._leaflet && this._onLayerChange) {
        this._leaflet.off("layerchange", this._onLayerChange);
      }
      this._leaflet = null;
    },
    /* which time layers are shown now, and the data range they span */
    async refresh() {
      if (!this.map) return;
      this.visible = this.timeLayers
        .filter((json) => {
          const layer = this.map.getLayer(json.name);
          return layer && layer.isSelected();
        })
        .map((json) => json.name);
      await Promise.all(
        this.timeLayers
          .filter(
            (json) =>
              this.visible.includes(json.name) &&
              !(json.name in this.extents) &&
              !this._loading[json.name]
          )
          .map((json) => this.loadExtent(json))
      );
      const extents = this.visible.map((name) => this.extents[name]).filter(Boolean);
      if (extents.length === 0) {
        this.range = null;
        this.stop();
        return;
      }
      const min = Math.min(...extents.map((e) => e[0]));
      const max = Math.max(...extents.map((e) => e[1]));
      const changed = !this.range || this.range[0] !== min || this.range[1] !== max;
      this.range = [min, max];
      if (changed) {
        this.span = [min, max];
      }
    },
    /* the first and last date of a layer, from its list endpoint sorted both ways */
    async loadExtent(json) {
      this._loading[json.name] = true;
      const repository = RepositoryFactory.get(json.entity + "EntityRepository");
      const first = json.temporal.start.property;
      const last = (json.temporal.end || json.temporal.start).property;
      const dateOf = async (property, direction) => {
        const page = await repository.getAll({
          params: { page: 0, size: SAMPLE_SIZE, sort: `${property},${direction}` },
        });
        const days = (page.content || [])
          .map((row) => parseDay(row[property]))
          .filter((day) => day !== null);
        return days.length ? days[0] : null;
      };
      try {
        const [min, max] = await Promise.all([dateOf(first, "asc"), dateOf(last, "desc")]);
        this.$set(this.extents, json.name, min !== null && max !== null && min <= max ? [min, max] : null);
      } catch (e) {
        this.$set(this.extents, json.name, null);
      } finally {
        this._loading[json.name] = false;
      }
    },
    onSlide() {
      clearTimeout(this.applyTimer);
      this.applyTimer = setTimeout(() => this.apply(), APPLY_DELAY_MS);
    },
    /* puts the window on every visible time layer, on top of the filter it already has */
    apply() {
      const full = this.range && this.span[0] === this.range[0] && this.span[1] === this.range[1];
      for (const json of this.timeLayers) {
        const layer = this.map.getLayer(json.name);
        if (!layer || !layer.options || !layer.options.params) continue;
        const params = layer.options.params;
        // What the layer was drawn with before the slider (a search filter): kept apart
        if (!("gpBaseCql" in layer)) layer.gpBaseCql = params.cql_filter || null;
        const time = full ? null : timeCql(json.temporal, this.span[0], this.span[1]);
        const filter = combineCql(layer.gpBaseCql, time);
        if (filter) params.cql_filter = filter;
        else delete params.cql_filter;
        Promise.resolve(layer.getLayer()).then((leafletLayer) => {
          if (!leafletLayer) return;
          /* Leaflet's setParams only adds and overwrites keys: a filter that is no longer
             wanted has to be removed from the layer's own parameters, or it is drawn again */
          if (!filter && leafletLayer.wmsParams) delete leafletLayer.wmsParams.cql_filter;
          if (leafletLayer.setParams) leafletLayer.setParams(params);
        });
      }
    },
    reset() {
      this.stop();
      if (this.range) this.span = [this.range[0], this.range[1]];
      this.apply();
    },
    togglePlay() {
      if (this.playing) return this.stop();
      this.playing = true;
      // From the start, unless the window is already partway through. A window that
      // covers everything has nowhere to go: play then uses a fifth of the range.
      if (this.span[1] >= this.range[1]) {
        const whole = this.range[1] - this.range[0];
        const covers = this.span[1] - this.span[0] >= whole;
        const width = Math.max(covers ? Math.round(whole / 5) : this.span[1] - this.span[0], ONE_DAY_MS);
        this.span = [this.range[0], Math.min(this.range[0] + width, this.range[1])];
        this.apply();
      }
      this.playTimer = setInterval(() => {
        const next = nextWindow(this.span[0], this.span[1], this.range[1]);
        this.span = [next.from, next.to];
        this.apply();
        if (next.done) this.stop();
      }, PLAY_INTERVAL_MS);
    },
    stop() {
      this.playing = false;
      clearInterval(this.playTimer);
      this.playTimer = null;
    },
  },
};
</script>

<style scoped>
.time-slider {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 900;
  width: min(560px, calc(100% - 32px));
  padding: 4px 12px 6px;
}
.time-slider__title {
  font-weight: 600;
}
.time-slider__dates {
  flex: 1 1 auto;
  text-align: right;
  padding-right: 4px;
  font-variant-numeric: tabular-nums;
}
</style>
/*% } %*/
