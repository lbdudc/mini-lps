/*% if (feature.MapViewer) { %*/
<template>
  <v-container fluid id="map-container">
    <v-row justify="start" class="mb-1" no-gutters>
      <v-col class="d-none d-md-block mr-2">
        <span class="headline no-split-words">
          {{ $t($route.meta.label) }}
        </span>
      </v-col>
      /*%
        var cols = 12;
        var md = 6;
        if(feature.MV_T_ViewMapAsList && feature.MV_MM_MMV_MapSelectorInMapViewer){
          cols = 6;
          md = 3;
        }
      %*/
      /*% if (feature.MV_T_ViewMapAsList) { %*/
      <list-layer-management v-if="map" :map="map"></list-layer-management>
      /*% } %*/
      /*% if (feature.MV_MM_MMV_MapSelectorInMapViewer) { %*/
      <v-col cols="/*%= cols %*/" md="/*%= md %*/">
        <v-row class="justify-end" no-gutters>
          <v-col cols="12" sm="5" class="mt-4 mr-4 text-center text-sm-right">
            <label>
              {{ $t("mapViewer.mapSelector") }}
            </label>
          </v-col>
          <v-col cols="12" sm="6">
            <v-select
              :items="maps"
              item-value="id"
              item-text="label"
              :label="$t('mapViewer.map')"
              v-if="maps.length > 0"
              v-model="mapSelected"
              @change="changeRoute"
              solo
            >
            </v-select>
          </v-col>
        </v-row>
      </v-col>
      /*% } %*/
    </v-row>
    /*% if (feature.MV_T_F_BasicSearch || feature.MV_T_Export) { %*/
    <v-row class="mt-1 mb-4" no-gutters>
      <v-col class="text-right">
        /*% if (feature.MV_T_F_BasicSearch) { %*/
        <v-text-field
          dense
          v-model="form.query"
          append-icon="search"
          @click:append="searchInMap"
          :label="$t('mapViewer.searchInMap')"
          @keydown.enter="searchInMap"
          single-line
          hide-details
          class="d-inline-block"
        ></v-text-field>
        /*% } %*/
        /*% if (feature.MV_T_Export) { %*/
        <v-btn
          class="primary ml-2"
          @click="openDialog({ component: 'export-management' })"
        >
          {{ $t("mapViewer.export") }}
        </v-btn>
        /*% } %*/
        /*% if (feature.ChartViewer) { %*/
        <v-btn
          class="ml-2"
          color="primary"
          outlined
          @click="goToChartViewer"
        >
          <v-icon left>mdi-chart-line</v-icon>
          {{ $t("chartViewer.title") }}
        </v-btn>
        /*% } %*/
      </v-col>
    </v-row>
    /*% } %*/

    <div ref="map" id="map">
    /*% if (feature.MV_LayerManagement || feature.MV_LM_ExternalLayer || feature.MV_T_ViewMapAsList || feature.MV_T_InformationMode || feature.MV_LM_BaseLayerSelector) { %*/
      <right-map-controls
        :overlays="viewOverlays"
        :map="map"
        :loadingMap="loadingMap"
        /*% if (feature.MV_T_InformationMode && feature.MV_T_F_BasicSearch) { %*/:form="form"/*% } %*/
        @build-control="openDialog"
      ></right-map-controls>
    /*% } %*/
    </div>

    /*% if (feature.MV_T_Export || feature.MV_LM_ExternalLayer || feature.MV_T_ViewMapAsList || feature.MV_T_InformationMode || feature.MV_LM_BaseLayerSelector) { %*/
    <v-dialog
      v-model="showDialog"
      hide-overlay
      :fullscreen="$vuetify.breakpoint.mdAndDown"
      :width="/*% if (feature.MV_T_InformationMode) { %*/wmsFeatures != null ? 1200 :/*% } %*/500"
      @click:outside="closeDialog"
    >
      <component
        v-if="dialogComponent"
        v-bind:is="dialogComponent"
        :map="map"
      /*% if (feature.MV_T_InformationMode) { %*/:features="wmsFeatures"/*% } %*/
      /*% if (feature.MV_LM_StylePreview) { %*/:wmsLegend="wmsLegendSelected"/*% } %*/
        @layer-added="refreshLayerManager"
        @close="closeDialog"
      ></component>
    </v-dialog>
    /*% } %*/
    /*% if (feature.MV_DetailOnClick) { %*/
    <information-popup
      ref="informationPopup"
      :form="popupForm"
      :layer="popupLayer"
    ></information-popup>
    /*% } %*/
  </v-container>
