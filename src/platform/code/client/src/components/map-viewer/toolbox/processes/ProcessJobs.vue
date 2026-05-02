/*% if (feature.MV_Processes) { %*/
<template>
  <v-container>
    <v-data-table
      dense
      show-expand
      item-key="jobID"
      :headers="headers"
      :items="filteredJobs"
      :sort-by.sync="sortBy"
      :sort-desc.sync="sortDesc"
      :expanded.sync="expanded"
      :no-data-text="$t('toolbox.jobs.noJobs')"
      @item-expanded="(val) => getJobState(val.item)"
    >
      <template v-slot:[`item.processID`]="{ item }">
        <span>{{ translateProcessId(item) }}</span>
      </template>

      <template v-slot:[`item.created`]="{ item }">
        <span>
          {{ utcDateToLocalCalendarString(item.created) }}
        </span>
      </template>

      <template v-slot:[`item.done`]="{ item }">
        <v-icon
          v-if="item.result && item.result.progress === 100"
          color="success"
        >
          mdi-check
        </v-icon>
        <v-icon
          v-if="item.result && item.result.status === 'failed'"
          color="error"
        >
          mdi-close
        </v-icon>
      </template>

      <template v-slot:expanded-item="{ headers, item }">
        <td :colspan="headers.length">
          <loading-page v-if="item.loading"></loading-page>

          <v-container v-else-if="item.result">
            <v-row no-gutters>
              <v-col class="text-right">
                <v-tooltip top open-delay="200">
                  <template v-slot:activator="{ on }">
                    <v-btn
                      icon
                      color="primary"
                      v-on="on"
                      :disabled="item.result.progress < 100"
                      @click="processResult(item)"
                    >
                      <v-icon>mdi-eye</v-icon>
                    </v-btn>
                  </template>
                  <span>{{ $t("toolbox.jobs.viewResult") }}</span>
                </v-tooltip>

                <v-tooltip top open-delay="200">
                  <template v-slot:activator="{ on }">
                    <v-btn
                      icon
                      color="error"
                      v-on="on"
                      @click="deleteResult(item, true)"
                    >
                      <v-icon>delete</v-icon>
                    </v-btn>
                  </template>
                  <span>{{ $t("toolbox.jobs.deleteJob") }}</span>
                </v-tooltip>
              </v-col>
            </v-row>

            <v-row v-if="item.result" class="pa-3" no-gutters>
              <v-col cols="3" md="2" class="text-left font-weight-bold">
                {{ $t("toolbox.jobs.message") }}:
              </v-col>
              <v-col cols="9" md="4">
                {{ item.result.message }}
              </v-col>

              <v-col cols="3" md="2" class="text-left font-weight-bold">
                {{ $t("toolbox.jobs.status") }}:
              </v-col>
              <v-col cols="9" md="4">
                {{ item.result.status }}
              </v-col>

              <v-col cols="3" md="2" class="text-left font-weight-bold">
                {{ $t("toolbox.jobs.started") }}:
              </v-col>
              <v-col cols="9" md="4">
                {{ utcDateToLocalCalendarString(item.result.started) }}
              </v-col>

              <v-col cols="3" md="2" class="text-left font-weight-bold">
                {{ $t("toolbox.jobs.finished") }}:
              </v-col>
              <v-col cols="9" md="4">
                {{
                  item.result.finished
                    ? utcDateToLocalCalendarString(item.result.finished)
                    : "N/A"
                }}
              </v-col>

              <v-col cols="3" md="2" class="text-left font-weight-bold">
                {{ $t("toolbox.jobs.progress") }}:
              </v-col>
              <v-col cols="9" md="4">
                {{ item.result.progress }}
              </v-col>
            </v-row>
          </v-container>
        </td>
      </template>
    </v-data-table>

    <delete-dialog
      @cancel="deleteDialog = false"
      @submit="deleteResult(selectedJob)"
      :dialog="deleteDialog"
      :title="$t('toolbox.jobs.deleteJob')"
      :content="$t('toolbox.jobs.deleteDialog')"
    />
  </v-container>
</template>

<script>
import LoadingPage from "@/components/loading-page/LoadingPage.vue";
import DeleteDialog from "@/components/modal_dialog/DeleteDialog.vue";
import { utcDateToLocalCalendarString } from "@/common/conversion-utils";
import { handleRequest } from "@/common/proxy";
import handleResult from "@/components/map-viewer/common/qgis-result-common";
import properties from "@/properties";

export default {
  name: "ProcessesHistory",
  components: { LoadingPage, DeleteDialog },

  props: {
    map: {
      type: Object,
      required: true,
    },
    newJob: {
      type: Object,
      default: null,
    },
    jobs: {
      type: Array,
      default: () => [],
    },
  },

  data() {
    return {
      intervalId: null,
      sortBy: "created",
      sortDesc: true,
      expanded: [],
      selectedJob: null,
      deleteDialog: false,
      loadingLayer: false,
    };
  },

  computed: {
    headers() {
      return [
        { text: this.$t("toolbox.jobs.processId"), value: "processID" },
        { text: this.$t("toolbox.jobs.created"), value: "created" },
        { text: "", sortable: false, value: "done" },
      ];
    },

    filteredJobs() {
      return this.jobs.filter((job) => job.hidden !== true);
    },
  },

  created() {
    if (this.newJob) this.expanded.push(this.newJob);

    this.reloadJobsStatus();
    this.intervalId = setInterval(this.reloadJobsStatus, 5000);
  },

  beforeDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
  },

  methods: {
    close() {
      this.$emit("close");
      this.$destroy();
    },

    reloadJobsStatus() {
      this.expanded.forEach((item) => {
        if (
          !item.result ||
          (item.result.progress < 100 && item.result.status !== "failed")
        ) {
          this.getJobState(item);
        }
      });
    },

    getJobState(item) {
      if (!item.result || item?.result.progress < 100) {
        this.$set(item, "loading", true);

        handleRequest(`${properties.QGIS_URL}/jobs`, {
          headers: {
            "X-Job-Realm": item.jobRealm,
            "Content-type": "application/json",
          },
        })
          .then((response) => response.json())
          .then((data) => (item.result = data.jobs[0]))
          .finally(() => this.$set(item, "loading", false));
      }
    },

    deleteResult(item, validate = false) {
      if (validate && this.map.getLayer(item.jobID)) {
        this.deleteDialog = true;
        this.selectedJob = item;
      } else {
        this.deleteDialog = false;
        this.selectedJob = null;

        this.$emit("delete-job", item);
      }
    },

    processResult(item) {
      handleRequest(`${properties.QGIS_URL}/jobs/${item.jobID}/results`, {
        headers: {
          "X-Job-Realm": item.jobRealm,
          "Content-type": "application/json",
        },
      })
        .then((response) => response.json())
        .then(async (result) => {
          const params = { jobID: result.JOB_ID };
          const layer = await handleResult(params, result, this.map);

          if (layer) {
            this.loadingLayer = true;
            this.map.addLayer(layer);

            this.loadingLayer = false;
          }
        });
    },

    translateProcessId(item) {
      const [provider, id] = item.processID.split(":");
      return provider !== "model"
        ? this.$t(`process.${id}.title`)
        : id;
    },

    utcDateToLocalCalendarString,
  },
};
</script>
/*% } %*/