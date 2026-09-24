/*% if (feature.MV_Processes) { %*/
<template>
  <v-container fluid class="process-jobs">
    <v-data-table
      dense
      show-expand
      item-key="jobID"
      :headers="headers"
      :items="jobs"
      :sort-by.sync="sortBy"
      :sort-desc.sync="sortDesc"
      :expanded.sync="expanded"
      :no-data-text="$t('toolbox.jobs.noJobs')"
    >
      <template v-slot:[`item.processID`]="{ item }">
        <span>{{ translateProcessId(item) }}</span>
      </template>

      <template v-slot:[`item.created`]="{ item }">
        <span>
          {{ utcDateToLocalCalendarString(item.created) }}
        </span>
      </template>

      <template v-slot:[`item.mapName`]="{ item }">
        <span>{{ mapLabel(item) }}</span>
      </template>

      <template v-slot:[`item.status`]="{ item }">
        <v-chip small label :color="statusColor(item)" :text-color="statusTextColor(item)">
          <v-progress-circular
            v-if="isRunning(item)"
            indeterminate
            :size="12"
            :width="2"
            class="mr-2"
          />
          {{ statusLabel(item) }}
        </v-chip>
      </template>

      <template v-slot:[`item.actions`]="{ item }">
        <v-tooltip v-if="isOnMap(item)" key="remove" top open-delay="200">
          <template v-slot:activator="{ on }">
            <v-btn icon small color="primary" v-on="on" @click="$emit('remove-from-map', item)">
              <v-icon>mdi-layers-remove</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("toolbox.jobs.removeFromMap") }}</span>
        </v-tooltip>

        <v-tooltip v-else key="add" top open-delay="200">
          <template v-slot:activator="{ on }">
            <span v-on="on">
              <v-btn
                icon
                small
                color="primary"
                :disabled="item.status !== 'successful'"
                @click="$emit('add-to-map', item)"
              >
                <v-icon>mdi-layers-plus</v-icon>
              </v-btn>
            </span>
          </template>
          <span>{{ $t("toolbox.jobs.addToMap") }}</span>
        </v-tooltip>

        <v-tooltip key="delete" top open-delay="200">
          <template v-slot:activator="{ on }">
            <v-btn icon small color="error" v-on="on" @click="deleteResult(item, true)">
              <v-icon>mdi-delete</v-icon>
            </v-btn>
          </template>
          <span>{{ $t("toolbox.jobs.deleteFromHistory") }}</span>
        </v-tooltip>
      </template>

      <template v-slot:expanded-item="{ headers, item }">
        <td :colspan="headers.length">
          <v-row class="pa-3" no-gutters>
            <v-col cols="4" md="2" class="text-left font-weight-bold">
              {{ $t("toolbox.jobs.message") }}:
            </v-col>
            <v-col cols="8" md="4">
              {{ item.message || "N/A" }}
            </v-col>

            <v-col cols="4" md="2" class="text-left font-weight-bold">
              {{ $t("toolbox.jobs.progress") }}:
            </v-col>
            <v-col cols="8" md="4">
              {{ item.progress != null ? item.progress + "%" : "N/A" }}
            </v-col>

            <v-col cols="4" md="2" class="text-left font-weight-bold">
              {{ $t("toolbox.jobs.started") }}:
            </v-col>
            <v-col cols="8" md="4">
              {{ dateOrNA(item.started) }}
            </v-col>

            <v-col cols="4" md="2" class="text-left font-weight-bold">
              {{ $t("toolbox.jobs.finished") }}:
            </v-col>
            <v-col cols="8" md="4">
              {{ dateOrNA(item.finished) }}
            </v-col>

            <v-col cols="4" md="2" class="text-left font-weight-bold">
              {{ $t("toolbox.jobs.expires") }}:
            </v-col>
            <v-col cols="8" md="4">
              {{ dateOrNA(item.expire) }}
            </v-col>
          </v-row>
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
import DeleteDialog from "@/components/modal_dialog/DeleteDialog.vue";
import { utcDateToLocalCalendarString } from "@/common/conversion-utils";

/*
 * The history of the jobs run from the toolbox. It only shows them: the toolbox
 * keeps the list, polls the server and adds/removes the result layers.
 */
export default {
  name: "ProcessesHistory",
  components: { DeleteDialog },

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
    /* bumped whenever result layers are added to or removed from the map */
    mapRevision: {
      type: Number,
      default: 0,
    },
  },

  data() {
    return {
      sortBy: "created",
      sortDesc: true,
      expanded: [],
      selectedJob: null,
      deleteDialog: false,
    };
  },

  computed: {
    headers() {
      return [
        { text: this.$t("toolbox.jobs.processId"), value: "processID" },
        { text: this.$t("toolbox.jobs.created"), value: "created" },
        { text: this.$t("toolbox.jobs.map"), value: "mapName" },
        { text: this.$t("toolbox.jobs.status"), value: "status" },
        { text: "", sortable: false, value: "actions", align: "end" },
      ];
    },
  },

  watch: {
    /* the tab is kept alive, so this is what expands the job that was just run */
    newJob(job) {
      if (job) this.expanded = [job];
    },
  },

  created() {
    if (this.newJob) this.expanded = [this.newJob];
  },

  methods: {
    deleteResult(item, validate = false) {
      if (validate && this.isOnMap(item)) {
        this.deleteDialog = true;
        this.selectedJob = item;
      } else {
        this.deleteDialog = false;
        this.selectedJob = null;

        this.$emit("delete-job", item);
      }
    },

    /* the result layers are gone after a reload, so this is read from the map itself */
    isOnMap(job) {
      // eslint-disable-next-line no-unused-expressions
      this.mapRevision;
      return (job.layerIds || []).some((id) => this.map.getLayer(id));
    },

    isRunning(job) {
      return job.status === "running" || job.status === "accepted";
    },

    statusLabel(job) {
      const status = job.status || "accepted";
      const key = `toolbox.jobs.statuses.${status}`;
      const label = this.$te(key) ? this.$t(key) : status;
      return this.isRunning(job) && job.progress != null
        ? `${label} ${job.progress}%`
        : label;
    },

    statusColor(job) {
      return (
        {
          successful: "success",
          failed: "error",
          running: "primary",
          accepted: "primary",
        }[job.status] || "grey lighten-2"
      );
    },

    statusTextColor(job) {
      return ["successful", "failed", "running", "accepted"].includes(job.status)
        ? "white"
        : "grey darken-3";
    },

    mapLabel(job) {
      if (!job.mapName) return "";
      const key = `mapViewer.map-label.${job.mapName.replace(".", "-")}`;
      return this.$te(key) ? this.$t(key) : job.mapName;
    },

    translateProcessId(item) {
      const [provider, id] = String(item.processID).split(":");
      if (provider === "model") return id;

      const key = `process.${id}.title`;
      return this.$te(key) ? this.$t(key) : item.processTitle || id;
    },

    dateOrNA(date) {
      return date ? utcDateToLocalCalendarString(date) : "N/A";
    },

    utcDateToLocalCalendarString,
  },
};
</script>

<style scoped>
.process-jobs {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}
</style>
/*% } %*/
