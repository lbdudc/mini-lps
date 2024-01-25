/*% if (feature.DM_DI_DF_Shapefile) { %*/
<template>
  <v-container>
    <v-row justify="center">
      <v-col cols="12" md="8">
        <v-card>
          <v-card-title class="justify-center">
            <h3>
              {{ $t("shapefile-importer.shapefile-importer") }}
            </h3>
          </v-card-title>
          <v-card-text>
            <v-row v-if="showError">
              <v-col>
                <v-alert type="warning" text dismissible>
                  {{ $t("shapefile-importer.no-entities") }}
                </v-alert>
              </v-col>
            </v-row>
            <v-row justify="center">
              <v-col
                cols="12"
                lg="6"
                class="justify-center d-inline-flex align-center"
              >
                <input type="file" ref="file" @change="changeFileRoute"/>
              </v-col>
              <v-col cols="12" lg="4">
                <v-select
                  v-model="selectedEncoding"
                  class="selector"
                  :label="$t('shapefile-importer.encoding')"
                  :items="['UTF-8', 'ISO-8859-1']"
                  :menu-props="{ offsetY: true }"
                ></v-select>
              </v-col>
            </v-row>
            <v-row justify="center">
              <v-btn
                :disabled="selectedEncoding === ''"
                :loading="loading"
                @click="loadFile"
              >
                {{ $t("shapefile-importer.actions.submit-file") }}
              </v-btn>
            </v-row>
            <v-row v-if="entities.length > 0" justify="center">
              <v-col cols="12" md="6">
                <v-select
                  v-model="selectedEntity"
                  class="selector"
                  :items="entities"
                  :label="$t('shapefile-importer.entities')"
                  :menu-props="{ offsetY: true }"
                  item-text="simpleName"
                  return-object
                  @change="clearProperties"
                ></v-select>
              </v-col>
              <v-col
                class="align-self-center d-inline-flex justify-center"
                cols="12"
                md="6"
              >
                <v-btn
                  :loading="importing"
                  @click="importData"
                >
                  {{ $t("shapefile-importer.actions.import-data") }}
                </v-btn>
              </v-col>
            </v-row>
            <v-row justify="center" v-if="entities.length > 0">
              <v-simple-table>
                <thead>
                <tr>
                  <th>{{ $t("shapefile-importer.table-headers.column") }}</th>
                  <th>
                    {{ $t("shapefile-importer.table-headers.read-value") }}
                  </th>
                  <th>{{ $t("shapefile-importer.table-headers.field") }}</th>
                  <th>{{ $t("shapefile-importer.table-headers.type") }}</th>
                </tr>
                </thead>
                <tbody>
                <tr
                  v-for="(property, index) in shpProperties"
                  :key="property"
                >
                  <td>{{ index + 1 }}</td>
                  <td>{{ property }}</td>
                  <td>
                    <v-select
                      v-model="shpEntityProperties[index]"
                      class="selector"
                      :items="selectedEntity.properties"
                      :disabled="selectedEntity === ''"
                      :menu-props="{ offsetY: true }"
                      :clearable="true"
                      item-text="name"
                      return-object
                    ></v-select>
                  </td>
                  <td v-if="shpEntityProperties[index]">
                    {{ shpEntityProperties[index].simpleType }}
                  </td>
                </tr>
                </tbody>
              </v-simple-table>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import i18n from "@/plugins/i18n";
import RepositoryFactory from "@/repositories/RepositoryFactory";
const EntitiesInformationRepository = RepositoryFactory.get(
  "EntitiesInformationRepository"
);
const ShapefileImportRepository = RepositoryFactory.get(
  "ShapefileImportRepository"
);

export default {
  name: "ShapefileImporter",
  data() {
    return {
      error: false,
      loading: false,
      importing: false,
      entities: [],
      shpProperties: [],
      shpEntityProperties: [],
      selectedEntity: "",
      selectedEncoding: "UTF-8",
      inputFile: "",
      tempFile: null,
      url: null,
      showError: false,
    };
  },
  methods: {
    changeFileRoute(e) {
      this.inputFile = e.target.files[0];
      this.resetData();
    },
    loadFile() {
      this.loading = true;
      // Getting entities from server
      const prom1 = EntitiesInformationRepository.getAll().then((data) => {
        this.entities = data;
        this.entities.forEach((e) => {
          e.properties = e.properties.filter((p) => !p.autogenerated);
          e.properties.sort((a, b) => a.name.localeCompare(b.name));
          e.simpleName = i18n.messages[i18n.locale][e.simpleName]
            ? i18n.messages[i18n.locale][e.simpleName].name
            : e.simpleName;
        });
        this.entities.sort((a, b) => a.name.localeCompare(b.name));
      });

      // Sending file to server
      const formData = new FormData();
      formData.append("type", "shapefile");
      formData.append("encoding", this.selectedEncoding);
      formData.append("file", this.inputFile);
      const prom2 = ShapefileImportRepository.loadFile(formData).then(
        (response) => {
          if (response.data.values.length === 0) {
            this.showError = true;
          } else {
            this.shpProperties = response.data.values;
            this.tempFile = response.data.temporaryFile;
            this.url = response.data.url;
          }
        }
      );

      Promise.all([prom1, prom2]).finally(() => (this.loading = false));
    },
    clearProperties() {
      this.shpEntityProperties = [];
    },
    importData() {
      this.importing = true;
      const data = {
        entityName: this.selectedEntity.name,
        encoding: this.selectedEncoding,
        file: this.tempFile,
        ncolumns: this.shpProperties.length,
        skipFirstLine: true,
        type: "shapefile",
        url: this.url,
        columns: this.shpEntityProperties,
      };
      ShapefileImportRepository.import(data)
        .then(() =>
          this.$notify({
            type: "success",
            text: this.$t("shapefile-importer.import-success"),
          })
        )
        .finally(() => (this.importing = false));
    },
    resetData() {
      this.entities = [];
      this.selectedEntity = "";
      this.tempFile = null;
      this.url = null;
      this.shpProperties = [];
      this.shpEntityProperties = [];
      this.showError = false;
    },
  },
};
</script>
/*% } %*/
