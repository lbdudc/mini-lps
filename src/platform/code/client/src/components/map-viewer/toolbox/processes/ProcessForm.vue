/*% if (feature.MV_Processes) { %*/
<template>
  <div class="process-form">
    <v-row class="process-form__body" no-gutters>
      <!-- Process selector -->
      <v-col class="process-form__tree" cols="12" md="3">
        <v-text-field
          ref="processSearch"
          dense
          clearable
          hide-details
          append-icon="mdi-magnify"
          v-model="searchProcess"
          item-value="id"
          item-text="title"
          :label="$t('toolbox.searchProcess')"
          outlined
          :loading="loading"
        >
        </v-text-field>

        <v-treeview
          class="scroll-container process-tree"
          activatable
          dense
          transition
          open-on-click
          return-object
          item-value="id"
          item-text="title"
          :active.sync="selectedProcess"
          :items="nestedProcesses"
          :search="searchProcess"
        >
          <template v-slot:label="{ item }">
            <v-tooltip right>
              <template v-slot:activator="{ on }">
                <span :class="item.children ? '' : 'leaf-node'" v-on="on">
                  {{ item.title }}
                </span>
              </template>
              <span>{{ item.title }}</span>
            </v-tooltip>
          </template>

          <template v-slot:prepend="{ item, open }">
            <v-icon v-if="item.children && open">mdi-cog-outline</v-icon>
            <v-icon v-else-if="item.children">mdi-cog</v-icon>
            <v-icon v-else>mdi-cogs</v-icon>
          </template>
        </v-treeview>
      </v-col>

      <!-- Process info -->
      <v-col class="process-form__detail pr-1 pl-md-5" cols="12" md="9">
        <loading-spinner v-if="loadingProcess"></loading-spinner>

        <v-row v-else-if="processDetail" no-gutters>
          <v-alert
            dense
            text
            type="info"
            color="primary"
            class="process-info ma-0 pa-3 full-height"
          >
            <h4>
              {{
                translateProcessData(processDetail.id, processDetail.title) ||
                $t("process.not_available.title")
              }}
            </h4>
            <br />

            <div
              class="process-description"
              v-html="
                translateProcessData(
                  processDetail.id,
                  processDetail.description
                ) || $t('process.not_available.description')
              "
            ></div>
          </v-alert>
        </v-row>

        <v-row v-else no-gutters class="no-selection">
          {{ $t("toolbox.noProcesses") }}
        </v-row>

        <v-form
          v-if="processDetail"
          class="mt-5"
          ref="processForm"
          v-model="validForm"
        >
          <div class="mt-3" v-for="(input, index) in inputs" :key="index">
            <component
              dense
              outlined
              v-model="inputValues[input.id]"
              :is="input.form.component"
              :type="input.type"
              :label="input.title"
              :required="input.mandatory"
              :items="input.values"
              :rules="[
                (v) => {
                  if (!input.mandatory) return true;
                  return (
                    !!v ||
                    v === false ||
                    $t('toolbox.fieldRequired', {
                      field: input.title,
                      type: input.type,
                    })
                  );
                },
                (v) => {
                  if (!input.isSink || !v) return true;
                  return (
                    !/\s/.test(v) ||
                    $t('toolbox.noSpacesAllowed', { field: input.title })
                  );
                },
              ]"
            >
              <!-- Bind process input description -->
              <template v-if="input.description" v-slot:append>
                <v-tooltip v-if="input.description" left>
                  <template v-slot:activator="{ on, attrs }">
                    <v-icon v-bind="attrs" v-on="on"> mdi-information </v-icon>
                  </template>
                  <span>{{ input.description }}</span>
                </v-tooltip>
              </template>
            </component>
          </div>
        </v-form>
      </v-col>
    </v-row>

    <!-- Pinned below the scrolling area, so it is always reachable -->
    <div class="process-form__footer">
      <v-btn
        class="ml-2"
        color="primary"
        @click="executeProcess"
        :disabled="!process"
      >
        <span>{{ $t("execute") }}</span>
      </v-btn>
    </div>
  </div>
</template>

<script>
import LoadingSpinner from "@/components/loading-page/LoadingSpinner.vue";

import { PROCESS_INPUTS } from "@/components/map-viewer/toolbox/processes/utils/inputs";

import {
  VTextField,
  VTextarea,
  VCheckbox,
  VSelect,
} from "vuetify/lib/components";

import { handleRequest } from "@/common/proxy";
import properties from "@/properties";

