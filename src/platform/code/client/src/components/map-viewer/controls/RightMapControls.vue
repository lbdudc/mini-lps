/*% if (feature.MV_LayerManagement || feature.MV_LM_ExternalLayer || feature.MV_T_InformationMode || feature.MV_MM_MMV_MapSelectorInMapViewer || feature.MV_T_Export || feature.MV_T_F_BasicSearch) { %*/
<template>
  <div v-if="map" class="map-controls">
    <div class="column">
      <v-btn
        v-if="$vuetify.breakpoint.smAndDown"
        color="white"
        max-height="25"
        max-width="20"
        @click.stop="showButtons"
      >
        <div v-if="!showBtns && $vuetify.breakpoint.smAndDown">
          <v-icon>mdi-wrench-outline</v-icon>
          <v-icon>mdi-chevron-down</v-icon>
        </div>
        <div v-else>
          <v-icon>mdi-wrench-outline</v-icon>
          <v-icon>mdi-chevron-up</v-icon>
        </div>
      </v-btn>
    </div>

    <div v-show="showBtns || !$vuetify.breakpoint.smAndDown">
      /*% if (feature.MV_T_F_BasicSearch) { %*/
      <div class="column search-column">
        <v-tooltip left open-delay="200" color="var(--appColor)">
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              color="white"
              :class="{ 'btn-selected': searchOpen }"
              data-test="map-search-toggle"
              @click.stop="toggleSearch"
            >
              <v-icon>mdi-magnify</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("mapViewer.searchInMap") }}</span>
        </v-tooltip>

        <div v-if="searchOpen" class="search-panel">
          <v-text-field
            ref="searchInput"
            dense
            :value="form.query"
            @input="$emit('update-query', $event)"
            prepend-inner-icon="mdi-magnify"
            :label="$t('mapViewer.searchInMap')"
            @keydown.enter="$emit('search')"
            @click:clear="$emit('clear-search')"
            single-line
            hide-details
            outlined
            rounded
            clearable
            background-color="white"
            data-test="map-search-input"
          ></v-text-field>
          <search-results
            :query="form.query"
            :map="map"
            :overlays="overlays"
          ></search-results>
        </div>
      </div>
      /*% } %*/

      /*% if (feature.MV_LayerManagement) { %*/
      <div class="column">
        <layer-manager
          :loadingMap="loadingMap"
          :overlays="overlays"
          :map="map"
          /*% if (feature.MV_LM_StylePreview) { %*/
          @wms-legend-selected="buildControl"
          /*% } %*/
        ></layer-manager>
      </div>
      /*% } %*/

      <div v-if="bookmarks.length > 0" class="column">
        <v-menu left offset-x>
          <template v-slot:activator="{ on: menu, attrs }">
            <v-tooltip left open-delay="200" color="var(--appColor)">
              <template v-slot:activator="{ on: tooltip }">
                <v-btn v-bind="attrs" v-on="{ ...tooltip, ...menu }" color="white">
                  <v-icon>mdi-bookmark-multiple-outline</v-icon>
                </v-btn>
              </template>
              <span>{{ $t("mapViewer.bookmarks") }}</span>
            </v-tooltip>
          </template>
          <v-list dense>
            <v-list-item
              v-for="(bookmark, index) in bookmarks"
              :key="index"
              @click="goToBookmark(bookmark)"
            >
              <v-list-item-title>{{ bookmark.name }}</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </div>

      /*% if (feature.MV_LM_ExternalLayer) { %*/
      <div class="column">
        <v-tooltip left open-delay="200" color="var(--appColor)">
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              color="white"
              @click.stop="buildControl({ component: 'add-new-layer' })"
            >
              <v-icon>mdi-layers-plus</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("addNewLayer.addNewLayerTitle") }}</span>
        </v-tooltip>
      </div>
      /*% } %*/
      
      /*% if (feature.MV_Processes) { %*/
      <div class="column">
        <v-tooltip left open-delay="200" color="var(--appColor)">
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              color="white"
              @click.stop="buildControl({ component: 'toolbox' })"
            >
              <v-icon dense>mdi-toolbox</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("toolbox.title") }}</span>
        </v-tooltip>
      </div>
      /*% } %*/

      /*% if (feature.MV_LM_BaseLayerSelector) { %*/
      <div class="column">
        <v-tooltip left open-delay="200" color="var(--appColor)">
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              color="white"
              @click.stop="buildControl({ component: 'change-base-layer' })"
            >
              <v-icon>mdi-layers-edit</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("changeBaseLayer.changeBaseLayerTitle") }}</span>
        </v-tooltip>
      </div>
      /*% } %*/

      /*% if (feature.MV_MM_MMV_MapSelectorInMapViewer) { %*/
      <div v-if="hasMultipleMaps" class="column">
        <v-tooltip left open-delay="200" color="var(--appColor)">
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              color="white"
              @click.stop="buildControl({ component: 'change-map' })"
            >
              <v-icon>mdi-map-outline</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("mapViewer.mapSelector") }}</span>
        </v-tooltip>
      </div>
      /*% } %*/

      /*% if (feature.MV_T_Export) { %*/
      <div class="column">
        <v-tooltip left open-delay="200" color="var(--appColor)">
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              color="white"
              @click.stop="buildControl({ component: 'export-management' })"
            >
              <v-icon>mdi-export-variant</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("mapViewer.export") }}</span>
        </v-tooltip>
      </div>
      /*% } %*/

      /*% if (feature.MV_T_InformationMode) { %*/
      <div class="column">
        <v-tooltip left open-delay="200" color="var(--appColor)">
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              :class="WMSInfoControlBtn ? null : 'btn-selected'"
              @click.stop="buildWMSInfoControl"
            >
              <v-icon v-if="WMSInfoControlBtn" color="red"
              >mdi-alert-circle</v-icon
              >
              <v-icon v-else color="green">mdi-alert-circle</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("mapViewer.wmsDetail.title") }}</span>
        </v-tooltip>
      </div>
      /*% } %*/
    </div>
  </div>
