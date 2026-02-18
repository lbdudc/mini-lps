/*% if (feature.ChartViewer) { %*/
<template>
  <v-container>
    <v-row justify="center">
      <v-col cols="12" md="10">
        <v-card>

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
                  :label="$t('chartViewer.selectChart')"
                  @change="loadChart"
                />
              </v-col>

              <v-col cols="12" md="4" v-if="fields.length">
                <v-select
                  v-model="selectedX"
                  :items="fields"
                  label="Eje X (Categoría)"
                  @change="renderChart"
                />
              </v-col>
              <v-col cols="12" md="4" v-if="fields.length">
                <v-select
                  v-model="selectedY"
                  :items="fields"
                  label="Eje Y (Valor)"
                  @change="renderChart"
                />
              </v-col>
            </v-row>

            <v-row justify="center" v-if="loading">
              <v-progress-circular indeterminate color="primary" />
            </v-row>

            <v-row v-show="!loading && spec">
              <v-col cols="12">
                <div ref="vegaContainer" class="d-flex justify-center" />
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

let context = null;
let charts = [];

try {
  context = require.context("./charts", false, /\.json$/);
  charts = context.keys().map(file => ({
    label: file.replace("./", "").replace(".json", ""),
    value: file
  }));
} catch (err) {
  console.warn("No charts folder found, skipping chart loading.");
}

export default {
  name: "ChartViewer",
  
  data() {
    return {
      charts,
      selectedChart: charts.length ? charts[0].value : null,
      loading: false,
      spec: null,
      fields: [], // TSV headers
      selectedX: null,
      selectedY: null
    };
  },
  mounted() {
    if (this.selectedChart) this.loadChart();
  },
  methods: {
    async loadChart() {
      if (!this.selectedChart) return;
      this.loading = true;
      
      try {
        this.spec = context(this.selectedChart);

        const tsvUrl = this.spec.data[0].url;
        const response = await fetch(tsvUrl);
        const text = await response.text();
        const firstLine = text.split('\n')[0];
        this.fields = firstLine.split('\t').map(f => f.trim());

        this.selectedX = this.fields[0];
        this.selectedY = this.fields[1] || this.fields[0];

        this.renderChart();
      } catch (err) {
        console.error("Error initializing chart:", err);
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
            width: this.$refs.vegaContainer.offsetWidth * 0.8
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
/*% } %*/