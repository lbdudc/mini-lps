/*% if (feature.MV_T_InformationMode) { %*/
<template>
  <v-card class="card">
    <v-card-title primary-title class="card-title headline white--text">
      <v-container class="pa-0 ma-0">
        <v-row no-gutters>
          <v-col>
            {{ $t("mapViewer.wmsDetail.title") }}
          </v-col>
          <v-col class="text-end" cols="1">
            <v-btn color="white" icon @click="close">
              <v-icon>mdi-close</v-icon>
            </v-btn>
          </v-col>
        </v-row>
      </v-container>
    </v-card-title>
    <v-card-text class="card-text">
      <v-row class="mt-4">
        <!-- LAYER MANAGEMENT COLUMN -->
        <v-col cols="12" md="3">
          <v-row class="title-text" justify="center" no-gutters>
            <h2>{{ $t("mapViewer.wmsDetail.layers") }}</h2>
          </v-row>
          <v-divider color="primary" class="mt-2 mb-5"></v-divider>
          <v-row
            v-for="layer in Object.keys(layersFeatures)"
            :key="layer"
            class="layers-row"
            :class="layerSelected === layer ? 'selected' : ''"
            @click="onClickLayer(layer)"
            no-gutters
          >
            <li class="layer-management-items">
              {{ $t(getLayerName(layersFeatures[layer].name + ".name")) }} ({{
                layersFeatures[layer].items.length
              }})
            </li>
          </v-row>
        </v-col>
        <!-- LAYER DATA TABLE COLUMN -->
        <v-col cols="12" md="9">
          <v-row class="title-text" justify="center" align="start" no-gutters>
            <v-col v-if="itemSelected" cols="1">
              <v-btn x-small color="secondary" icon @click="onClickBack">
                <v-icon> arrow_back </v-icon>
              </v-btn>
            </v-col>
            <v-col
              :cols="itemSelected ? 8 : 12"
              :class="!itemSelected ? 'text-center' : 'pl-5'"
            >
              <h2 v-if="!itemSelected">
                {{ $t("mapViewer.wmsDetail.layer-items") }}
              </h2>
              <h2 v-else>
                {{ $t("mapViewer.wmsDetail.layer-item-detail") }}
              </h2>
              <v-tooltip v-if="itemSelected && layerForm" bottom>
                <template v-slot:activator="{ on }">
                  <v-icon color="primary" @click="goToForm" v-on="on">
                    description
                  </v-icon>
                </template>
                <span> {{ $t("mapViewer.wmsDetail.form-tooltip") }} </span>
              </v-tooltip>
            </v-col>
          </v-row>
          <v-divider color="primary" class="mt-2 mb-9"></v-divider>
          <v-row class="layers-container" justify="start">
            <wms-information-detail
              v-if="itemSelected"
              class="mt-2"
              :item="itemSelected"
              :order="orderedHeaders"
            />
            <v-data-table
              dense
              hide-default-footer
              v-else-if="layerSelected"
              class="clickable"
              height="100%"
              :headers="tableHeaders"
              :items="layerItems"
              @click:row="onClickItem"
            >
              <template v-slot:[`header.name`]="{ header }">
                <span class="red">
                  {{ header.text.toUpperCase() }}
                </span>
              </template>
              <template v-slot:[`item.id`]="{ item }">
                <span>
                  {{
                    item.id.substr(item.id.lastIndexOf(".") + 1, item.id.length)
                  }}
                </span>
              </template>
            </v-data-table>
            <span v-else>{{ $t("mapViewer.wmsDetail.no-data") }}</span>
          </v-row>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script>
import layers from "../config-files/layers.json";
import WMSInformationDetail from "@/components/map-viewer/wms-information/WMSInformationDetail";
import attributesOrder from "@/modules/entities-attributes.json";