</template>

<script>
import maps from "./config-files/maps.json";
import layers from "./config-files/layers.json";
/*% if (feature.MV_DetailOnClick) { %*/
import InformationPopup from "@/components/map-viewer/InformationPopup";
/*% } %*/
import properties from "@/properties";
/*% if (feature.MV_CI_Map || feature.MV_T_ZoomWindow) { %*/
import devCheck from "@/common/device-check";
/*% } %*/
/*% if (feature.MV_T_Export) { %*/
import ExportManagement from "./export-management/ExportManagement";
/*% } %*/
/*% if (feature.MV_LayerManagement || feature.MV_LM_ExternalLayer || feature.MV_T_ViewMapAsList || feature.MV_T_InformationMode) { %*/
import RightMapControls from "./controls/RightMapControls.vue";
/*% } %*/
/*% if (feature.MV_LM_ExternalLayer) { %*/
import AddNewLayer from "./add-new-layer/AddNewLayer";
/*% } %*/
/*% if (feature.MV_LM_BaseLayerSelector ) { %*/
import ChangeBaseLayer from "./change-base-layer/ChangeBaseLayer";
/*% } %*/
/*% if (feature.MV_T_ViewMapAsList) { %*/
import ListLayerManagement from "./list-layer-management/ListLayerManagement";
/*% } %*/
/*% if (feature.MV_T_InformationMode) { %*/
import WMSInformation from "./wms-information/WMSInformation";
/*% } %*/
/*% if (feature.MV_LM_StylePreview) { %*/
import WMSLegend from "./wms-legend/WMSLegend.vue";
/*% } %*/
/*% if (feature.MV_T_E_F_URL) { %*/
import RepositoryFactory from "@/repositories/RepositoryFactory";
/*% } %*/
/*% if (feature.MV_T_E_F_URL) { %*/
const MVExportManagementRepository = RepositoryFactory.get("MVExportManagementRepository");
/*% } %*/
/*% if (feature.MV_MS_GJ_Paginated) { %*/
import { updateLayer } from "./common/map-layer-common";
/*% } %*/
import { createMap, loadBaseLayers, loadOverlaysLayers } from "./common/map-common";

import { /*% if (feature.MV_CI_Scale) { %*/buildMapScaleControl,/*% } %*/
  /*% if (feature.MV_CI_CenterCoordinates) { %*/ buildMapCoordinatesControl,/*% } %*/
  /*% if (feature.MV_CI_Map) { %*/ buildMiniMapControl,/*% } %*/
  /*% if (feature.MV_T_MeasureControl) { %*/ buildMeasureControl,/*% } %*/
  /*% if (feature.MV_T_ZoomWindow) { %*/ buildZoomBoxControl,/*% } %*/
  /*% if (feature.MV_T_UserGeolocation) { %*/ buildLocateControl/*% } %*/
} from "./common/map-controls";

