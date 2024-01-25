/*% if (feature.MV_LM_Style) { %*/
<template>
  <v-dialog persistent :value="showStyleDialog" width="auto">
    <v-card>
      <v-card-title primary-title class="headline primary white--text">
        <v-container class="pa-0 ma-0">
          <v-row no-gutters>
            <v-col class="ma-0">
              {{ $t("layerManager.wms-style.newStyle") }}
            </v-col>
            <v-col cols="1">
              <v-btn @click="$emit('close')" color="white" icon>
                <v-icon>mdi-close</v-icon>
              </v-btn>
            </v-col>
          </v-row>
        </v-container>
      </v-card-title>
      <v-card-text class="pb-0">
        <v-form ref="form">
          <v-container class="mt-3 pb-1">
            <v-row no-gutters>
              <v-text-field
                v-model="styleName"
                class="mt-0"
                dense
                :label="$t('layerManager.wms-style.styleName')"
                :rules="[(v) => !!v || $t('layerManager.nameRequired')]"
                type="text"
              ></v-text-field>
            </v-row>
            <v-row no-gutters class="py-2">
              <v-col class="text-h6 pt-1" cols="8">
                <span class="black--text">
                  {{ $t("layerManager.wms-style.fill") }}
                </span>
              </v-col>
            </v-row>
            <v-divider class="black"></v-divider>
            <v-row no-gutters class="py-2">
              <v-checkbox
                v-model="transparency"
                class="mt-0 ml-2"
                :label="$t('layerManager.wms-style.transparent')"
              ></v-checkbox>
            </v-row>
            <v-row v-if="!transparency" no-gutters class="py-2 pl-3">
              <v-col class="text-subtitle-1 pt-1" cols="8">
                <span>
                  {{ $t("layerManager.wms-style.color") }}
                </span>
                <input
                  class="color-selector float-right"
                  type="color"
                  :value="fillColor"
                  @change="(val) => (fillColor = val.target.value)"
                />
              </v-col>
            </v-row>
            <v-row v-if="!transparency" no-gutters class="py-2 pl-3">
              <v-col class="d-inline-flex">
                <span class="mr-2">
                  {{ $t("layerManager.opacity") }}
                </span>
                <input
                  max="1"
                  min="0"
                  step="0.1"
                  type="range"
                  :value="fillOpacity"
                  @change="(val) => (fillOpacity = val.target.value)"
                />
              </v-col>
            </v-row>
            <v-row no-gutters class="py-2">
              <v-col class="text-h6 pt-1" cols="8">
                <span class="black--text">
                  {{ $t("layerManager.wms-style.stroke") }}
                </span>
              </v-col>
            </v-row>
            <v-divider class="black"></v-divider>
            <v-row no-gutters class="py-2 pl-3">
              <v-col class="text-subtitle-1 pt-1" cols="8">
                {{ $t("layerManager.wms-style.color") }}
                <input
                  class="color-selector float-right"
                  type="color"
                  :value="strokeColor"
                  @change="(val) => (strokeColor = val.target.value)"
                />
              </v-col>
            </v-row>
            <v-row no-gutters class="py-2 pl-3">
              <v-col class="d-inline-flex">
                <span class="mr-2">
                  {{ $t("layerManager.opacity") }}
                </span>
                <input
                  max="1"
                  min="0"
                  step="0.1"
                  type="range"
                  :value="strokeOpacity"
                  @change="(val) => (strokeOpacity = val.target.value)"
                />
              </v-col>
            </v-row>
            <v-row no-gutters class="py-2"> </v-row>
          </v-container>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn class="mr-2 mb-3" @click="applyNewColor">
          {{ $t("layerManager.wms-style.apply") }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  name: "WMSStyleDialog",
  props: {
    showStyleDialog: {
      type: Boolean,
      required: true,
    },
  },
  watch: {
    showStyleDialog(newVal) {
      if (newVal) {
        this.fillColor = "#000000";
        this.strokeColor = "#000000";
        this.styleName = null;
        this.$refs.form?.resetValidation();
        this.transparency = false;
        this.fillOpacity = 1;
        this.strokeOpacity = 1;
      }
    },
  },
  data() {
    return {
      fillColor: "#000000",
      strokeColor: "#000000",
      styleName: null,
      transparency: false,
      fillOpacity: 1,
      strokeOpacity: 1,
    };
  },
  methods: {
    applyNewColor() {
      if (!this.$refs.form.validate()) {
        this.$notify({
          type: "error",
          text: this.$t("layerManager.wms-style.form-error"),
        });
      } else {
        this.$emit("update", {
          styleName: this.styleName,
          fillColor: this.transparency ? "none" : this.fillColor,
          strokeColor: this.strokeColor,
          fillOpacity: this.fillOpacity,
          strokeOpacity: this.strokeOpacity,
        });
      }
    },
  },
};
</script>
/*% } %*/
