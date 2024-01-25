/**
 * Map Control Functions
 */

/*% if (feature.MV_CI_Scale) { %*/
/**
 * Builds Scale control
 **/
function buildMapScaleControl() {
    return new L.control.scale();
}
/*% } %*/
/*% if (feature.MV_CI_CenterCoordinates) { %*/
/**
 * Builds coordinates control
 * https://github.com/xguaita/Leaflet.MapCenterCoord
 */
function buildMapCoordinatesControl() {
    var mapCenterOptions = {
        icon: false,
        onMove: true,
        latlngFormat: "DMS",
        latlngDesignators: true
    };
    // Show map coordinates
    return new L.control.mapCenterCoord(mapCenterOptions);
}
/*% } %*/
/*% if (feature.MV_CI_Map) { %*/
/**
 * Builds MiniMap control
 * https://github.com/Norkart/Leaflet-MiniMap
 */
function buildMiniMapControl(isMobile, defaultBaseLayer) {
    var miniMapOptions = {
        position: "bottomright",
        width: isMobile ? 75 : 125,
        height: isMobile ? 75 : 125,
        toggleDisplay: true,
        zoomLevelOffset: -5,
        zoomLevelFixed: false,
        zoomAnimation: false,
        autoToggleDisplay: true,
        aimingRectOptions: {
            color: "#ff7800",
            weight: 1,
            clickable: false,
        },
        shadowRectOptions: {
            color: "#000000",
            weight: 1,
            clickable: false,
            opacity: 0,
            fillOpacity: 0,
        },
    };
    let miniMapLayer;
    if (defaultBaseLayer) {
        miniMapLayer = new L.TileLayer(defaultBaseLayer.url, {
            minZoom: 0,
            maxZoom: 13,
        });
    } else {
        miniMapLayer = new L.TileLayer(
            "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
            {
                minZoom: 0,
                maxZoom: 13,
            }
        );
    }

    return new L.Control.MiniMap(miniMapLayer, miniMapOptions);
}
/*% } %*/
/*% if (feature.MV_T_MeasureControl) { %*/
/**
 * Builds the measure tool control
 */
function buildMeasureControl() {
    const measureOptions = {
        position: "topleft",
        primaryLengthUnit: "kilometers",
        secondaryLengthUnit: undefined,
        primaryAreaUnit: "sqkilometers",
        activeColor: "#CC3E33",
        completedColor: "#33CC3E",
        units: {
            sqkilometers: {
                factor: 0.000001,
                display: "km2",
                decimals: 2
            }
        },
        decPoint: ",",
        thousandsSep: "."
    };
    return L.control.measure(measureOptions);
}
/*% } %*/
/*% if (feature.MV_T_ZoomWindow) { %*/
/**
 * Builds the zoombox control
 * http://angular-ui.github.io/ui-leaflet/examples/0000-viewer.html#/controls/minimap-example
 * https://github.com/consbio/Leaflet.ZoomBox
 */
function buildZoomBoxControl() {
    return L.control.zoomBox({
        modal: false,
        position: "topleft",
        title: "Zoom Box Control"
    });
}
/*% } %*/
/*% if (feature.MV_T_UserGeolocation) { %*/
/** Builds the locate control
 * https://github.com/domoritz/leaflet-locatecontrol
 */
function buildLocateControl() {
    const locateControlOptions = {
        strings: {
            title: "User location"
        },
        iconElementTag: "a",
        icon: "user-location-icon"
    };
    return L.control.locate(locateControlOptions);
}
/*% } %*/


export { /*% if (feature.MV_CI_Scale) { %*/buildMapScaleControl,/*% } %*/
    /*% if (feature.MV_CI_CenterCoordinates) { %*/ buildMapCoordinatesControl,/*% } %*/
    /*% if (feature.MV_CI_Map) { %*/ buildMiniMapControl,/*% } %*/
    /*% if (feature.MV_T_MeasureControl) { %*/ buildMeasureControl,/*% } %*/
    /*% if (feature.MV_T_ZoomWindow) { %*/ buildZoomBoxControl,/*% } %*/
    /*% if (feature.MV_T_UserGeolocation) { %*/ buildLocateControl/*% } %*/
};
