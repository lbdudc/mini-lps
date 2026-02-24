/*% if (feature.ChartViewer) { %*/
<template>
  <v-container fluid class="chart-viewer-container">
    <v-row justify="center" no-gutters>
      <v-col cols="12">
        <v-card class="chart-card">

          <v-card-title>
            <v-row align="center" class="w-100">
              <v-col cols="auto">
                <v-btn icon @click="goBack">
                  <v-icon>mdi-arrow-left</v-icon>
                </v-btn>
              </v-col>

              <v-col class="text-center">
                <h3>{{ $t('chartViewer.title') }}</h3>
              </v-col>

              <v-col cols="auto" />
            </v-row>
          </v-card-title>

          <v-divider />

          <v-card-text>
            <v-row justify="center" class="mb-4">
              <v-col cols="12" md="4">
                <v-select
                  v-model="selectedChart"
                  :items="charts"
                  item-text="label"
                  item-value="value"
                  :label="$t('chartViewer.chartCustom')"  
                  @change="selectChart"
                />
              </v-col>
              <v-col cols="12" md="4">
                <v-select
                  v-model="selectedEntity"
                  :items="entities"
                  :label="$t('chartViewer.chartEntity')"
                  @change="selectEntity"
                />
              </v-col>

              <v-col cols="12" md="4" v-if="fields.length && useEntityTemplateChart">
                <v-select
                  v-model="selectedX"
                  :items="fields"
                  :label="$t('chartViewer.axisX')"
                  @change="renderChart"
                />
              </v-col>
              <v-col cols="12" md="4" v-if="fields.length && useEntityTemplateChart">
                <v-select
                  v-model="selectedY"
                  :items="fields"
                  :label="$t('chartViewer.axisY')"
                  @change="renderChart"
                />
              </v-col>
            </v-row>

            <v-row justify="center" v-if="loading">
              <v-progress-circular indeterminate color="primary" />
            </v-row>

           <v-row v-show="!loading && spec">
              <v-col cols="12">
                <div class="vega-scroll-container">
                  <div ref="vegaContainer" />
                </div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import embed from "vega-embed";
import layers from "../map-viewer/config-files/layers.json";
import genericChart from "./entityTemplateChart.json";

let context = null;
let charts = [];

try {
  context = require.context("./charts", false, /\.json$/);
  charts = context.keys().map(file => ({
    label: file.replace("./", "").replace(".json", ""),
    value: file
  }));
} catch (err) {
  console.warn("No charts folder found.");
}

export default {
  name: "ChartViewer",

  data() {
    return {
      charts,
      selectedChart: null,
      entities: [],
      selectedEntity: null,
      useEntityTemplateChart: false,
      loading: false,
      spec: null,
      fields: [], // TSV headers
      selectedX: null,
      selectedY: null
    };
  },
  mounted() {
    this.entities = layers.layers
      .filter(layer => layer.list != null)
      .map(layer => layer.list);
  },
  methods: {
    selectChart() {
      this.useEntityTemplateChart = false;
      this.selectedEntity = null;
      this.loadChart();
    },
    selectEntity() {
      this.useEntityTemplateChart = true;
      this.selectedChart = null;
      this.loadChart();
    },
    async loadChart() {
      this.loading = true;

      try {
        let baseSpec;

        if (this.useEntityTemplateChart) {
          baseSpec = genericChart;
        } else {
          if (!this.selectedChart) return;
          baseSpec = context(this.selectedChart);
        }

        let specString = JSON.stringify(baseSpec);

        if (this.useEntityTemplateChart && this.selectedEntity) {
          const entityName = this.selectedEntity.toLowerCase() + "s";
          specString = specString.replace(/__ENTITY__/g, entityName);
        }

        const finalSpec = JSON.parse(specString);
        const tsvUrl = finalSpec.data[0].url;
        const response = await fetch(tsvUrl);
        const text = await response.text();
        const firstLine = text.split('\n')[0];
        this.fields = firstLine.split('\t').map(f => f.trim());

        this.selectedX = this.fields[0];
        this.selectedY = this.fields[1] || this.fields[0];

        this.spec = finalSpec;
        this.renderChart();
      } catch (err) {
        console.error("Chart error:", err);
      } finally {
        this.loading = false;
      }
    },

    renderChart() {
      this.$nextTick(async () => {
        if (!this.$refs.vegaContainer || !this.spec) return;

        let specString = JSON.stringify(this.spec);
        specString = specString.replace(/__XFIELD__/g, this.selectedX);
        specString = specString.replace(/__YFIELD__/g, this.selectedY);

        const finalSpec = JSON.parse(specString);

        try {
          await embed(this.$refs.vegaContainer, finalSpec, {
            actions: false,
            renderer: 'svg',
            width: Math.max(this.$refs.vegaContainer.offsetWidth, 800)
          });
        } catch (e) {
          console.error("Vega Embed Error:", e);
        }
      });
    },

    goBack() {
      this.$router.push({ name: "MapViewer", params: { backAction: true } });
    }
  }
};
</script>
<style scoped>
.chart-viewer-container {
  min-height: 100vh;
  padding: 0;
}

.chart-card {
  height: 100%;
}

.vega-scroll-container {
  overflow-x: auto;
  width: 100%;
}

.vega-scroll-container > div {
  min-width: 800px;
}
</style>
/*% } %*/