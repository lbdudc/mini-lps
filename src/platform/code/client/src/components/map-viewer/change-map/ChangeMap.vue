/*% if (feature.MV_MM_MMV_MapSelectorInMapViewer) { %*/
<template>
  <v-card class="card">
    <v-card-title primary-title class="headline card-title white--text">
      <v-container class="pa-0 ma-0">
        <v-row no-gutters>
          <v-col class="ma-0">
            {{ $t("mapViewer.mapSelector") }}
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
        :items="maps"
        item-value="id"
        item-text="label"
        v-model="selected"
        :label="$t('mapViewer.map')"
        @change="select"
        :menu-props="{ offsetY: true }"
      >
      </v-select>
    </v-card-text>
  </v-card>
</template>

<script>
export default {
  name: "changeMapControl",
  props: ["maps", "mapSelected"],
  data() {
    return {
      selected: this.mapSelected,
    };
  },
  methods: {
    select() {
      this.$emit("change-map", this.selected);
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
