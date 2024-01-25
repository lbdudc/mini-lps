/*% if (feature.MV_LM_BaseLayerSelector) { %*/
<template>
  <v-card class="card">
    <v-card-title primary-title class="headline card-title white--text">
      <v-container class="pa-0 ma-0">
        <v-row no-gutters>
          <v-col class="ma-0">
            {{ $t("changeBaseLayer.changeBaseLayerTitle") }}
          </v-col>
          <v-col cols="1">
            <v-btn @click="close" color="white" icon
            ><v-icon>mdi-close</v-icon></v-btn
            >
          </v-col>
        </v-row>
      </v-container>
    </v-card-title>

    <v-card-text class="card-text">
      <v-select
        class="mt-4"
        outlined
        :items="items"
        item-text="name"
        item-value="name"
        v-model="selectedBaseLayer"
        :label="$t('changeBaseLayer.selectBaseLayer')"
        @change="changeBaseLayer"
        :menu-props="{ offsetY: true }"
      >
      </v-select>
    </v-card-text>
  </v-card>
</template>

<script>
import layerList from "@/components/map-viewer/config-files/layers.json";
import { TileLayer } from "@lbdudc/map-viewer";
export default {
  name: "changeBaseLayerControl",
  props: ["map"],
  data() {
    return {
      items: [],
      mapSelected: null,
      selectedBaseLayer: null,
    };
  },
  mounted() {
    this.items = layerList.layers.filter((e) => e.layerType == "tilelayer");
  },

  methods: {
    changeBaseLayer() {
      const baseLayers = this.map.getBaseLayers();
      const selectedLayer = layerList.layers.find(
        (e) => e.name == this.selectedBaseLayer
      );

      const layerOptions = {
        id: selectedLayer.name,
        label: selectedLayer.name,
        type: selectedLayer.layerType,
        baseLayer: true,
        opacity: 0.5,
        selected: true,
        url: selectedLayer.url,
      };

      const addedLayer = baseLayers.find(
        (layer) => layer.options.id == layerOptions.id
      );

      if (!addedLayer) {
        this.map.addLayer(new TileLayer(layerOptions));
      } else {
        this.map.setBaseLayer(addedLayer.options.id);
      }
      this.close();
    },

    close() {
      this.$emit("close");
      this.$destroy();
    },
  },
};
</script>

<style scoped>
.card-title {
  background-color: #1976d2;
}
</style>
/*% } %*/
