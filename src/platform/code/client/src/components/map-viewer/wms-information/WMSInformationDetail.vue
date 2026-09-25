/*% if (feature.MV_T_InformationMode) { %*/
<template>
  <v-container class="ma-0 pa-0">
    <div
      v-if="tipHtml && !showAll"
      class="popup-template"
      data-test="feature-popup"
      v-html="tipHtml"
    ></div>
    <v-simple-table v-else dense>
      <template v-slot:default>
        <tbody>
          <tr
            v-for="row in rows"
            :key="item.id + '-' + row.column"
            data-test="feature-detail-row"
          >
            <td>{{ row.label + ":" }}</td>
            <td>
              {{ row.value || $t("mapViewer.wmsDetail.no-property-data") }}
            </td>
          </tr>
        </tbody>
      </template>
    </v-simple-table>
    <v-btn
      v-if="tipHtml"
      text
      small
      color="primary"
      class="mt-2"
      data-test="feature-popup-toggle"
      @click="showAll = !showAll"
    >
      {{ showAll ? $t("mapViewer.wmsDetail.show-tip") : $t("mapViewer.wmsDetail.show-all") }}
    </v-btn>
  </v-container>
</template>
<script>
import {
  attributesOf,
  visibleAttributes,
  columnOf,
  labelOf,
  displayValue,
  popupTemplateOf,
  renderPopup,
} from "@/common/feature-format";

export default {
  name: "WMSInformationDetail",
  props: {
    item: { type: Object, required: true },
    /* the entity of the feature, as in the model (see feature-format) */
    entity: { type: String, default: "" },
  },
  data() {
    return { showAll: false };
  },
  computed: {
    /* the fields to list: the ones QGIS shows, with their labels and value-map texts */
    rows() {
      const properties = this.item.properties || {};
      if (attributesOf(this.entity).length === 0) {
        /* a layer the model doesn't know (an external WMS): the columns as they come */
        return Object.keys(properties).map((column) => ({
          column,
          label: column,
          value: properties[column],
        }));
      }
      return visibleAttributes(this.entity)
        .filter((a) => columnOf(a) in properties)
        .map((a) => ({
          column: columnOf(a),
          label: labelOf(this.entity, a),
          value: displayValue(a, properties[columnOf(a)]),
        }));
    },
    /* the QGIS map tip filled in with this feature, or null when the layer has none */
    tipHtml() {
      const template = popupTemplateOf(this.entity);
      return template
        ? renderPopup(template, this.entity, this.item.properties || {})
        : null;
    },
  },
};
</script>
<style scoped>
.popup-template {
  padding: 4px 8px;
}
</style>
/*% } %*/