export default {
  name: "WMSInformation",
  components: { "wms-information-detail": WMSInformationDetail },
  props: ["features"],
  data() {
    return {
      layerSelected: null,
      itemSelected: null,
      orderedHeaders: [],
    };
  },
  computed: {
    layersFeatures() {
      //removes duplicate features when the map shows two different styles for one entity
      const features = this.removeDuplicateFeatures(this.features);
      const layersFeatures = {};
      features.forEach((feature) => {
        const tableName = feature.id.substr(0, feature.id.lastIndexOf("."));
        if (!layersFeatures[tableName]) {
          layersFeatures[tableName] = {
            name: tableName.toUpperCase(),
            items: [],
          };
        }
        layersFeatures[tableName].items.push(feature);
      });
      return layersFeatures;
    },
    layerItems() {
      return this.layersFeatures[this.layerSelected]?.items || [];
    },
    layerForm() {
      return layers.layers.find((layer) => layer.name === this.layerSelected)
        ?.form;
    },
    tableHeaders() {
      let headers =
        this.layerItems.length > 0
          ? [{ text: "id", value: "id" }].concat(
            Object.keys(this.layerItems[0].properties)
              .filter((p) => !p.startsWith("geometria"))
              .map((prop) => {
                return {
                  text: prop,
                  value: "properties." + prop,
                };
              })
          )
          : [];
      return this.orderHeaders(headers);
    },
  },
  created() {
    this.layerSelected = Object.keys(this.layersFeatures)[0];
  },
  methods: {
    onClickLayer(layer) {
      this.layerSelected = layer;
      this.itemSelected = null;
    },
    onClickItem(item) {
      this.itemSelected = item;
    },
    onClickBack() {
      this.itemSelected = null;
    },
    goToForm() {
      this.$router.push({
        name: this.layerForm,
        params: {
          id: this.itemSelected.id.substr(
            this.itemSelected.id.lastIndexOf(".") + 1,
            this.itemSelected.id.length
          ),
          backPrevious: true,
        },
      });
    },
    close() {
      this.$emit("close");
      this.$destroy();
    },
    removeDuplicateFeatures(features) {
      let resultIds = [];
      let result = [];
      features.forEach((f) => {
        if (!resultIds.includes(f.id)) {
          resultIds.push(f.id);
          result.push(f);
        }
      });
      return result;
    },
    getLayerName(layer) {
      let layerName = layer
        .toLowerCase()
        .substring(layer.indexOf("_") + 1, layer.length);
      return "t_" + this.toCamelCase(layerName);
    },
    toCamelCase(s) {
      return s.replace(/([-_][a-z])/gi, ($1) => {
        return $1.toUpperCase().replace("-", "").replace("_", "");
      });
    },
    ToSnakeCase(name) {
      return name.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`);
    },

    orderHeaders(headers) {
      let entityName = this.layerItems[0].id.substring(2);
      entityName = this.toCamelCase(
        entityName.substring(0, entityName.indexOf("."))
      );

      const order = attributesOrder[entityName]
        .map((att) => att.name)
        .map((att) => this.ToSnakeCase(att));

      let orderedHeaders = [];
      order.forEach((att) => {
        headers.forEach((header) => {
          if (att == header.text) {
            orderedHeaders.push(header);
          }
        });
      });
      this.orderedHeaders = orderedHeaders;
      return orderedHeaders;
    },
  },
};
</script>
<style scoped>
.card {
  height: 100%;
  overflow: hidden;
}

.card-title {
  overflow: hidden;
  background-color: var(--appColor);
}

.card-text {
  margin-bottom: 0;
  padding-bottom: 0;
  max-height: 90vh;
  overflow-x: hidden;
  overflow-y: auto;
}

.title-text {
  color: var(--appColor);
}

.layers-row {
  margin-bottom: 4px;
  cursor: pointer;
}

.layer-management-items {
  white-space: pre-line;
  padding-bottom: 4px;
}

.layers-row:hover {
  color: dodgerblue;
}
.selected {
  color: dodgerblue;
}
.clickable {
  cursor: pointer;
  flex: 1 1 auto;
}

::v-deep .v-data-table-header tr {
  white-space: nowrap !important;
}
</style>
/*% } %*/
