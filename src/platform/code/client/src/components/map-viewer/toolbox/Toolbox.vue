/*% if (feature.MV_Processes) { %*/
<template>
  <v-card
    class="toolbox-card d-flex flex-column"
    :class="{ 'toolbox-card--full': $vuetify.breakpoint.smAndDown }"
  >
    <v-card-title class="primary white--text flex-grow-0">
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

    <v-card-text class="toolbox-content d-flex flex-column">
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

      <v-divider class="mb-4 flex-grow-0"></v-divider>

      <div class="toolbox-body">
        <loading-spinner
          v-if="loading"
          :message="'toolbox.loadEnvironment'"
        />

        <!-- keep-alive: switching tabs must not throw away the form inputs -->
        <keep-alive
          v-else-if="!loading && (processingMap || !currentRadio.dependsOnMap)"
        >
          <component
            :is="radios"
            :map="map"
            :process-map="processingMap"
            :jobs="jobs"
            :new-job="newJob"
            :map-revision="mapRevision"
            @show-jobs="viewJobs"
            @add-to-map="addToMap"
            @remove-from-map="removeFromMap"
            @delete-job="deleteJob"
            @close="close"
          />
        </keep-alive>

        <div v-else class="load-error">
          <span>{{ $t("toolbox.loadError") }}</span>
        </div>
      </div>
    </v-card-text>
  </v-card>
</template>

<script>
import ProcessForm from "@/components/map-viewer/toolbox/processes/ProcessForm.vue";
import ProcessJobs from "@/components/map-viewer/toolbox/processes/ProcessJobs.vue";

