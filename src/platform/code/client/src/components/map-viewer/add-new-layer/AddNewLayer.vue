/*% if (feature.MV_LM_ExternalLayer) { %*/
<template>
  <v-card class="card">
    <v-card-title primary-title class="headline card-title white--text">
      <v-container class="pa-0 ma-0">
        <v-row no-gutters>
          <v-col class="ma-0">
            {{ $t("addNewLayer.addNewLayerTitle") }}
          </v-col>
          <v-col cols="1">
            <v-btn @click="close" color="white" icon>
              <v-icon>mdi-close</v-icon>
            </v-btn>
          </v-col>
        </v-row>
      </v-container>
    </v-card-title>
    <v-card-text>
      <v-container class="ma-0 pa-0">
        <v-row no-gutters>
          <v-radio-group v-model="radios" row mandatory>
            <v-radio
              class="radio"
              name="radio2"
              :label="$t('addNewLayer.addExistentLayer')"
              value="1"
              @click="
                () => ((this.newLayer = null), (this.selectedUrlLayer = null))
              "
            ></v-radio>

            <v-radio
              class="radio"
              name="radio1"
              :label="$t('addNewLayer.addUrlLayer')"
              value="0"
              @click="() => ((this.newLayer = null), (this.layerName = null))"
            ></v-radio>
          </v-radio-group>
        </v-row>

        <v-divider class="mb-4"></v-divider>
        <v-row no-gutters>
          <v-col cols="12">
            <v-form ref="formUrl" v-model="isValidUrl" class="formData">
              <!-- Layer from URL -->
              <v-row align="center" no-gutters v-if="radios == '0'">
                <v-col class="text-left mt-6" cols="12" md="8">
                  <v-text-field
                    v-model="url"
                    clearable
                    dense
                    :label="$t('addNewLayer.url')"
                    outlined
                    :rules="[(v) => !!v || $t('addNewLayer.urlRequired')]"
                    @click:clear="
                      () => (
                        (loadingUrl = false),
                        (url = null),
                        (urlLayers = null),
                        (newLayer = null)
                      )
                    "
                    @keydown.enter.prevent="searchLayers"
                  ></v-text-field>
                </v-col>
                <v-col
                  v-if="radios == '0'"
                  cols="12"
                  md="3"
                  offset-md="1"
                  class="text-left"
                >
                  <v-btn
                    color="#6caac6"
                    class="white--text"
                    :loading="loadingUrl"
                    @click="searchLayers"
                  >
                    <v-icon class="mr-1">mdi-magnify</v-icon>
                    {{ $t("addNewLayer.search") }}
                  </v-btn>
                </v-col>
              </v-row>
            </v-form>

            <v-form ref="formLayer" v-model="isValidLayer" class="formData">
              <!-- Layer from URL -->
              <v-row
                v-if="radios == '0' && !loadingUrl && urlLayers != null"
                align="center"
                no-gutters
              >
                <v-col cols="8">
                  <v-select
                    v-model="selectedUrlLayer"
                    outlined
                    dense
                    :items="urlLayers"
                    :item-value="(item) => item"
                    :label="$t('addNewLayer.selectLayer')"
                    :menu-props="{ offsetY: true }"
                    :rules="[(v) => !!v || $t('addNewLayer.layerRequired')]"
                    @change="createLayer"
                  >
                    <template v-slot:item="{ item }">
                      {{ item.layerTitle || item.layerName }}
                    </template>
                    <template v-slot:selection="{ item }">
                      {{ item.layerTitle || item.layerName }}
                    </template>
                  </v-select>
                </v-col>
                <v-col cols="12" md="3" offset-md="1" v-if="urlLayers != null">
                  <v-btn class="appButton" @click="addLayer"
                  ><v-icon class="mr-2"> mdi-plus </v-icon>
                    {{ $t("addNewLayer.add") }}
                  </v-btn>
                </v-col>
              </v-row>

              <!-- Layer from layers.json -->
              <v-row v-if="radios == '1'" align="center" no-gutters>
                <v-col class="text-left mt-6">
                  <v-autocomplete
                    v-model="layerName"
                    outlined
                    dense
                    :items="layersFromJSON"
                    item-text="name"
                    item-value="id"
                    :label="$t('addNewLayer.selectLayer')"
                    :menu-props="{ offsetY: true }"
                    :rules="[(v) => !!v || $t('addNewLayer.layerRequired')]"
                    @change="createLayer"
                  >
                    <template v-slot:item="{ item }">
                      {{ $t("mapViewer.layer-label." + item.id) }}
                    </template>
                    <template v-slot:selection="{ item }">
                      {{ $t("mapViewer.layer-label." + item.id) }}
                    </template>
                  </v-autocomplete>
                </v-col>
                <v-col cols="12" md="3" offset-md="1">
                  <v-btn class="appButton" @click="addLayer"
                  ><v-icon class="mr-2"> mdi-plus </v-icon>
                    {{ $t("addNewLayer.add") }}
                  </v-btn>
                </v-col>
              </v-row>
            </v-form>
          </v-col>
        </v-row>

        <v-row no-gutters>
          <v-col cols="12">
            <map-preview
              v-show="newLayer != null"
              id="extension"
              height="250px"
              :entity="{ sampleGeom: null }"
              propName="extension"
              :newLayer="newLayer"
            ></map-preview>
          </v-col>
        </v-row>
      </v-container>
    </v-card-text>
  </v-card>
