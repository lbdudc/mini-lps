/*% if (feature.MV_T_ViewMapAsList) { %*/
<template>
  <v-select
    :items="lists"
    @change="showList"
    :label="$t('mapViewer.chooseEntity')"
    :menu-props="{ offsetY: true }"
    prepend-inner-icon="mdi-format-list-bulleted"
    dense
    hide-details
    outlined
    rounded
    class="map-toolbar-select"
  >
  </v-select>
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