export default {
  name: "Map",
  /*% if (feature.MV_T_Export || feature.MV_T_ViewMapAsList || feature.MV_T_InformationMode || feature.MV_DetailOnClick) { %*/
  components: {
    /*% if (feature.MV_LayerManagement || feature.MV_T_ViewMapAsList || feature.MV_T_InformationMode) { %*/
    RightMapControls,
    /*% } %*/
    /*% if (feature.MV_DetailOnClick) { %*/"information-popup": InformationPopup,/*% } %*/
    /*% if (feature.MV_T_Export) { %*/"export-management": ExportManagement,/*% } %*/
    /*% if (feature.MV_LM_BaseLayerSelector) { %*/"change-base-layer": ChangeBaseLayer,/*% } %*/
    /*% if (feature.MV_T_ViewMapAsList) { %*/"list-layer-management": ListLayerManagement,/*% } %*/
    /*% if (feature.MV_T_InformationMode) { %*/"wms-information": WMSInformation,/*% } %*/
    /*% if (feature.MV_LM_ExternalLayer) { %*/"add-new-layer": AddNewLayer,/*% } %*/
    /*% if (feature.MV_LM_StylePreview) { %*/"wms-legend": WMSLegend,/*% } %*/
  },
  /*% } %*/
  data() {
    return {
      /*% if (feature.MV_T_F_BasicSearch) { %*/
      form: { query: null },
      /*% } %*/
      /*% if (feature.MV_T_Export || feature.MV_LM_ExternalLayer || feature.MV_T_ViewMapAsList || feature.MV_T_InformationMode || feature.MV_LM_BaseLayerSelector) { %*/
      dialogComponent: null,
      showDialog: false,
      /*% if (feature.MV_T_InformationMode) { %*/
      wmsFeatures: null,
      /*% } %*/
      /*% } %*/
      mapSelected: null,
      map: null,
      maps: [],
      viewOverlays: [],
      loadingMap: false,
      /*% if (feature.MV_DetailOnClick) { %*/
      popupForm: null,
      popupLayer: null,
      /*% } %*/
      /*% if (feature.MV_LM_StylePreview) { %*/
      wmsLegendSelected: null,
      /*% } %*/
    };
  },
  watch: {
    $route() {
      const routeSelectedMap = this.maps.find(
        (map) => map.name === this.$route.params.id
      );
      if (routeSelectedMap) {
        this.mapSelected = routeSelectedMap.id;
        this.loadMap();
      }
      this.viewOverlays = this.loadOverlays();
      // Needed for child to refresh his overlays
      this.refreshLayerManager();
    },
  },
  mounted() {
    this.maps = maps.maps.map((map, index) => {
      return {
        id: index,
        name: map.name,
        label: this.$t("mapViewer.map-label." + map.name.replace('.', '-'))
      };
    });
    let mapFromRoute = null;

    /*% if (feature.MV_T_F_BasicSearch) { %*/
    this.form.query = this.$route.query.query;
    /*% } %*/

    if (this.$route.params.id) {
      mapFromRoute = this.maps.find(map => map.name === this.$route.params.id);
      if (mapFromRoute) {
        this.mapSelected = mapFromRoute.id;
        this.loadMap();
      }
    }
    if (!mapFromRoute && this.maps.length === 1) {
      this.mapSelected = this.maps[0].id;
      this.changeRoute();
    } else if (this.$route.params.id) {
      this.changeRoute();
    }

    /*% if (feature.MV_MS_GJ_Paginated) { %*/
    // update features on map move
    if (this.map) {
      this.map.getLeafletMap().on("moveend", () => {
        const bbox = this.map.getLeafletMap().getBounds();
        updateLayer(this.map, bbox, this.geoJSONPopupFunction);
      });
    }
    /*% } %*/
  },
  beforeDestroy() {
    localStorage.setItem("state", JSON.stringify(this.map.exportState()));
  },
  methods: {
    loadOverlays() {
      return layers.layers.filter((l) =>
        maps.maps[this.mapSelected].layers.find(
          (mapLayer) => !mapLayer.baseLayer && mapLayer.name == l.name
        )
      );
    },
    /*% if (feature.MV_T_Export || feature.MV_LM_ExternalLayer || feature.MV_T_ViewMapAsList || feature.MV_T_InformationMode || feature.MV_LM_StylePreview || feature.MV_LM_BaseLayerSelector) { %*/
    closeDialog() {
      this.dialogComponent = "";
      this.showDialog = false;
      this.wmsFeatures = null;
      this.wmsLegendSelected = null;
    },
    openDialog(args) {
      /*% if (feature.MV_LM_StylePreview) { %*/
      if (args.layer) this.wmsLegendSelected = args.layer;
      /*% } %*/
      /*% if (feature.MV_T_InformationMode) { %*/
      if (args.filteredFeatures) this.wmsFeatures = args.filteredFeatures;
      /*% } %*/
      this.dialogComponent = args.component;
      this.showDialog = true;
    },
    /*% } %*/
    changeRoute() {
      if (
        this.$route.params.id !== this.maps[this.mapSelected].name /*% if (feature.MV_T_F_BasicSearch) { %*/||
        this.$route.query.query !== this.form.query /*% } %*/
      ) {
        this.$router.replace({
          name: "MapViewer",
          params: {
            id: this.maps[this.mapSelected].name
          }/*% if (feature.MV_T_F_BasicSearch) { %*/,
          query: { query: this.form.query || undefined }/*% } %*/
        });
      }
    },
    loadMap() {
      // If map is already created, we remove it
      if (this.map != null) {
        this.map.clearMap();
        this.map.getLeafletMap().off();
        this.map.getLeafletMap().remove();
      }

      // We don't continue creating a map if there is no one selected
      if (this.mapSelected == null) {
        return;
      }

      // Setting title's tab
      document.title =
        this.$t(`mapViewer.map-label.${maps.maps[this.mapSelected].name}`) +
        " - " +
        properties.APP_NAME;

      // Creating map
      this.map = createMap(this.mapSelected);
      /*% if (feature.MV_MS_GJ_Paginated) { %*/
      this.map.centerView();
      /*% } %*/

      /*% if (feature.MV_CI_Scale) { %*/
      this.map.addControl(buildMapScaleControl());
      /*% } %*/
      /*% if (feature.MV_CI_CenterCoordinates) { %*/
      this.map.addControl(buildMapCoordinatesControl());
      /*% } %*/
      /*% if (feature.MV_CI_Map) { %*/
      // We already obtain the base layers to obtain the default base layer of the map to build the Mini-Map
      let baseLayers = layers.layers.filter((layer) =>
        maps.maps[this.mapSelected].layers.find(
          (mapLayer) => mapLayer.name == layer.name && mapLayer.baseLayer
        )
      );

      // Minimap control - we get the mini-map base layer from current map default base layer
      const defaultBaseLayer = baseLayers.find((bl) =>
        maps.maps[this.mapSelected].layers.find(
          (l) => l.selected && bl.name === l.name
        )
      );
      this.map.addControl(
        buildMiniMapControl(devCheck.isMobile(), defaultBaseLayer)
      );
      /*% } %*/
      /*% if (feature.MV_T_E_F_URL) { %*/
      if (this.$route.query.token != null) {
        MVExportManagementRepository
          .import(this.$route.query.token)
          .then(data => {
            this.map.importState(data);
            this.refreshLayerManager();
          });
      }
      /*% } %*/
      /*% if (feature.MV_T_MeasureControl) { %*/
      this.map.addControl(buildMeasureControl());
      /*% } %*/
      /*% if (feature.MV_T_ZoomWindow) { %*/
      if (!devCheck.isMobile()) {
        this.map.addControl(buildZoomBoxControl());
      }
      /*% } %*/
      /*% if (feature.MV_T_UserGeolocation) { %*/
      this.map.addControl(buildLocateControl());
      /*% } %*/

      // Loading base layers
      loadBaseLayers(this.map, this.mapSelected);

      // Loading overlays
      loadOverlaysLayers(this.map, this.mapSelected/*% if (feature.MV_DetailOnClick) { %*/, this.geoJSONPopupFunction/*% } %*//*% if (feature.MV_T_F_BasicSearch) { %*/, this.form/*% } %*/);

      /*% if (feature.GUI_L_ViewListAsMap || feature.GUI_L_LocateInMap) { %*/
      // hiding layers not selected (by url params)
      if (this.$route.query.layers) {
        // ?layers=layerA&layers=layerB <---- array
        // ?layers=layerA,layerB <---- string
        let visibleLayers = Array.isArray(this.$route.query.layers)
          ? this.$route.query.layers
          : this.$route.query.layers.split(",");

        this.map.getOverlays().forEach(layer => {
          if (!visibleLayers.includes(layer.getId())) {
            layer.hide();
          }
        });
      }
      /*% } %*/

      // focusing visible layers if focus (by url params)
      if (this.$route.query.focus) {
        this.map.centerView("visible");
      }

      /*% if (feature.GUI_L_LocateInMap) { %*/
      // highlight item
      let layerPreffix, targetId;
      if (this.$route.query.item) {
        let item = this.$route.query.item;
        layerPreffix = item.substring(0, item.lastIndexOf("-") + 1);
        targetId = item.substring(item.lastIndexOf("-") + 1, item.length);
      }
      this.map
        .getOverlays()
        .filter(layer => {
          return layer.getId().startsWith(layerPreffix);
        })
        .forEach(layer => {
          targetId = parseInt(targetId);
          layer.highlight(targetId);
        });
      /*% } %*/

      if (localStorage.getItem("state")) {
        this.map.importState(JSON.parse(localStorage.getItem("state")));
        localStorage.removeItem("state");
      }
    },
    /*% if (feature.MV_DetailOnClick) { %*/
    geoJSONPopupFunction(form) {
      return (layer) => {
        this.popupForm = form;
        this.popupLayer = layer;
        return this.$refs.informationPopup.$el;
      };
    },
    /*% } %*/
    /*% if (feature.MV_T_F_BasicSearch) { %*/
    searchInMap() {
      if (this.form.query !== this.$route.query.query) {
        localStorage.setItem("state", JSON.stringify(this.map.exportState()));
        this.changeRoute()
      }
    },
    /*% } %*/
    refreshLayerManager(/*% if (feature.MV_DetailOnClick) { %*/id/*% } %*/) {
      /*% if (feature.MV_DetailOnClick) { %*/
      if (id) {
        const layer = layers.layers.find((l) => l.name === id);
        if (this.map.getLayer(id).options.hasOwnProperty("popup"))
          this.map
            .getLayer(id)
            .bindPopup(this.geoJSONPopupFunction(layer.form));
      }
      /*% } %*/

      this.loadingMap = true;
      setTimeout(() => {
        this.loadingMap = false;
      }, 0);
    },
    /*% if (feature.ChartViewer) { %*/
    goToChartViewer() {
      this.$router.push({
        name: 'chartViewer'
      });
    },
    /*% } %*/
  }
};
</script>