</template>

<script>
import layerList from "@/components/map-viewer/config-files/layers.json";
import MapPreview from "@/components/map-viewer/add-new-layer/MapPreview.vue";
import { createGeoJSONLayer, createWMSLayer } from "../common/map-layer-common";

export default {
  name: "AddNewLayerControl",
  props: ["map"],
  components: {
    MapPreview,
  },
  data() {
    return {
      isValidLayer: false,
      isValidUrl: false,
      layerName: null,
      layersFromJSON: [],
      loadingUrl: false,
      newLayer: null,
      radios: null,
      selectedUrlLayer: null,
      url: null,
      urlLayers: null,
    };
  },
  mounted() {
    layerList.layers.forEach((layer) => {
      if (layer.layerType !== "tilelayer" && !this.map.getLayer(layer.name)) {
        this.layersFromJSON.push({
          id: layer.name,
          name: this.$t("mapViewer.layer-label." + layer.name),
        });
      }
    });

    // Sorting layers based on i18n
    this.layersFromJSON.sort((a, b) => {
      const renamedA = this.$t("mapViewer.layer-label." + a.id);
      const renamedB = this.$t("mapViewer.layer-label." + b.id);
      return renamedA.localeCompare(renamedB);
    });
  },
  methods: {
    close() {
      this.$emit("close");
      this.$destroy();
    },
    createLayer() {
      // Return error if no layer is selected
      if (this.layerName == null && this.selectedUrlLayer == null) {
        this.$refs.formLayer.validate();
        return;
      }

      const layerParams = {
        added: true,
      };

      if (this.layerName != null && this.radios === "1") {
        const layer = layerList.layers.find(
          (layer) => layer.name === this.layerName
        );

        layerParams.label = this.$t(
          "mapViewer.layer-label." + layer.name.replace(".", "-")
        );
        /*% if (feature.MV_MS_GJ_Paginated) { %*/
        layerParams.bounds = this.map.getLeafletMap().getBounds();
        /*% } %*/

        if (layer.layerType === "wms") {
          this.map.removeLayer(layer.name);
          this.newLayer = createWMSLayer(layer, layerParams);
        } else if (layer.layerType === "geojson") {
          this.map.removeLayer(layer.name);
          this.newLayer = createGeoJSONLayer(layer, layerParams);
        }
      } else if (this.selectedUrlLayer != null) {
        this.map.removeLayer(this.selectedUrlLayer.layerName);
        layerParams.label = this.selectedUrlLayer.layerTitle || this.selectedUrlLayer.layerName;
        const layer = {
          name: this.selectedUrlLayer.layerName,
          url: this.url,
          options: {
            layers: [this.selectedUrlLayer.layerName],
            format: "image/png",
            transparent: true,
          },
        }
        this.newLayer = createWMSLayer(layer, layerParams);
      }
    },
    addLayer() {
      if (this.newLayer == null) return;

      this.map.addLayer(this.newLayer);
      this.$emit("layer-added", this.newLayer.options.id);
      this.close();
    },
    searchLayers() {
      this.loadingUrl = true;
      this.urlLayers = null;
      this.selectedUrlLayer = null;
      if (this.isValidUrl) {
        try {
          new URL(
            this.url + "?service=WMS&version=1.1.1&request=GetCapabilities"
          );
        } catch (error) {
          this.$notify({
            text: this.$t("addNewLayer.invalidUrl"),
            type: "error",
          });
          this.loadingUrl = false;
          return;
        }

        fetch(
          new URL(
            this.url + "?service=WMS&version=1.1.1&request=GetCapabilities"
          )
        )
          .then((response) => response.text())
          .then((data) => {
            this.urlLayers = [];
            const xmlDoc = new DOMParser().parseFromString(
              data,
              "application/xml"
            );

            const layerNames = Array.from(
              xmlDoc.querySelectorAll("Capability Layer Layer > Name")
            ).map((layerName) => layerName.textContent);

            const layerTitles = Array.from(
              xmlDoc.querySelectorAll("Capability Layer Layer > Title")
            ).map((layerTitle) => layerTitle.textContent);

            for (let i = 0; i < layerNames.length; i++) {
              const layer = {
                layerName: layerNames[i],
                layerTitle: layerTitles[i],
              };
              this.urlLayers.push(layer);
            }

            this.urlLayers.sort((a, b) =>
              a.layerName.localeCompare(b.layerName)
            );

            if (this.urlLayers.length === 0) {
              this._errorNoLayersFound(true);
            }
          })
          .catch(() => this._errorNoLayersFound())
          .finally(() => (this.loadingUrl = false));
      } else {
        this.$refs.formUrl.validate();
        this.loadingUrl = false;
      }
    },
    _errorNoLayersFound(resetVariables = false) {
      if (resetVariables) {
        this.urlLayers = null;
      }
      this.$notify({
        text: this.$t("addNewLayer.noLayers"),
        type: "error",
      });
    },
  },
};
</script>

<style scoped>
.card-title {
  background-color: #1976d2;
}
.radio {
  font-size: 0.9em;
}
.formData {
  margin-top: -20px;
  margin-left: 20px;
  margin-right: 20px;
}
</style>
/*% } %*/
