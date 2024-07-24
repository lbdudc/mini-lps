/*% if (feature.MV_T_ViewMapAsList) { %*/
/*%
var cols = 12;
var md = 6;
if(feature.MV_T_ViewMapAsList && feature.MV_MM_MMV_MapSelectorInMapViewer){
  cols = 6;
  md = 3;
}
%*/
<template>
  <v-col cols="/*%= cols %*/" md="/*%= md %*/">
    <v-row class="justify-end" no-gutters>
      <v-col cols="12" sm="5" class="mt-4 mr-4 text-center text-sm-right">
        <label>
          {{ $t("mapViewer.listSelector") }}
        </label>
      </v-col>
      <v-col cols="12" sm="6">
      <v-select
        :items="lists"
        @change="showList"
        :label="$t('mapViewer.chooseEntity')"
        :menu-props="{ offsetY: true }"
        solo
      >
      </v-select>
      </v-col>
    </v-row>
  </v-col>
</template>

<script>
import layers from "../config-files/layers.json";

export default {
  name: "ListLayerManagement",
  props: ["map"],
  data() {
    return {
      lists: []
    };
  },
  mounted() {
    this.lists = layers.layers
      .filter((layer) =>
        this.map
          .getVisibleOverlays()
          .find(
            (mapLayer) =>
              layer.list != null && layer.name === mapLayer.options.id
          )
      )
      .map((layer) => layer.list);
  },
  methods: {
    showList(e) {
      this.$router.push({ name: e + " List"});
    },
    close() {
      this.$emit("close");
      this.$destroy();
    }
  }
};
</script>
/*% } %*/
