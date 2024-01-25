/*% if (feature.MV_T_ViewMapAsList) { %*/
<template>
  <v-card>
    <v-card-title primary-title class="headline primary white--text">
      {{ $t("mapViewer.listLayerElements") }}
    </v-card-title>
    <v-card-text>
      <v-row>
        <v-col class="text-left">
          <v-select
            :items="lists"
            @change="showList"
            :label="$t('mapViewer.chooseEntity')"
            :menu-props="{ offsetY: true }"
          >
          </v-select>
        </v-col>
      </v-row>
      <v-col class="text-right">
        <v-btn @click="close">{{ $t("mapViewer.close") }}</v-btn>
      </v-col>
    </v-card-text>
  </v-card>
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
      this.$router.push({ name: e });
    },
    close() {
      this.$emit("close");
      this.$destroy();
    }
  }
};
</script>
/*% } %*/
