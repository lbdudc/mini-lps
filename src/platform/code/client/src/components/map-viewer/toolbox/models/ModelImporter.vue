/*% if (feature.MV_Processes) { %*/
<template>
  <v-container fluid class="full-height d-flex flex-column">
    <v-row class="justify-end flex-grow-0">
      <!-- Search models -->
      <v-col cols="12" md="4" class="text-end">
        <debounced-text-field
          v-model="search"
          append-icon="search"
          class="d-md-inline-block text-center"
          dense
          hide-details
          :label="$t('toolbox.models.search')"
          :outlined="true"
          @input="fetchModels"
        ></debounced-text-field>
      </v-col>
    </v-row>
    <v-row class="justify-center flex-grow-1">
      <!-- Models table -->
      <v-container>
        <v-data-table
          dense
          :headers="headers"
          :items="models"
          :loading="loading"
          :options="entitiesPage"
          :server-items-length="totalModels"
          @update:options="fetchOnTableChange"
        >
          <template v-slot:[`item.lastModifiedTime`]="{ item }">
            {{ localDateToVCalendarString(item.lastModifiedTime) }}
          </template>

          <template v-slot:[`item.action`]="{ item }">
            <v-btn color="red" icon @click="deleteModel(item)">
              <v-icon>mdi-delete</v-icon>
            </v-btn>

            <v-btn color="success" icon @click="downloadModel(item)">
              <v-icon>mdi-download</v-icon>
            </v-btn>
          </template>
        </v-data-table>
      </v-container>
    </v-row>
    <v-row class="my-2 mx-10 flex-grow-0" justify="center">
      <!-- Import model -->
      <v-col cols="12" md="6" class="d-inline-flex align-center justify-start">
        <div>
          <v-btn class="mr-3" color="primary" @click="triggerFileInput">
            <v-icon left>mdi-upload</v-icon>
            {{ $t("toolbox.models.examine") }}
          </v-btn>
          <input
            ref="file"
            type="file"
            accept=".model3"
            style="display: none"
            @change="changeFileRoute"
          />
        </div>
        <span v-if="inputFile">{{ inputFile.name }}</span>
        <span v-else>{{ $t("toolbox.models.noModel") }}</span>
      </v-col>
      <v-col
        cols="12"
        md="6"
        class="d-inline-flex align-center justify-start justify-md-end"
      >
        <v-btn
          :loading="importing"
          :disabled="!inputFile"
          @click="importModel(false)"
        >
          {{ $t("toolbox.models.import") }}
        </v-btn>
      </v-col>
    </v-row>

    <!-- Overwrite map dialog -->
    <modal-dialog
      @cancel="overwriteDialog = false"
      @submit="importModel(true)"
      :dialog="overwriteDialog"
      :title="$t('toolbox.models.overwrite.title')"
      titleClass="warning white--text"
      titleIcon="warning"
      submitClass="warning"
      :submitText="$t('accept')"
      :content="$t('toolbox.models.overwrite.text')"
    ></modal-dialog>
  </v-container>
</template>

<script>
import { localDateToVCalendarString } from "@/common/conversion-utils";
import ModalDialog from "@/components/modal_dialog/ModalDialog.vue";
import DebouncedTextField from "@/components/debouncing-inputs/DebouncedTextField.vue";

import RepositoryFactory from "@/repositories/RepositoryFactory";
const Model3EntityRepository = RepositoryFactory.get("Model3EntityRepository");

export default {
  name: "ModelImporter",
  components: { DebouncedTextField, ModalDialog },
  data() {
    return {
      loading: false,
      search: undefined,
      models: [],
      inputFile: null,
      importing: false,
      overwriteDialog: false,
      entitiesPage: {
        page: 1,
        itemsPerPage: 5,
      },
      totalModels: 0,
      headers: [
        {
          text: this.$t("toolbox.models.model"),
          value: "fileName",
          sortable: false,
        },
        {
          text: this.$t("toolbox.models.lastModifiedDate"),
          value: "lastModifiedTime",
          sortable: false,
        },
        { text: "", value: "action", sortable: false },
      ],
    };
  },
  created() {
    this.fetchModels();
  },
  methods: {
    localDateToVCalendarString,
    fetchModels() {
      this.loading = true;
      const options = {
        params: {
          search: this.search,
          page: this.entitiesPage.page - 1,
          size: this.entitiesPage.itemsPerPage,
        },
      };

      Model3EntityRepository.getAll(options)
        .then((response) => {
          this.models = response.content;
          this.totalModels = response.totalElements;
        })
        .finally(() => (this.loading = false));
    },
    triggerFileInput() {
      this.$refs.file.click();
    },
    changeFileRoute(e) {
      this.inputFile = e.target.files[0];
    },
    importModel(overwrite) {
      this.overwriteDialog = false;

      if (!this.inputFile.name.endsWith(".model3")) {
        this.$notify({
          title: this.$t("toolbox.models.invalid_file.title"),
          text: this.$t("toolbox.models.invalid_file.text"),
          type: "error",
        });
        return;
      }

      if (this.inputFile.size > 10485760) {
        this.$notify({
          title: this.$t("toolbox.models.large_file.title"),
          text: this.$t("toolbox.models.large_file.text"),
          type: "error",
        });
        return;
      }

      this.importing = true;
      const formData = new FormData();

      formData.append("file", this.inputFile);
      formData.append("overwrite", overwrite);

      Model3EntityRepository.save(formData)
        .then((response) => {
          this.inputFile = null;
          if (response.duplicatedAlgorithm) {
            this.$notify({
              title: this.$t("toolbox.models.duplicated_algorithm.title"),
              text: this.$t("toolbox.models.duplicated_algorithm.text"),
              type: "warning",
            });
          } else {
            this.$notify({
              title: this.$t("toolbox.models.success.title"),
              text: this.$t("toolbox.models.success.save"),
              type: "success",
            });
          }
        })
        .catch((err) => {
          const status = err.response.status;
          if (status === 409) this.overwriteDialog = true;
        })
        .finally(() => {
          this.importing = false;
          this.fetchModels();
        });
    },
    deleteModel(model) {
      Model3EntityRepository.delete(model.id)
        .then(() => {
          this.$notify({
            title: this.$t("toolbox.models.success.title"),
            text: this.$t("toolbox.models.success.delete"),
            type: "success",
          });
        })
        .finally(() => this.fetchModels());
    },
    async downloadModel(model) {
      const res = await Model3EntityRepository.getFileById(model.id);
      var fileURL = window.URL.createObjectURL(new Blob([res]));
      var fileLink = document.createElement("a");
      fileLink.href = fileURL;
      fileLink.setAttribute("download", model.fileName);
      document.body.appendChild(fileLink);
      fileLink.click();
    },
    fetchOnTableChange(pagination = this.entitiesPage) {
      this.entitiesPage = pagination;
      this.fetchModels();
    },
  },
};
</script>
/*% } %*/