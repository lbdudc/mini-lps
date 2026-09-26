/*% if (feature.MV_T_Editing) { %*/
<template>
  <div v-if="editableLayers.length > 0" class="feature-editor">
    <div class="feature-editor__toolbar">
      <v-btn
        small
        :color="mode === 'off' ? 'white' : 'primary'"
        :title="$t('mapViewer.edit.toggle')"
        data-test="edit-toggle"
        @click="toggle"
      >
        <v-icon small left>mdi-pencil</v-icon>
        {{ $t("mapViewer.edit.toggle") }}
      </v-btn>

      <v-menu v-if="mode !== 'off' && editableLayers.length > 1" offset-y>
        <template v-slot:activator="{ on }">
          <v-btn small color="white" class="ml-2" data-test="edit-add" v-on="on">
            <v-icon small left>mdi-plus</v-icon>
            {{ $t("mapViewer.edit.add") }}
          </v-btn>
        </template>
        <v-list dense>
          <v-list-item
            v-for="layer in editableLayers"
            :key="layer.name"
            data-test="edit-add-layer"
            @click="startAdd(layer)"
          >
            <v-list-item-title>{{ layerLabel(layer) }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
      <v-btn
        v-else-if="mode !== 'off'"
        small
        color="white"
        class="ml-2"
        data-test="edit-add"
        @click="startAdd(editableLayers[0])"
      >
        <v-icon small left>mdi-plus</v-icon>
        {{ $t("mapViewer.edit.add") }}
      </v-btn>
    </div>

    <v-card
      v-if="mode !== 'off'"
      class="feature-editor__panel"
      elevation="8"
      data-test="edit-panel"
    >
      <v-card-title class="subtitle-1 py-2">
        {{ title }}
        <v-spacer></v-spacer>
        <v-btn icon small data-test="edit-close" @click="leave">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>
      <v-progress-linear v-if="busy" indeterminate height="2"></v-progress-linear>

      <v-card-text class="pt-0">
        <div v-if="mode === 'pick'" class="grey--text text--darken-1" data-test="edit-hint">
          {{ notice || $t("mapViewer.edit.hint-pick") }}
        </div>
        <div v-else-if="mode === 'draw'" class="grey--text text--darken-1" data-test="edit-hint">
          {{ $t("mapViewer.edit.hint-draw") }}
        </div>

        <template v-if="current">
          <div class="grey--text text--darken-1 mb-2" data-test="edit-hint">
            {{ $t("mapViewer.edit.hint-move") }}
          </div>
          <div v-for="field in current.fields" :key="field.name" class="mb-1">
            <v-switch
              v-if="field.kind === 'boolean'"
              v-model="values[field.name]"
              :label="field.label"
              dense
              hide-details
              :data-test="'edit-field-' + field.name"
            ></v-switch>
            <v-select
              v-else-if="field.kind === 'choice'"
              v-model="values[field.name]"
              :items="field.choices"
              :label="field.label"
              dense
              clearable
              hide-details="auto"
              :data-test="'edit-field-' + field.name"
            ></v-select>
            <v-text-field
              v-else
              v-model="values[field.name]"
              :label="field.label"
              :type="field.kind === 'date' ? 'date' : field.kind === 'number' ? 'number' : 'text'"
              dense
              hide-details="auto"
              :data-test="'edit-field-' + field.name"
            ></v-text-field>
          </div>
          <div v-if="error" class="error--text mt-2" data-test="edit-error">{{ error }}</div>
        </template>
      </v-card-text>

      <v-card-actions v-if="current">
        <v-btn
          v-if="mode === 'edit'"
          text
          small
          color="error"
          data-test="edit-delete"
          @click="confirmDelete = true"
        >
          {{ $t("mapViewer.edit.delete") }}
        </v-btn>
        <v-spacer></v-spacer>
        <v-btn text small data-test="edit-cancel" @click="cancel">
          {{ $t("mapViewer.edit.cancel") }}
        </v-btn>
        <v-btn
          small
          color="primary"
          :disabled="busy"
          data-test="edit-save"
          @click="save"
        >
          {{ $t("mapViewer.edit.save") }}
        </v-btn>
      </v-card-actions>
    </v-card>

    <v-dialog v-model="confirmDelete" max-width="360">
      <v-card data-test="edit-confirm-delete">
        <v-card-title class="subtitle-1">{{ $t("mapViewer.edit.delete-title") }}</v-card-title>
        <v-card-text>{{ $t("mapViewer.edit.delete-text") }}</v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn text @click="confirmDelete = false">{{ $t("mapViewer.edit.cancel") }}</v-btn>
          <v-btn color="error" data-test="edit-confirm-delete-yes" @click="remove">
            {{ $t("mapViewer.edit.delete") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import RepositoryFactory from "@/repositories/RepositoryFactory";
import {
  parseFeatureId,
  geometryAttribute,
  editableFields,
  toInputValue,
  fromInputValue,
} from "@/common/feature-format";

const SHAPE = { color: "#e53935", weight: 3 };
const lowerFirst = (text) => text.charAt(0).toLowerCase() + text.slice(1);

/* The map's editor: for the layers marked editable in QGIS, pick a feature on the map to
   change its shape and its fields, or draw a new one, or delete it. Everything goes through
   the entity's own API (the same the forms use); nginx asks for the editing account when a
   change is sent. */
export default {
  name: "FeatureEditor",
  props: {
    /* the map-viewer Map */
    map: { type: Object, default: null },
    /* the layers of the map shown: layers.json entries */
    overlays: { type: Array, default: () => [] },
  },
  data() {
    return {
      mode: "off" /* off | pick | edit | draw | new */,
      busy: false,
      error: "",
      notice: "",
      current: null,
      values: {},
      confirmDelete: false,
    };
  },
  computed: {
    editableLayers() {
      return this.overlays.filter((l) => l.editable && l.entity);
    },
    title() {
      if (this.current) return this.layerLabel(this.current.layer);
      return this.$t("mapViewer.edit.title");
    },
  },
  watch: {
    /* the map's right-hand button shows whether there is anything to edit, and whether editing is on */
    mode() {
      this.reportState();
    },
    editableLayers() {
      this.reportState();
    },
    confirmDelete(open) {
      if (!open) this._dialogClosedAt = Date.now();
    },
    /* another map (a search reloads it): the old one is gone with what was on it */
    map() {
      this.leave();
    },
  },
  mounted() {
    this.reportState();
  },
  created() {
    this._dialogClosedAt = 0;
    this._group = null;
    this._editor = null;
    this._drawer = null;
  },
  beforeDestroy() {
    this.leave();
  },
  methods: {
    leaflet() {
      return this.map && this.map.getLeafletMap();
    },
    layerLabel(layer) {
      return this.$t("mapViewer.layer-label." + layer.name.replace(".", "-"));
    },
    repositoryOf(layer) {
      return RepositoryFactory.get(layer.entity + "EntityRepository");
    },
    reportState() {
      this.$emit("state", { available: this.editableLayers.length > 0, active: this.mode !== "off" });
    },
    toggle() {
      if (this.mode === "off") this.enterPick();
      else this.leave();
    },
    enterPick() {
      const leaflet = this.leaflet();
      if (!leaflet) return;
      // Clicks now pick a feature to edit: the info tool must not answer them too
      if (this.map.deactivateLayerInfo) this.map.deactivateLayerInfo();
      this.mode = "pick";
      this.notice = "";
      leaflet.on("click", this.onMapClick);
      leaflet.getContainer().style.cursor = "crosshair";
      document.addEventListener("keydown", this.onKey);
    },
    /* back to nothing: no edit in progress, no picking */
    leave() {
      this.clearWork();
      const leaflet = this.leaflet();
      if (leaflet) {
        leaflet.off("click", this.onMapClick);
        leaflet.getContainer().style.cursor = "";
      }
      document.removeEventListener("keydown", this.onKey);
      this.mode = "off";
      this.notice = "";
    },
    /* drops the feature being edited or drawn, staying in "pick" */
    cancel() {
      this.clearWork();
      if (this.mode !== "off") this.mode = "pick";
    },
    clearWork() {
      const leaflet = this.leaflet();
      if (this._drawer) {
        this._drawer.disable();
        this._drawer = null;
      }
      if (leaflet && this._onCreated) {
        leaflet.off(L.Draw.Event.CREATED, this._onCreated);
        this._onCreated = null;
      }
      if (this._editor) {
        this._editor.disable();
        this._editor = null;
      }
      if (this._group) {
        this._group.remove();
        this._group = null;
      }
      this.current = null;
      this.values = {};
      this.error = "";
      this.confirmDelete = false;
    },
    onKey(event) {
      if (event.key !== "Escape") return;
      // The confirmation closes itself on Escape (and is already closed by the time this
      // runs): that key press was for it, not for the edit behind it
      if (this.confirmDelete || Date.now() - this._dialogClosedAt < 400) return;
      if (this.current || this.mode === "draw") this.cancel();
    },
    /* --- picking */
    onMapClick(event) {
      if (this.mode !== "pick") return;
      this.map.identify(event.containerPoint.x, event.containerPoint.y, (features) =>
        this.pickFrom(features)
      );
    },
    pickFrom(features) {
      const byEntity = {};
      this.editableLayers.forEach((l) => (byEntity[lowerFirst(l.entity)] = l));
      for (const feature of features || []) {
        const { entity, id } = parseFeatureId(feature.id);
        if (byEntity[entity] && id) {
          this.notice = "";
          this.load(byEntity[entity], entity, id);
          return;
        }
      }
      this.notice = this.$t("mapViewer.edit.nothing");
    },
    async load(layer, entityKey, id) {
      this.busy = true;
      this.error = "";
      try {
        const repo = this.repositoryOf(layer);
        const full = await repo.get(id);
        const geomAttr = geometryAttribute(entityKey);
        this.clearWork();
        this.current = {
          layer,
          entityKey,
          repo,
          id,
          full,
          geomAttr,
          fields: editableFields(entityKey),
        };
        this.values = Object.fromEntries(
          this.current.fields.map((f) => [f.name, toInputValue(f, full[f.name])])
        );
        this.showGeometry(full[geomAttr.name], true);
        this.mode = "edit";
      } catch (e) {
        this.error = this.$t("mapViewer.edit.error");
      } finally {
        this.busy = false;
      }
    },
    /* the shape on the map, ready to be dragged into another */
    showGeometry(geometry, fit) {
      const leaflet = this.leaflet();
      this._group = L.featureGroup().addTo(leaflet);
      L.geoJSON(geometry, { style: SHAPE }).eachLayer((l) => this._group.addLayer(l));
      this.startShapeEdit(leaflet);
      if (fit) {
        try {
          leaflet.fitBounds(this._group.getBounds(), { maxZoom: 17, padding: [80, 80] });
        } catch (e) {
          /* a shape with no extent: the view stays */
        }
      }
    },
    startShapeEdit(leaflet) {
      this._editor = new L.EditToolbar.Edit(leaflet, { featureGroup: this._group });
      this._editor.enable();
    },
    /* the shape as it is now, in the class the entity stores (Multi* wrap a single one) */
    currentGeometry() {
      const geometries = this._group
        .toGeoJSON()
        .features.map((f) => f.geometry)
        .filter(Boolean);
      const shape = geometries[0];
      if (!shape) return null;
      const wanted = this.current.geomAttr.class;
      const multi = { Polygon: "MultiPolygon", LineString: "MultiLineString", Point: "MultiPoint" };
      if (multi[shape.type] === wanted) {
        return { type: wanted, coordinates: [shape.coordinates] };
      }
      return shape;
    },
    /* --- drawing a new feature */
    startAdd(layer) {
      const leaflet = this.leaflet();
      if (!leaflet) return;
      this.clearWork();
      this.notice = "";
      const entityKey = lowerFirst(layer.entity);
      const geomAttr = geometryAttribute(entityKey);
      if (!geomAttr) return;
      const cls = geomAttr.class;
      this._drawer = cls.includes("Point")
        ? new L.Draw.Marker(leaflet, {})
        : cls.includes("Polygon")
          ? new L.Draw.Polygon(leaflet, { shapeOptions: SHAPE })
          : new L.Draw.Polyline(leaflet, { shapeOptions: SHAPE });
      this._onCreated = (event) => {
        this._drawer = null;
        this._onCreated = null;
        this.finishDraw(layer, entityKey, geomAttr, event.layer);
      };
      leaflet.once(L.Draw.Event.CREATED, this._onCreated);
      this._drawer.enable();
      this.mode = "draw";
    },
    finishDraw(layer, entityKey, geomAttr, drawn) {
      const leaflet = this.leaflet();
      this._group = L.featureGroup([drawn]).addTo(leaflet);
      this.startShapeEdit(leaflet);
      const fields = editableFields(entityKey);
      this.current = {
        layer,
        entityKey,
        repo: this.repositoryOf(layer),
        id: null,
        full: {},
        geomAttr,
        fields,
      };
      this.values = Object.fromEntries(fields.map((f) => [f.name, toInputValue(f, null)]));
      this.mode = "new";
    },
    /* --- saving and deleting */
    async save() {
      if (!this.current || this.busy) return;
      this.busy = true;
      this.error = "";
      try {
        if (this._editor) this._editor.save();
        const { repo, geomAttr, fields, layer } = this.current;
        const dto = { ...this.current.full };
        fields.forEach((f) => (dto[f.name] = fromInputValue(f, this.values[f.name])));
        dto[geomAttr.name] = this.currentGeometry();
        await repo.save(dto);
        await this.afterChange(layer, repo);
        this.cancel();
        this.notice = this.$t("mapViewer.edit.saved");
      } catch (e) {
        this.error = this.$t("mapViewer.edit.error");
      } finally {
        this.busy = false;
      }
    },
    async remove() {
      if (!this.current || this.busy) return;
      this.confirmDelete = false;
      this.busy = true;
      try {
        const { repo, id, layer } = this.current;
        await repo.delete(id);
        await this.afterChange(layer, repo);
        this.cancel();
        this.notice = this.$t("mapViewer.edit.deleted");
      } catch (e) {
        this.error = this.$t("mapViewer.edit.error");
      } finally {
        this.busy = false;
      }
    },
    /* the layer on the map shows what changed */
    async afterChange(layer, repo) {
      if (repo.restartGeom) {
        try {
          await repo.restartGeom();
        } catch (e) {
          /* the extent is only a hint: the change itself is saved */
        }
      }
      const mapLayer = this.map.getLayer(layer.name);
      if (mapLayer) {
        const leafletLayer = await Promise.resolve(mapLayer.getLayer());
        // A new parameter: the tiles are asked for again instead of taken from the cache
        if (leafletLayer && leafletLayer.setParams) {
          leafletLayer.setParams({ gpRefresh: Date.now() });
        }
      }
    },
  },
};
</script>

<style scoped>
.feature-editor__toolbar {
  position: absolute;
  top: 58px;
  left: 64px;
  z-index: 950;
  display: flex;
}
.feature-editor__panel {
  position: absolute;
  top: 100px;
  right: 84px;
  z-index: 950;
  width: 340px;
  max-width: calc(100% - 100px);
  max-height: calc(100% - 100px);
  overflow-y: auto;
}
</style>
/*% } %*/
