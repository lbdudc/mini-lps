/*% if (feature.MV_LayerManagement || feature.MV_LM_ExternalLayer || feature.MV_T_ViewMapAsList || feature.MV_T_InformationMode) { %*/
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

      /*% if (feature.MV_T_ViewMapAsList) { %*/
      <div class="column">
        <v-tooltip left open-delay="200" color="var(--appColor)">
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              color="white"
              @click.stop="buildControl({ component: 'list-layer-management' })"
            >
              <v-icon>mdi-format-list-bulleted</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("mapViewer.listLayerElements") }}</span>
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

export default {
  name: "RightMapControls",
  /*% if (feature.MV_T_InformationMode) { %*/
  watch: {
    $route() {
      // Deactivate info control if it is active
      if (!this.WMSInfoControlBtn) {
        this.buildWMSInfoControl();
      }
    },
  },
  /*% } %*/
  components: {
    /*% if (feature.MV_LayerManagement) { %*/
    LayerManager,
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
    /*% if (feature.MV_T_InformationMode) { %*/
    form: {
      type: Object,
      default: () => {},
    },
    /*% } %*/
    loadingMap: {
      type: Boolean,
      required: false,
    }
  },
  data() {
    return {
      showBtns: false,
      /*% if (feature.MV_T_InformationMode) { %*/
      WMSInfoControlBtn: true,
      /*% } %*/
    };
  },
  methods: {
    showButtons() {
      this.showBtns = !this.showBtns;
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
      let filteredFeatures =
        /*% if (feature.MV_T_F_BasicSearch) { %*/
        (this.form.query != null && this.form.query !== "")
          ? features.filter(f => f.id.endsWith(this.form.query))
          : /*% } %*/ features;
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
  top: 4px;
  right: 0px;
  margin-right: 0px;
  padding: 0;
}
.column {
  margin-top: 0.6em;
  padding-right: 8px;
}

/* we will explain what these classes do next! */
.v-enter-active,
.v-leave-active {
  transition: opacity 0.4s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}

.btn-selected {
  border: 2px solid #1976d2;
}

.arrow-icon {
  font-size: 24px;
}
</style>
/*% } %*/
