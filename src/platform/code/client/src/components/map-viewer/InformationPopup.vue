/*% if (feature.MV_DetailOnClick) { %*/
<template>
  <v-container v-if="layer" fluid>
    <v-col cols="12" class="px-0 py-0">
      <span v-if="form" class="link" @click="goToDetail">
        <v-icon>remove_red_eye</v-icon>
        {{ layer.feature.properties.displayString }}
      </span>
      <span v-else>
        {{ layer.feature.properties.displayString }}
      </span>
    </v-col>
  </v-container>
</template>

<script>
export default {
  name: "InformationPopup",
  props: {
    form: {
      mandatory: true,
    },
    layer: {
      mandatory: true,
    },
  },
  updated() {
    if (this.layer) this.layer.updatePopup();
  },
  methods: {
    goToDetail() {
      this.$router.push({
        name: this.form + "Detail",
        params: { id: this.layer.feature.id, backPrevious: true },
      });
    },
  },
};
</script>

<style scoped>
.link {
  font-size: 15px;
  cursor: pointer;
  display: inline-flex;
}
</style>
<style>
.leaflet-popup-content {
  width: auto !important;
}
</style>
/*% } %*/
