/*% if (feature.DM_DataExport) { %*/
<template>
  <v-menu offset-y>
    <template v-slot:activator="{ on, attrs }">
      <v-btn
        v-if="compact"
        small
        icon
        :loading="loading"
        :title="$t('download.button')"
        v-bind="attrs"
        v-on="on"
        data-test="layer-export-button"
      >
        <v-icon>mdi-download</v-icon>
      </v-btn>
      <v-btn
        v-else
        outlined
        color="primary"
        class="ml-2"
        :loading="loading"
        v-bind="attrs"
        v-on="on"
        data-test="data-export-button"
      >
        <v-icon left>mdi-download</v-icon>
        <span class="d-none d-sm-block">{{ $t("download.button") }}</span>
      </v-btn>
    </template>
    <v-list dense>
      <v-list-item
        v-for="format in formats"
        :key="format"
        :data-test="'data-export-' + format"
        @click="start(format)"
      >
        <v-list-item-title>{{ $t("download." + format) }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script>
import { saveBlob } from "@/common/file-download";

/* A "Download" button with one item per format. `fetchData(format)` resolves to the
   Blob to save; a failure was already reported to the user by the HTTP client. */
export default {
  name: "DataExportMenu",
  props: {
    formats: { type: Array, default: () => ["csv"] },
    fileName: { type: String, required: true },
    fetchData: { type: Function, required: true },
    /* a small icon button, for a row of actions */
    compact: { type: Boolean, default: false },
  },
  data() {
    return { loading: false };
  },
  methods: {
    async start(format) {
      this.loading = true;
      try {
        const blob = await this.fetchData(format);
        saveBlob(blob, `${this.fileName}.${format}`);
      } catch (e) {
        /* nothing to add: the HTTP client showed the error */
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>
/*% } %*/