import { handleRequest, retrieveURL } from "@/common/proxy";
import handleResult from "@/components/map-viewer/common/qgis-result-common";
import {
  loadJobs,
  saveJobs,
  loadPendingCleanup,
  savePendingCleanup,
  storageKey,
  isFinalStatus,
} from "@/components/map-viewer/toolbox/processes/utils/job-store";
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
      jobs: loadJobs(),
      newJob: null,
      mapRevision: 0,
      pollIntervalId: null,
      polling: false,
      skipSave: false,

      processOptions: [
        {
          value: "ProcessForm",
          label: "toolbox.jobForm",
          admin: false,
          dependsOnMap: true,
        },
        {
          value: "ProcessJobs",
          label: "toolbox.jobHistory",
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

  watch: {
    jobs: {
      deep: true,
      handler() {
        /* a list just read from the storage must not be written straight back */
        if (this.skipSave) {
          this.skipSave = false;
          return;
        }
        saveJobs(this.jobs);
      },
    },
  },

  created() {
    this.importEnvironment();

    window.addEventListener("storage", this.onStorage);
    /* the server may have dropped jobs since the last visit: check them all once */
    this.pollJobs(true);
    this.pollIntervalId = setInterval(this.pollJobs, 5000);
  },

  beforeDestroy() {
    clearInterval(this.pollIntervalId);
    window.removeEventListener("storage", this.onStorage);

    /*
     * Runs however the dialog was closed (button or click outside). A queued job
     * still needs its processing project, so then the deletion waits for it.
     */
    if (!this.processingMap) return;
    if (this.hasUnfinishedJobs(this.processingMap)) {
      savePendingCleanup([
        ...new Set([...loadPendingCleanup(), this.processingMap]),
      ]);
    } else {
      this.destroyEnvironment(this.processingMap);
    }
  },

  methods: {
    close() {
      this.$emit("close");
    },

    onStorage(event) {
      if (event.key !== storageKey()) return;
      this.skipSave = true;
      this.jobs = loadJobs();
    },

    hasUnfinishedJobs(processingMap) {
      return this.jobs.some(
        (job) =>
          job.processingMap === processingMap && !isFinalStatus(job.status)
      );
    },

    viewJobs(job) {
      if (job) {
        this.jobs.push({
          ...job,
          processingMap: this.processingMap,
          mapName: this.$route?.params?.id || null,
          status: job.status || "accepted",
        });
        /* the reactive copy, so the jobs table can expand it */
        this.newJob = this.jobs[this.jobs.length - 1];
        this.pollJobs();
      }
      this.radios = "ProcessJobs";
    },

    jobRequestOptions(job, extra = {}) {
      return {
        ...extra,
        headers: {
          "X-Job-Realm": job.jobRealm,
          "Content-type": "application/json",
        },
      };
    },

    /* checkAll: also re-check finished jobs, the server may have dropped them */
    async pollJobs(checkAll = false) {
      if (this.polling) return;
      this.polling = true;
      try {
        await Promise.all(
          this.jobs
            .filter(
              (job) =>
                job.status !== "expired" &&
                (checkAll === true || !isFinalStatus(job.status))
            )
            .map((job) => this.refreshJob(job))
        );
        this.cleanupEnvironments();
      } finally {
        this.polling = false;
      }
    },

    async refreshJob(job) {
      try {
        const response = await handleRequest(
          `${properties.QGIS_URL}/jobs/${job.jobID}`,
          this.jobRequestOptions(job)
        );

        /* the server no longer knows this job (expired, or its storage was reset) */
        if (response.status === 404) {
          this.$set(job, "status", "expired");
          return;
        }
        if (!response.ok) return;

        const data = await response.json();
        const status =
          data.status || (data.progress === 100 ? "successful" : "running");
        this.$set(job, "status", status);
        ["progress", "message", "started", "finished", "expire"].forEach(
          (field) => {
            if (data[field] !== undefined) this.$set(job, field, data[field]);
          }
        );
      } catch (e) {
        /* the server can't be reached right now: try again on the next poll */
      }
    },

    /* deletes the processing projects whose deletion was waiting for a job */
    cleanupEnvironments() {
      const pending = loadPendingCleanup();
      if (pending.length === 0) return;

      const stillNeeded = pending.filter(
        (id) => id === this.processingMap || this.hasUnfinishedJobs(id)
      );
      pending
        .filter((id) => !stillNeeded.includes(id))
        .forEach((id) => this.destroyEnvironment(id));
      savePendingCleanup(stillNeeded);
    },

    removeFromMap(job) {
      (job.layerIds || []).forEach((id) => {
        if (this.map.getLayer(id)) this.map.removeLayer(id);
      });
      this.mapRevision++;
    },

    async addToMap(job) {
      /* adding it twice would duplicate its layers */
      this.removeFromMap(job);

      try {
        const response = await handleRequest(
          `${properties.QGIS_URL}/jobs/${job.jobID}/results`,
          this.jobRequestOptions(job)
        );
        if (response.status === 404) {
          this.$set(job, "status", "expired");
          return;
        }

        const result = await response.json();
        const layers = await handleResult(
          { jobID: job.jobID },
          result,
          this.map
        );

        const layerIds = [];
        layers.forEach((layer) => {
          this.map.addLayer(layer);
          layerIds.push(layer.options.id);
        });
        this.$set(job, "layerIds", layerIds);
      } catch (e) {
        this.$notify({
          title: this.$t("toolbox.jobs.resultError.title"),
          text: this.$t("toolbox.jobs.resultError.text"),
          type: "error",
        });
      } finally {
        this.mapRevision++;
      }
    },

    deleteJob(job) {
      this.removeFromMap(job);
      this.jobs = this.jobs.filter((j) => j.jobID !== job.jobID);

      /* also drop it from the server; whether that is supported doesn't matter here */
      handleRequest(
        `${properties.QGIS_URL}/jobs/${job.jobID}`,
        this.jobRequestOptions(job, { method: "DELETE" })
      ).catch(() => {});
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
              /* a raster is fetched by the service as a coverage, not as features */
              raster: layer.options.raster === true,
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

    destroyEnvironment(processingMap) {
      if (!processingMap) return;

      handleRequest(
        `${properties.QGIS_URL}/processes/common:deletemapstate/execution`,
        {
          method: "POST",
          headers: {
            "Content-type": "application/json",
          },
          body: JSON.stringify({
            inputs: { MAP_ID: processingMap },
          }),
        }
      );
    },
  },
};
</script>

<style scoped>
/* the header and the tabs stay put; only the body of each tab scrolls */
.toolbox-card {
  height: 85vh;
  max-height: 820px;
}

.toolbox-card--full {
  height: 100%;
  max-height: none;
}

/* beats the "scrollable dialog" rule that makes the whole text area scroll */
.toolbox-card .toolbox-content.v-card__text {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.toolbox-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.load-error {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
/*% } %*/
