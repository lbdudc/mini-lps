/*% if (feature.DM_DI_DF_Shapefile) { %*/
import ShapefileImport from "./ShapefileImporter";

const routes = [
  {
    path: "/shapefile",
    name: "Shapefile",
    meta: {
      label: "shapefile-importer.shapefile-importer"
    },
    component: ShapefileImport
  }
];

export default routes;
/*% } %*/
