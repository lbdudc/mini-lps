/*% if (feature.MV_T_Export) { %*/
<template>
  <v-card>
    <v-card-title primary-title class="headline primary white--text">
      {{ $t("mapViewer.exportManager") }}
    </v-card-title>
    <v-card-text>
      /*% if (feature.MV_T_E_F_PDF || feature.MV_T_E_F_PNG) { %*/
      <v-col>
        <v-row class="mt-4">
          <v-label> {{ $t("mapViewer.downloadFormat") }} </v-label>
        </v-row>
        /*% if (feature.MV_T_E_F_PDF) { %*/
        <v-checkbox v-model="pdf" label="PDF"></v-checkbox>
        <v-checkbox
          v-model="pdfOptions.portrait"
          class="mt-0 ml-2"
          :label="$t('mapViewer.portrait')"
          v-show="pdf"
        ></v-checkbox>
        <v-text-field
          v-model="pdfOptions.margin"
          class="ml-4"
          style="width: 100px"
          v-show="pdf && pdfOptions.portrait"
          :label="$t('mapViewer.margin')"
          type="number"
        ></v-text-field>
        /*% } %*/ /*% if (feature.MV_T_E_F_PNG) { %*/
        <v-checkbox v-model="png" label="PNG"></v-checkbox>
        /*% } %*/
      </v-col>
      /*% if (feature.MV_T_E_SetScale) { %*/
      <v-row class="px-2">
        <v-text-field
          v-model="scale"
          :label="$t('mapViewer.scale')"
          type="number"
        ></v-text-field>
      </v-row>
      /*% } %*/
      /*% } %*/ /*% if (feature.MV_T_E_F_URL) { %*/
      <v-row class="mt-4">
        <v-label> {{ $t("mapViewer.urlToShare") }} </v-label>
      </v-row>
      <v-row class="px-2">
        <v-text-field
          v-model="shareUrl"
          readonly
          append-icon="mdi-content-copy"
          @click:append="copyToClipBoard(shareUrl)"
        ></v-text-field>
      </v-row>
      /*% } %*/
      <v-col class="text-right">
        <v-btn @click="close">
          /*% if (feature.MV_T_E_F_PDF || feature.MV_T_E_F_PNG) { %*/
          {{ $t("mapViewer.cancel") }}
          /*% } else { %*/
          {{ $t("mapViewer.close") }}
          /*% } %*/
        </v-btn>
        /*% if (feature.MV_T_E_F_PDF || feature.MV_T_E_F_PNG) { %*/
        <v-btn class="ml-2 primary" @click="exportMethod" :loading="loading">
          {{ $t("mapViewer.export") }}
        </v-btn>
        /*% } %*/
      </v-col>
    </v-card-text>
  </v-card>
</template>

<script>
/*% if (feature.MV_T_E_F_PDF) { %*/
import jsPDF from "jspdf";
/*% } %*/
/*% if (feature.MV_T_E_F_URL) { %*/
import RepositoryFactory from "@/repositories/RepositoryFactory";
const MVExportManagementRepository = RepositoryFactory.get("MVExportManagementRepository");
/*% } %*/

