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

            <v-tabs v-model="activeTab" background-color="transparent">
              <v-tab>{{ $t('chartViewer.tabExplorer') }}</v-tab>
              <v-tab>{{ $t('chartViewer.tabSaved') }}</v-tab>
            </v-tabs>

            <v-tabs-items v-model="activeTab">

              <v-tab-item>
                <v-row justify="center">
                  <v-col cols="12" md="4">
                    <v-select
                      v-model="selectedEntity"
                      :items="entities"
                      :label="$t('chartViewer.chartEntity')"
                      @change="selectEntity"
                    />
                  </v-col>

                  <v-col cols="12" md="4">
                    <v-select
                      v-model="selectedChartType"
                      :items="chartTypes"
                      :label="$t('chartViewer.chartType')"
                      @change="loadChart"
                    />
                  </v-col>
                </v-row>

                <v-row justify="center" v-if="fields.length">
                  <v-col cols="12" md="4">
                    <v-select
                      v-model="selectedX"
                      :items="fields"
                      :label="$t('chartViewer.axisX')"
                      @change="renderChart"
                    />
                  </v-col>

                  <v-col cols="12" md="4">
                    <v-select
                      v-model="selectedY"
                      :items="fields"
                      :label="$t('chartViewer.axisY')"
                      @change="renderChart"
                    />
                  </v-col>

                  <v-col cols="12" md="2" class="d-flex align-center">
                    <v-btn block color="primary" @click="exportChart">
                      <v-icon left>mdi-download</v-icon>
                      {{ $t('chartViewer.exportChart') }}
                    </v-btn>
                  </v-col>
                </v-row>
              </v-tab-item>

              <v-tab-item>
                <v-row justify="center">
                  <v-col cols="12" md="6">
                    <v-select
                      v-model="selectedChart"
                      :items="charts"
                      item-text="label"
                      item-value="value"
                      :label="$t('chartViewer.chartCustom')"
                      @change="loadSavedChart"
                    />
                  </v-col>
                </v-row>
              </v-tab-item>

            </v-tabs-items>

            <v-row justify="center" v-if="loading">
              <v-progress-circular indeterminate color="primary" />
            </v-row>

            <v-row v-show="!loading && spec" justify="center">
              <v-col cols="12" md="10" lg="8">
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

let context = null;
let charts = [];
let templateContext = require.context("./templates", false, /\.json$/);
const defaultChartType = "linechart.json";

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
      activeTab: 0, // 0 = Explorer, 1 = Saved Charts
      charts,
      selectedChart: null,
      entities: [],
      selectedEntity: null,
      useEntityTemplateChart: true,
      loading: false,
      spec: null,
      fields: [], // TSV headers
      selectedX: null,
      selectedY: null,
      chartTypes: [
        { text: this.$t('chartViewer.chartTypeLine'), value: "linechart.json" },
        { text: this.$t('chartViewer.chartTypeBar'), value: "barchart.json" },
        { text: this.$t('chartViewer.chartTypePie'), value: "piechart.json" }
      ],
      selectedChartType: defaultChartType,
    };
  },
  watch: {
    activeTab(newTab, oldTab) {
      this.spec = null;      
      this.selectedX = null;
      this.selectedY = null;

      if (newTab === 1) {
        this.useEntityTemplateChart = false;
        this.selectedChart = null;
      } else {
        this.useEntityTemplateChart = true;
        this.selectedChartType = defaultChartType;
        this.selectedEntity = null;
      }
    }
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
      this.loadSavedChart();
    },
    selectEntity() {
      this.useEntityTemplateChart = true;
      this.selectedChart = null;

      this.selectedX = null;
      this.selectedY = null;
      this.fields = [];
      this.spec = null;
      
      this.loadChart();
    },
    async loadChart() {
      if (!this.selectedChartType && this.useEntityTemplateChart) return;
      this.loading = true;

      try {
        let baseSpec = this.useEntityTemplateChart
          ? templateContext(`./${this.selectedChartType}`)
          : context(this.selectedChart);

        let specString = JSON.stringify(baseSpec);

        if (this.useEntityTemplateChart && this.selectedEntity) {
          const entityName = this.selectedEntity.charAt(0).toLowerCase() + this.selectedEntity.slice(1) + "s";
          specString = specString.replace(/__ENTITY__/g, entityName);
        }

        const finalSpec = JSON.parse(specString);

        if (this.useEntityTemplateChart) {
          const tsvUrl = finalSpec.data[0].url;
          const response = await fetch(tsvUrl);
          const text = await response.text();
          const firstLine = text.split('\n')[0];
          this.fields = firstLine.split('\t').map(f => f.trim());

          this.selectedX = this.selectedX || this.fields[0];
          this.selectedY = this.selectedY || this.fields[1] || this.fields[0];
        }

        this.spec = finalSpec;
        this.renderChart();
      } catch (err) {
        console.error("Chart error:", err);
      } finally {
        this.loading = false;
      }
    },

     async loadSavedChart() {
      if (!this.selectedChart) return;
      this.loading = true;

      try {
        const baseSpec = context(this.selectedChart);

        // Only for saved charts: Add extra "s" to entity in URL to handle user-added JSONs that don't include it
        if (baseSpec.data && baseSpec.data[0] && baseSpec.data[0].url) {
          const match = baseSpec.data[0].url.match(/\/api\/entities\/([^/]+)\//);
          if (match) {
            const entityFromUrl = match[1];
            let correctedEntity = entityFromUrl.endsWith("ss") 
              ? entityFromUrl 
              : entityFromUrl + "s";
            baseSpec.data[0].url = baseSpec.data[0].url.replace(
              `/api/entities/${entityFromUrl}/`,
              `/api/entities/${correctedEntity}/`
            );
          }
        }

        this.spec = baseSpec;
        this.renderChart();
      } catch (err) {
        console.error(err);
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
            width: this.$refs.vegaContainer.offsetWidth
          });
        } catch (e) {
          console.error("Vega Embed Error:", e);
        }
      });
    },

    exportChart() {
      if (!this.spec) return;

      let specString = JSON.stringify(this.spec);
      specString = specString.replace(/__XFIELD__/g, this.selectedX);
      specString = specString.replace(/__YFIELD__/g, this.selectedY);

      if (this.useEntityTemplateChart && this.selectedEntity) {
        const entityName = this.selectedEntity.charAt(0).toLowerCase() + this.selectedEntity.slice(1) + "s";
        specString = specString.replace(/__ENTITY__/g, entityName);
      }

      const blob = new Blob([specString], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${this.selectedEntity || 'chart'}_${this.selectedX}_${this.selectedY}.json`;
      a.click();
      URL.revokeObjectURL(url);
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
  width: 100%;
}
</style>
/*% } %*/