export default {
  name: "Toolbox",
  components: {
    "loading-spinner": LoadingSpinner,
    "v-text-field": VTextField,
    "v-textarea": VTextarea,
    "v-checkbox": VCheckbox,
    "v-select": VSelect,
  },

  props: {
    map: {
      type: Object,
      required: true,
    },
    processMap: {
      type: String,
      required: true,
    },
    jobs: {
      type: Array,
      default: () => [],
    },
  },

  data() {
    return {
      loading: false,
      validForm: false,
      processes: [],
      isMenuOpen: false,
      searchProcess: null,
      selectedProcess: [],
      loadingProcess: false,
      process: null,
      processDetail: null,
      inputValues: {},
      showResult: false,
      success: false,
    };
  },

  computed: {
    nestedProcesses() {
      const providers = {};

      this.processes.forEach((process) => {
        const provider = process.id.split(":")[0];
        if (provider !== "common") {
          if (!providers[provider]) {
            providers[provider] = {
              id: provider,
              title: this.$t(`process.provider.${provider}`),
              children: [],
            };
          }

          providers[provider].children.push({
            ...process,
            title: this.translateProcessData(process.id, process.title),
          });
        }
      });

      return [
        {
          id: "processes",
          title: this.$t("toolbox.processes"),
          children: Object.values(providers),
        },
      ];
    },

    inputs() {
      if (this.processDetail) {
        const processId = this.processDetail.id;

        return Object.keys(this.processDetail.inputs).map((input) => {
          const inputData = this.processDetail.inputs[input];
          /* an input whose schema matches nothing renders as a plain string field */
          const inputForm =
            PROCESS_INPUTS.find((processInput) =>
              processInput.condition(inputData)
            ) || PROCESS_INPUTS.find((processInput) => processInput.type === "string");

          // populate input values
          this.inputValues[input] = inputData.schema?.default;

          // return inputs data
          return {
            id: input,
            title: this.translateProcessData(processId, inputData.title),
            description: this.translateProcessData(
              processId,
              inputData.description
            ),
            type: inputForm.type,
            default: inputData.schema.default || null,
            /* a layer is offered by its label; what is sent is still its id */
            values: (inputData.schema?.enum || []).map((value) => ({
              value,
              text: this.layerLabel(value),
            })),
            mandatory: inputData.minOccurs > 0,
            form: inputForm,
            isSink: inputData.metadata.some((obj) => obj.href === "sink"),
          };
        });
      } else return [];
    },
  },

  watch: {
    selectedProcess(newVal, oldVal) {
      if (oldVal !== newVal && this.selectedProcess.length > 0) {
        this.isMenuOpen = false;

        if (this.selectedProcess[0].id !== "processes") {
          this.process = this.selectedProcess[0];
          this.loadProcessData();
        }
      }
    },
  },

  created() {
    this.fetchProcesses();
  },

  methods: {
    fetchProcesses() {
      this.loading = true;

      handleRequest(`${properties.QGIS_URL}/processes`, {
        headers: {
          "Content-type": "application/json",
        },
      })
        .then((response) => response.json())
        .then((data) => (this.processes = data.processes))
        .catch(() =>
          this.$notify({
            title: this.$t("toolbox.error.title"),
            text: this.$t("toolbox.error.text"),
            type: "error",
          })
        )
        .finally(() => (this.loading = false));
    },

    loadProcessData() {
      this.loadingProcess = true;

      handleRequest(
        `${properties.QGIS_URL}/processes/${this.process.id}?MAP=${this.processMap}`,
        {
          headers: {
            "Content-type": "application/json",
          },
        }
      )
        .then((response) => response.json())
        .then((data) => (this.processDetail = data))
        .finally(() => (this.loadingProcess = false));
    },

    async executeProcess() {
      this.$refs.processForm.validate();

      if (this.validForm) {
        const response = await handleRequest(
          `${properties.QGIS_URL}/processes/${this.process.id}/execution?MAP=${this.processMap}`,
          {
            method: "POST",
            headers: {
              Prefer: "respond-async",
              "Content-type": "application/json",
            },
            body: JSON.stringify({ inputs: this.inputValues }),
          }
        );

        const data = await response.json();

        const newJob = {
          processID: data.processID,
          processTitle: this.translateProcessData(
            this.processDetail.id,
            this.processDetail.title
          ),
          jobID: data.jobID,
          jobRealm: response.headers.get("X-Job-Realm"),
          created: data.created || new Date().toISOString(),
        };

        this.$emit("show-jobs", newJob);
      }
    },

    /* the label of a map layer, or the value itself when it isn't one (an option of an enum) */
    layerLabel(value) {
      const layer = this.map.getLayer(value);
      return layer && typeof layer.getLabel === "function"
        ? layer.getLabel() || value
        : value;
    },

    translateProcessData(processId, data) {
      const provider = processId.split(":")[0];
      return provider !== "model" ? this.$t(data) : data;
    },
  },
};
</script>

<style scoped>
.process-form {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.process-form__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  align-content: flex-start;
}

/* on small screens the tree stacks above the form: keep it from pushing the form away */
.process-form__tree {
  max-height: 35vh;
  overflow-y: auto;
}

.process-form__footer {
  flex: 0 0 auto;
  padding: 12px 8px 4px;
  text-align: right;
  border-top: 1px solid rgba(0, 0, 0, 0.12);
}

/* wide screens: tree and form sit side by side and scroll on their own */
@media (min-width: 960px) {
  .process-form__body {
    overflow-y: hidden;
    flex-wrap: nowrap;
    align-content: stretch;
  }

  .process-form__tree,
  .process-form__detail {
    max-height: 100%;
    overflow-y: auto;
  }
}
</style>
/*% } %*/