<style lang='css' scoped>
#map-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

#map {
  width: 100%;
  height: 100%;
  z-index: 1;
}

::v-deep .leaflet-top.leaflet-right {
  margin-top: 10px !important;
}

::v-deep .leaflet-top.leaflet-right .leaflet-control {
  margin-top: 0 !important;
}

::v-deep .leaflet-layer-manager-toggle {
  width: 30px !important;
  height: 30px !important;
}

::v-deep .leaflet-layer-manager-overlays {
  display: inline-grid;
}

::v-deep .leaflet-add-layer-control-icon {
  width: 30px !important;
  height: 30px !important;
}

::v-deep .leaflet-layer-manager-selector {
  vertical-align: text-top;
}

::v-deep .leaflet-layer-manager-style-selector {
  float: right;
  margin-right: 5px;
  margin-top: 3px;
}

::v-deep .leaflet-layer-manager-icon {
  max-height: 16px;
  position: relative;
  top: 4px;
}

::v-deep .leaflet-layer-manager-icon-square {
  display: inline-block;
  height: 13px;
  width: 16px;
  border: 2px solid white;
  border-right-width: 3px;
  border-bottom-width: 0px;
  margin-bottom: -1px;
  margin-top: 4px;
}

::v-deep .leaflet-layer-manager-button {
  float: right;
  margin-right: 5px;
  margin-top: 3px;
  background-repeat: no-repeat;
  background-size: 15px 15px;
  background-position: center;
  height: 16px;
  width: 10px;
}

/*% if (feature.MV_T_InformationMode) { %*/
::v-deep .disabled {
  color: red !important;
}
::v-deep .enabled {
  color: green !important;
}
/*% } %*/
/*% if (feature.MV_T_UserGeolocation) { %*/
::v-deep .user-location-icon {
  background: url("../../assets/user-location-icon.png");
  background-repeat: inherit;
  background-size: 100%;
}
/*% } %*/
</style>
/*% } %*/
