/*% if (feature.MV_Processes) { %*/
<template>
  <v-card class="card" min-height="400" height="auto">
    <v-card-title class="primary white--text">
      <v-row align="center" no-gutters>
        <v-col>
          <span>
            {{ $t("toolbox.title") }}
          </span>
        </v-col>
        <v-col class="text-right">
          <v-btn @click="close" color="white" icon>
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-col>
      </v-row>
    </v-card-title>

    <v-card-text class="d-flex flex-column">
      <v-row no-gutters align="center" justify="center" class="flex-grow-0">
        <v-radio-group v-if="!loading" v-model="radios" row mandatory>
          <div v-for="option in processOptions" :key="option.value">
            <v-radio
              :value="option.value"
              :label="$t(option.label)"
            />
          </div>
        </v-radio-group>
      </v-row>

      <v-divider class="mb-4"></v-divider>

      <v-row no-gutters class="flex-grow-1">
        <loading-spinner
          v-if="loading"
          :message="'toolbox.loadEnvironment'"
        />

        <component
          v-else-if="!loading && (processingMap || !currentRadio.dependsOnMap)"
          :is="radios"
          :map="map"
          :process-map="processingMap"
          :jobs="jobs"
          :new-job="newJob"
          @show-jobs="viewJobs"
          @delete-job="deleteJob"
          @close="close"
        />

        <div v-else class="load-error">
          <span>{{ $t("toolbox.loadError") }}</span>
        </div>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script>
import ProcessForm from "@/components/map-viewer/toolbox/processes/ProcessForm.vue";
import ProcessJobs from "@/components/map-viewer/toolbox/processes/ProcessJobs.vue";

import { handleRequest, retrieveURL } from "@/common/proxy";
import properties from "@/properties";
import LoadingSpinner from "@/components/loading-page/LoadingSpinner.vue";

export default {
  name: "SearchCatalog",
  components: {
    LoadingSpinner,
    ProcessForm,
    ProcessJobs,
  },

  props: {
    map: {
      type: Object,
      required: true,
    },
    customMap: {
      type: Object,
      default: null,
    },
  },

  data() {
    return {
      loading: false,
      radios: "ProcessForm",
      processingMap: null,
      jobs: [],
      newJob: null,

      processOptions: [
        {
          value: "ProcessForm",
          label: "toolbox.jobForm",
          admin: false,
          dependsOnMap: true,
        },
        {
          value: "ProcessJobs",
          label: "toolbox.jobStatus",
          admin: false,
          dependsOnMap: true,
        },
      ],
    };
  },

  computed: {
    currentRadio() {
      return this.radios
        ? this.processOptions.find((o) => o.value === this.radios)
        : this.processOptions[0];
    },
  },

  created() {
    this.importEnvironment();
  },

  methods: {
    close() {
      this.destroyEnvironment();
      this.$emit("close");
      this.$destroy();
    },

    viewJobs(job) {
      if (job) this.jobs.push(job);
      this.radios = "ProcessJobs";
    },

    deleteJob(job) {
      (job.layerIds || []).forEach((id) => {
        if (this.map.getLayer(id)) this.map.removeLayer(id);
      });
      this.jobs = this.jobs.filter((j) => j.jobID !== job.jobID);
    },

    importEnvironment() {
      if (!this.map || typeof this.map.exportState !== "function") {
        console.error("El objeto mapa no está listo o no tiene exportState");
        this.loading = false;
        return;
      }

      this.loading = true;

      const mapState = this.map.exportState();

      /*
       * Map.exportState() silently drops every layer whose export throws — and
       * that is every GeoServer WMS layer here: its styles are plain wrappers
       * without an exportState() (see _wrapWMSStyle). The processing service
       * would then get a map with no layers at all and offer no inputs for a
       * model. It only needs what identifies a layer, so describe the dropped
       * ones directly, leaving the styles out.
       */
      const exportedIds = mapState.layers.map((l) => l.options.id);
      this.map
        .getLayers()
        .filter((layer) => !exportedIds.includes(layer.getId()))
        .forEach((layer) => {
          mapState.layers.push({
            options: {
              id: layer.getId(),
              type: layer.getType(),
              opacity: layer.getOpacity(),
              selected: layer.isSelected(),
              baseLayer: layer.isBaseLayer(),
              url: layer.options.url,
              params: layer.options.params,
              label: layer.getLabel(),
            },
          });
        });

      /* only the app's own GeoServer layers can be fetched by the service */
      mapState.layers = mapState.layers.filter(
        (layer) =>
          layer.options.type !== "WMS" ||
          !layer.options.url ||
          layer.options.url.startsWith(properties.GEOSERVER_URL)
      );

      // Update layers url
      mapState.layers = mapState.layers.map((layer) => ({
        ...layer,
        options: {
          ...layer.options,
          url: retrieveURL(layer.options.url),
        },
      }));

      // Set dimension filters
      mapState.dimensionFilters = this.map.dimensionFilters || null;

      // Set inputs
      const inputs = { MAP_STATE: JSON.stringify(mapState) };

      if (this.customMap) {
        inputs.MAP_ID = this.customMap.customId;
      }

      handleRequest(
        `${properties.QGIS_URL}/processes/common:importmapstate/execution`,
        {
          method: "POST",
          headers: {
            "Content-type": "application/json",
          },
          body: JSON.stringify({ inputs }),
        }
      )
        .then((response) => response.json())
        .then((data) => (this.processingMap = data.REF_ID))
        .finally(() => (this.loading = false));
    },

    destroyEnvironment() {
      if (!this.processingMap) return;

      handleRequest(
        `${properties.QGIS_URL}/processes/common:deletemapstate/execution`,
        {
          method: "POST",
          headers: {
            "Content-type": "application/json",
          },
          body: JSON.stringify({
            inputs: { MAP_ID: this.processingMap },
          }),
        }
      );
    },
  },
};
</script>

<style>
.card {
  max-height: 85vh;
  overflow-y: auto;
}

.load-error {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
/*% } %*/