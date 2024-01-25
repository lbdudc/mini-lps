/*% if (feature.MV_LM_StylePreview) { %*/
<template>
  <v-card v-if="wmsLegend">
    <v-card-title primary-title class="headline primary white--text">
      <v-container class="pa-0 ma-0">
        <v-row no-gutters>
          <v-col class="ma-0"> {{ $t("mapViewer.wmsLegend.title") }} </v-col>
          <v-col cols="1">
            <v-btn @click="close" color="white" icon
            ><v-icon>mdi-close</v-icon></v-btn
            >
          </v-col>
        </v-row>
      </v-container>
    </v-card-title>
    <v-card-text class="card-text">
      <v-container>
        <v-row justify="center" class="mb-6">
          <h2>{{ $t(`mapViewer.layer-label.${wmsLegend.id}`) }}</h2>
        </v-row>
        <v-row align="center" justify="center" class="mt-8">
          <div v-if="wmsLegendImage">
            <v-img
              max-height="400"
              max-width="400"
              contain
              :src="wmsLegendImage"
            ></v-img>
          </div>
          <div v-else-if="geoJSONStyle" :style="geoJSONStyle"></div>
          <div v-else>
            {{ $t("mapViewer.wmsLegend.no-data") }}
          </div>
        </v-row>
      </v-container>
    </v-card-text>
  </v-card>
</template>

<script>
import { reactive } from "vue";

export default {
  props: {
    wmsLegend: {
      type: Object,
      required: true,
    },
    map: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      wmsLegendImage: null,
      geoJSONStyle: null,
    };
  },
  async created() {
    const layer = this.map
      .getLayers()
      .find((l) => l.options.id == this.wmsLegend.id);

    if (layer == null) return;

    if (layer.getIconUrl() != null) {
      const [url, params] = layer.getIconUrl().split("?"); // wmsLayer._style ???

      let urlParms = new URLSearchParams(params);
      urlParms.set("HEIGHT", 300);
      urlParms.set("WIDTH", 300);
      urlParms.append("LANGUAGE", this.$i18n.locale);
      urlParms.append(
        "LEGEND_OPTIONS",
        "forceLabels:on;fontAntiAliasing:true;fontSize:20;fontName:Arial;columnheight:350;"
      );

      let blob = await fetch(url, {
        method: "POST",
        body: urlParms,
        headers: {
          "Content-type": "application/x-www-form-urlencoded",
        },
      })
        .then((response) => {
          return response.blob();
        })
        .catch(() => {});

      if (blob.type === "image/png") {
        this.wmsLegendImage = URL.createObjectURL(blob);
      }
    } else {
      let fillColor = layer.getStyle().fillColor;
      let strokeColor = layer.getStyle().strokeColor;

      this.geoJSONStyle = reactive({
        margin: "20px",
        height: "150px",
        width: "150px",
        background: fillColor,
        outline: `5px solid ${strokeColor}`,
      });
    }
  },
  methods: {
    close() {
      this.$emit("close");
      this.$destroy();
    },
  },
};
</script>

<style scoped>
.card-text {
  min-height: 20vh;
}
</style>
/*% } %*/