export default {
  name: "ExportManagement",
  props: ["map"],
  /*% if (feature.MV_T_E_F_URL) { %*/
  mounted() {
    let json = this.map.exportState();
    MVExportManagementRepository.export(json)
      .then(data => {
        this.shareUrl =
          window.location.origin +
          window.location.pathname +
          "?token=" +
          data.token;
      });
  },
  /*% } %*/
  data() {
    return {
      /*% if (feature.MV_T_E_F_PDF && feature.MV_T_E_F_PNG) { %*/loading: false,/*% } %*/
      /*% if (feature.MV_T_E_F_PNG) { %*/png: false,/*% } %*/
      /*% if (feature.MV_T_E_SetScale) { %*/scale: 0,/*% } %*/
      /*% if (feature.MV_T_E_F_URL) { %*/shareUrl: "",/*% } %*/
      /*% if (feature.MV_T_E_F_PDF) { %*/
      pdf: false,
      pdfMargin: 30, // TODO: pasar esto como opción de usuario?
      pdfOptions: { portrait: false, margin: 0 }
      /*% } %*/
    };
  },
  methods: {
    close() {
      this.$emit("close");
      this.$destroy();
    },
    /*% if (feature.MV_T_E_F_PDF || feature.MV_T_E_F_PNG) { %*/
    exportMethod() {
      let timeout = 0;

      if (/*% if (feature.MV_T_E_F_PNG) { %*/this.png/*% } %*/
        /*% if (feature.MV_T_E_F_PDF && feature.MV_T_E_F_PNG) { %*/ || this.pdf/*% } else if (feature.MV_T_E_F_PDF) { %*/ this.pdf/*% } %*/) {
        this.loading = true;
        /*% if (feature.MV_T_E_SetScale) { %*/
        if (this.scale != 0) {
          timeout = 800;
          this.map._map.setZoom(this.scale);
        }
        /*% } %*/
        setTimeout(() => this.startDownload(), timeout);
      } else {
        this.$notify({
          text: this.$t("mapViewer.mustSelectAnExportOption"),
          type: "error"
        });
      }
    },
    startDownload() {
      let map = this.map._map.getContainer();
      let mapControls = this.getChildByClassName(
        map,
        "leaflet-control-container"
      );

      let includedControls = [];

      /*% if (feature.MV_T_E_SetScale) { %*/
      includedControls.push("leaflet-control-scale leaflet-control");
      /*% } %*/

      /*% if (feature.MV_CI_CenterCoordinates) { %*/
      includedControls.push("leaflet-control-mapcentercoord leaflet-control");
      /*% } %*/

      this.excludeControls(mapControls, mapControls, includedControls);

      this.$html2canvas(map, { useCORS: true, logging: false }).then(canvas => {
        /*% if (feature.MV_T_E_F_PDF) { %*/
        if (this.pdf) {
          this.downloadAsPdf(canvas);
        }
        /*% } %*/
        /*% if (feature.MV_T_E_F_PNG) { %*/
        if (this.png) {
          this.downloadAsPng(canvas);
        }
        /*% } %*/
        this.loading = false;
      });
    },
    getChildByClassName(element, className) {
      let children = element.childNodes;
      let childFound = null;
      children.forEach(child => {
        if (child.className == className) {
          childFound = child;
        }
      });
      return childFound;
    },
    excludeControls(parent, container, but) {
      const childNodes = container.childNodes;
      if (but.length == 0) {
        container.setAttribute("data-html2canvas-ignore", "true");
      } else {
        if (childNodes.length != 0 || container.nodeType != Node.TEXT_NODE) {
          container.setAttribute("data-html2canvas-ignore", "true");
        }
        childNodes.forEach(childNode => {
          if (but.includes(childNode.className)) {
            this.includeControl(parent, childNode);
          } else {
            this.excludeControls(parent, childNode, but);
          }
        });
      }
    },
    includeControl(parent, control) {
      if (parent.className != control.className) {
        control.removeAttribute("data-html2canvas-ignore");
        this.includeControl(parent, control.parentNode);
      } else {
        parent.removeAttribute("data-html2canvas-ignore");
      }
    },
    /*% if (feature.MV_T_E_F_PDF) { %*/
    downloadAsPdf(canvas) {
      let doc = new jsPDF({ orientation: "1" });

      let canvasDimensions = this.getCanvasDimensions(
        canvas,
        doc,
        this.pdfMargin
      );

      let iniPosX = (doc.internal.pageSize.width - canvasDimensions.width) / 2;
      let iniPosY;
      if (this.pdfOptions.portrait) {
        iniPosY = this.pdfOptions.margin / 2;
      } else {
        iniPosY = (doc.internal.pageSize.height - canvasDimensions.height) / 2;
      }
      doc.addImage(
        canvas.toDataURL("image/png"),
        "PNG",
        iniPosX,
        iniPosY,
        canvasDimensions.width,
        canvasDimensions.height
      );
      doc.save("map.pdf");
    },
    getCanvasDimensions(canvas, pdfDoc, margin) {
      let docWidth = pdfDoc.internal.pageSize.width;
      let docHeight = pdfDoc.internal.pageSize.height;

      if (
        docWidth > canvas.width + margin &&
        docHeight > canvas.height + margin
      ) {
        return {
          width: canvas.width,
          height: canvas.height
        };
      } else {
        let adjustPercent = (docWidth - (margin || 0)) / canvas.width;
        return {
          width: canvas.width * adjustPercent,
          height: canvas.height * adjustPercent
        };
      }
    },
    /*% } %*/
    /*% if (feature.MV_T_E_F_PNG) { %*/
    downloadAsPng(canvas) {
      let link = document.createElement("a");
      link.setAttribute("download", "map.png");
      link.setAttribute(
        "href",
        canvas.toDataURL("image/png").replace("image/png", "image/octet-stream")
      );
      link.setAttribute("type", "hidden");
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },
    /*% } %*/
    /*% } %*/
    /*% if (feature.MV_T_E_F_URL) { %*/
    copyToClipBoard(textToCopy) {
      navigator.clipboard
        .writeText(textToCopy)
        .then(() => {
          this.$notify({
            text: this.$t("mapViewer.successCopyURL"),
            type: "success"
          });
        })
        .catch(() => {
          this.$notify({
            text: this.$t("mapViewer.errorCopyURL"),
            type: "error"
          });
        });
    },
    /*% } %*/
  }
};
</script>
/*% } %*/