</template>

<script>
/*% if (feature.MV_LayerManagement) { %*/
import LayerManager from "./LayerManager.vue";
/*% } %*/
/*% if (feature.MV_T_F_BasicSearch) { %*/
import SearchResults from "../search/SearchResults.vue";
/*% } %*/

export default {
  name: "RightMapControls",
  /*% if (feature.MV_T_InformationMode || feature.MV_T_F_BasicSearch) { %*/
  watch: {
    /*% if (feature.MV_T_F_BasicSearch) { %*/
    /* a search that arrives after this is created (from a shared link) opens the field */
    "form.query"(query) {
      if (query) this.searchOpen = true;
    },
    /*% } %*/
    /*% if (feature.MV_T_InformationMode) { %*/
    $route() {
      // Deactivate info control if it is active
      if (!this.WMSInfoControlBtn) {
        this.buildWMSInfoControl();
      }
    },
    /*% } %*/
  },
  /*% } %*/
  components: {
    /*% if (feature.MV_LayerManagement) { %*/
    LayerManager,
    /*% } %*/
    /*% if (feature.MV_T_F_BasicSearch) { %*/
    "search-results": SearchResults,
    /*% } %*/
  },
  props: {
    overlays: {
      type: Array,
      default: () => [],
    },
    map: {
      type: Object,
      default: () => {},
    },
    /*% if (feature.MV_T_F_BasicSearch) { %*/
    form: {
      type: Object,
      default: () => ({ query: null }),
    },
    /*% } %*/
    /*% if (feature.MV_MM_MMV_MapSelectorInMapViewer) { %*/
    hasMultipleMaps: {
      type: Boolean,
      default: false,
    },
    /*% } %*/
    loadingMap: {
      type: Boolean,
      required: false,
    },
    bookmarks: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      showBtns: false,
      /*% if (feature.MV_T_F_BasicSearch) { %*/
      /* a search already in the URL (a shared link) keeps the field open */
      searchOpen: !!(this.form && this.form.query),
      /*% } %*/
      /*% if (feature.MV_T_InformationMode) { %*/
      WMSInfoControlBtn: true,
      /*% } %*/
    };
  },
  methods: {
    showButtons() {
      this.showBtns = !this.showBtns;
    },

    /*% if (feature.MV_T_F_BasicSearch) { %*/
    toggleSearch() {
      this.searchOpen = !this.searchOpen;
      if (this.searchOpen) {
        this.$nextTick(() => this.$refs.searchInput && this.$refs.searchInput.focus());
      }
    },
    /*% } %*/

    goToBookmark(bookmark) {
      this.map.getLeafletMap().fitBounds(bookmark.bounds);
    },

    buildControl(args) {
      this.$emit("build-control", {
        component: args.component,
        /*% if (feature.MV_LM_StylePreview) { %*/
        layer: args.layer,
        /*% } %*/
        /*% if (feature.MV_T_InformationMode) { %*/
        filteredFeatures: args.filteredFeatures,
        /*% } %*/
      });
    },

    /*% if (feature.MV_T_InformationMode) { %*/
    buildWMSInfoControl() {
      if (this.WMSInfoControlBtn) {
        document.getElementById("map").style.setProperty("cursor", "pointer");
        this.map.activateLayerInfo(this.wmsCallback);
      } else {
        this.map.deactivateLayerInfo();
        document.getElementById("map").style.setProperty("cursor", "grab");
      }
      this.WMSInfoControlBtn = !this.WMSInfoControlBtn;
    },

    wmsCallback(features) {
      /* what the search text hides is not drawn: the layers are filtered by it (CQL) */
      let filteredFeatures = features;
      if (filteredFeatures.length > 0) {
        this.buildControl({
          component: "wms-information",
          filteredFeatures: filteredFeatures,
        });
      }
    },
    /*% } %*/
  },
};
</script>

<style scoped>
.map-controls {
  position: absolute;
  z-index: 1000;
  top: 12px;
  right: 8px;
  margin-right: 0px;
  padding: 0;
}
.column {
  margin-top: 0.6em;
  padding-right: 0;
}

/* The search field opens to the left of the buttons, level with its own button; the
   suggestions (SearchResults) hang under it. */
.search-column {
  position: relative;
}

.search-panel {
  position: absolute;
  top: 0;
  right: 100%;
  margin-right: 10px;
  width: min(340px, calc(100vw - 96px));
}

/* Every floating control button (wrench toggle, layer manager, add layer,
   toolbox, base layer, info mode) gets the same rounded, elevated look
   instead of a flat undecorated square, with a subtle lift on hover. */
.map-controls ::v-deep .v-btn {
  border-radius: 50% !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3) !important;
  transition: box-shadow 0.15s ease, transform 0.15s ease;
}

.map-controls ::v-deep .v-btn:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.4) !important;
  transform: translateY(-1px);
}

/* Fade transition for a control panel opening/closing (e.g. the layer
   manager, add-new-layer dialog). */
.v-enter-active,
.v-leave-active {
  transition: opacity 0.4s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}

.btn-selected {
  border: 2px solid var(--appColor);
}

.arrow-icon {
  font-size: 24px;
}
</style>
/*% } %*/
