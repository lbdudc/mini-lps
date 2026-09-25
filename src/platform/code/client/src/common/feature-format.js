/*% if (feature.MV_T_InformationMode || feature.MV_T_Editing) { %*/
/**
 * How a clicked feature is shown: the field labels of the project (QGIS aliases), only
 * the fields QGIS shows (its hidden columns stay hidden), the labels a value map gives
 * to stored codes, and the QGIS map tip when the layer has one.
 *
 * Features from GeoServer come with the database column names (snake_case) as their
 * property names; the model (modules/entities-attributes.json) knows each field by its
 * camel-cased name.
 */
import attributes from "@/modules/entities-attributes.json";
import layers from "@/components/map-viewer/config-files/layers.json";
import i18n from "@/plugins/i18n";

const GEOMETRY_CLASSES = [
  "Geometry",
  "Point",
  "MultiPoint",
  "LineString",
  "MultiLineString",
  "Polygon",
  "MultiPolygon",
];

const snake = (name) => name.replace(/[A-Z]/g, (l) => `_${l.toLowerCase()}`);
const camel = (s) => s.replace(/_([a-z0-9])/gi, (_, c) => c.toUpperCase());

/** "t_municipios.5" -> { entity: "municipios", id: "5" } (entity as in the model). */
export function parseFeatureId(featureId) {
  const dot = featureId.lastIndexOf(".");
  const table = dot < 0 ? featureId : featureId.substring(0, dot);
  return {
    entity: camel(table.replace(/^t_/, "")),
    id: dot < 0 ? "" : featureId.substring(dot + 1),
  };
}

export function attributesOf(entity) {
  return attributes[entity] || [];
}

/** The fields of the entity a popup shows, in the model's order: no geometry, no hidden ones. */
export function visibleAttributes(entity) {
  return attributesOf(entity).filter(
    (a) => !a.hidden && !GEOMETRY_CLASSES.includes(a.class)
  );
}

/** The field holding the entity's geometry, or null. */
export function geometryAttribute(entity) {
  return attributesOf(entity).find((a) => GEOMETRY_CLASSES.includes(a.class)) || null;
}

/* the kind of input an editing form offers for a field's class (relationships and other
   classes have none: they are left as they are) */
const INPUT_KINDS = {
  String: "text",
  Text: "text",
  Long: "number",
  Integer: "number",
  Short: "number",
  BigDecimal: "number",
  Float: "number",
  Double: "number",
  Boolean: "boolean",
  Date: "date",
};

/**
 * The fields an editing form shows for the entity, in the model order, each with the kind
 * of input it needs (`text`, `number`, `boolean`, `date` or `choice` for a value map):
 * the ones QGIS shows, of a class with an input, without the id.
 */
export function editableFields(entity) {
  return visibleAttributes(entity)
    .filter((a) => a.name !== "id" && INPUT_KINDS[a.class])
    .map((a) => ({
      attribute: a,
      name: a.name,
      label: labelOf(entity, a),
      kind: a.valueMap ? "choice" : INPUT_KINDS[a.class],
      choices: a.valueMap
        ? Object.entries(a.valueMap).map(([value, text]) => ({ value, text }))
        : [],
    }));
}

/** A stored value as its input holds it: a date as "YYYY-MM-DD", the rest as they are. */
export function toInputValue(field, stored) {
  if (stored === null || stored === undefined) return field.kind === "boolean" ? false : "";
  if (field.kind === "date" && Array.isArray(stored)) {
    const [y, m, d] = stored;
    return [y, String(m).padStart(2, "0"), String(d).padStart(2, "0")].join("-");
  }
  return stored;
}

/** What an input holds as the value to store (empty means none). */
export function fromInputValue(field, value) {
  if (field.kind === "boolean") return !!value;
  if (value === "" || value === null || value === undefined) return null;
  if (field.kind === "number") {
    const n = Number(value);
    return Number.isFinite(n) ? n : null;
  }
  return value;
}

/** The database column a field's value comes in. */
export function columnOf(attribute) {
  return snake(attribute.name);
}

/** The field's label: the project's (a QGIS alias) when it has one, else its name. */
export function labelOf(entity, attribute) {
  const key = `t_${entity}.prop.${attribute.name}`;
  return i18n.te(key) ? i18n.t(key) : attribute.name;
}

/** What to show for a stored value: the value map's label for it, or the value itself. */
export function displayValue(attribute, raw) {
  if (raw === null || raw === undefined || raw === "") return "";
  const text = String(raw);
  const map = attribute && attribute.valueMap;
  if (map && Object.prototype.hasOwnProperty.call(map, text)) return map[text];
  return text;
}

/** The layer's QGIS map tip (a template with {{field}} placeholders), or null. */
export function popupTemplateOf(entity) {
  const layer = layers.layers.find(
    (l) =>
      l.popup &&
      l.entity &&
      l.entity.charAt(0).toLowerCase() + l.entity.slice(1) === entity
  );
  return layer ? layer.popup.template : null;
}

const ALLOWED_TAGS = new Set([
  "a", "b", "strong", "i", "em", "u", "br", "hr", "p", "div", "span", "img",
  "ul", "ol", "li", "table", "thead", "tbody", "tr", "td", "th",
  "h1", "h2", "h3", "h4", "h5", "h6", "small", "sub", "sup",
]);
const ALLOWED_ATTRS = new Set(["href", "src", "alt", "title", "colspan", "rowspan", "width", "height"]);
const SAFE_URL = /^(https?:|mailto:|data:image\/|\/|#)/i;

const escapeHtml = (text) =>
  String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");

/** HTML with only harmless tags and attributes: what a map tip may contain. */
export function sanitizeHtml(html) {
  const doc = new DOMParser().parseFromString(String(html), "text/html");
  const clean = (node) => {
    for (const child of Array.from(node.childNodes)) {
      if (child.nodeType === 3) continue;
      if (child.nodeType !== 1) {
        child.remove();
        continue;
      }
      const tag = child.tagName.toLowerCase();
      if (!ALLOWED_TAGS.has(tag)) {
        child.remove();
        continue;
      }
      for (const attribute of Array.from(child.attributes)) {
        const name = attribute.name.toLowerCase();
        const unsafeUrl =
          (name === "href" || name === "src") && !SAFE_URL.test(attribute.value.trim());
        if (!ALLOWED_ATTRS.has(name) || unsafeUrl) child.removeAttribute(attribute.name);
      }
      if (tag === "a") {
        child.setAttribute("target", "_blank");
        child.setAttribute("rel", "noopener noreferrer");
      }
      clean(child);
    }
  };
  clean(doc.body);
  return doc.body.innerHTML;
}

/** The map tip filled in with the feature's values (escaped), then sanitized. */
export function renderPopup(template, entity, properties) {
  const shown = {};
  for (const attribute of attributesOf(entity)) {
    shown[attribute.name] = displayValue(attribute, properties[columnOf(attribute)]);
  }
  const filled = template.replace(/\{\{([^}]*)\}\}/g, (_, name) =>
    escapeHtml(shown[name] === undefined ? "" : shown[name])
  );
  return sanitizeHtml(filled);
}
/*% } %*/
