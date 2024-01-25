/*% if (feature.MV_T_InformationMode) { %*/
<template>
  <v-container class="ma-0 pa-0">
    <v-simple-table dense>
      <template v-slot:default>
        <thead></thead>
        <tbody>
        <tr
          v-for="(property, propertyName) in item.properties"
          :key="item.id + '-' + propertyName"
        >
          <td>{{ propertyName + ":" }}</td>
          <td>
            {{ property || $t("mapViewer.wmsDetail.no-property-data") }}
          </td>
        </tr>
        </tbody>
      </template>
    </v-simple-table>
  </v-container>
</template>
<script>
export default {
  name: "WMSInformationDetail",
  props: ["item", "order"],

  created() {
    let attributes = this.order.map((att) => att.text);
    let orderedProperties = {};
    attributes.forEach((att) => {
      if (this.item.properties[att]) {
        orderedProperties[att] = this.item.properties[att];
      }
    });
    this.itemData = this.item;
    this.itemData.properties = orderedProperties;
  },
};
</script>
/*% } %*/
