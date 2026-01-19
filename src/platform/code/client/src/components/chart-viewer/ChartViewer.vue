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
              <v-col cols="12" md="6">
                <v-select
                  v-model="selectedChart"
                  :items="charts"
                  item-text="label"
                  item-value="value"
                  :label="$t('chartViewer.selectChart')"
                  @change="loadChart"
                />
              </v-col>
            </v-row>

            <v-row justify="center" v-if="loading">
              <v-progress-circular indeterminate />
            </v-row>

            <v-row v-if="spec">
              <v-col cols="12">
                <div ref="vegaContainer" />
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

const context = require.context("./charts", false, /\.json$/);

const charts = context.keys().map(file => {
  const name = file.replace("./", "").replace(".json", "");
  return {
    label: name,
    value: file
  };
});

export default {
  name: "ChartViewer",

  data() {
    return {
      charts,
      selectedChart: null,
      loading: false,
      spec: null
    };
  },

  mounted() {
    if (this.charts.length) {
      this.selectedChart = this.charts[0].value;
      this.loadChart();
    }
  },

  methods: {
    async loadChart() {
      if (!this.selectedChart) return;

      this.loading = true;
      this.spec = null;

      try {
        this.spec = context(this.selectedChart);

        this.$nextTick(async () => {
          this.$refs.vegaContainer.innerHTML = "";
          await embed(this.$refs.vegaContainer, this.spec, { actions: false });
        });
      } catch (err) {
        console.error("Error loading Vega chart:", err);
      } finally {
        this.loading = false;
      }
    },
    goBack() {
      this.$router.push({
        name: "MapViewer",
        params: { 
          backAction: true 
        }
      });
    }
  }
};
</script>
/*% } %*/