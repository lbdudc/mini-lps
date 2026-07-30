/*% if (feature.MV_Processes) { %*/
# QGIS

This project uses [Py-QGIS-WPS](https://github.com/3liz/py-qgis-wps) and [Py-QGIS-Server](https://github.com/3liz/py-qgis-server) to provide geospatial processing services and serve the results of these processes via WFS/WCS.

## Project setup

### Environment variables

The most important environment variables that can affect deployment and behavior during local development are:

- `QGSWPS_SERVER_PROXY_URL`: The base URL of the WPS server (Default is http://localhost:8888)
- `QGSWPS_SERVER_OWS_SERVICE_URL`: Base URL used for generated OWS (WFS/WCS) service requests (Default is http://localhost:8082/ows/)
- `QGSRV_SERVER_PROXY_URL`: The base URL of the map server (Default is http://localhost:8082)

⚠️ Make sure the default deployment ports (8888, 8082 and 6379) are free before starting the service containers.

### Local Deployment

You can run both services locally using docker compose from this folder:

```
docker-compose --env-file ../deploy/.env.development -f ../deploy/docker-compose.yml up -d --build py-qgis-wps py-qgis-server
```

This will also create the shared volumes used to store results and base projects.

## WPS

Py-QGIS-WPS implements [OGC WPS](https://www.ogc.org/es/standards/wps/) (Web Processing Service) standards using QGIS Python libraries. It provides geospatial algorithms and operations that can be executed via HTTP requests. 

- `http://localhost:8888/processes`: List of available WPS processes
- `http://localhost:8888/jobs`: View running jobs, including their status and results

### Create processing environment

First, create a **base environment project** to provide layers for processing. Use the `common:importmapstate` process with the map state in JSON format in the request body.

**HTTP Request:**
```http
POST http://localhost:8888/processes/common:importmapstate/execution
Content-Type: application/json
```

**Request Body Example:**
```
{
  "inputs": {
    "MAP_STATE": {
      "id": "test_map",
      "view": {
        "center": {
          "lat": 42.35854391749705,
          "lng": -127.96875000000001
        },
        "zoom": 3
      },
      "minZoom": 0,
      "maxZoom": 18,
      "layerOrder": [
        "us-states.geojson"
      ],
      "layers": [
        {
          "options": {
            "id": "OpenStreetMap.Mapnik",
            "type": "TILE",
            "opacity": null,
            "selected": true,
            "baseLayer": true,
            "url": "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
            "filters": []
          }
        },
        {
          "options": {
            "id": "us-states.geojson",
            "type": "GeoJSONLayerStyle",
            "fillColor": "#106CB6",
            "strokeColor": "#106CB6",
            "fillOpacity": 0.55,
            "strokeOpacity": 0.55,
            "weight": 3,
            "radius": 7,
            "icon": null,
            "draggable": null,
            "dragend": null
          },
          "baseLayer": false,
          "color": "#106CB6",
          "added": true,
          "list": null,
          "url": "https://raw.githubusercontent.com/shawnbot/topogram/master/data/us-states.geojson",
          "label": "us-states.geojson",
          "filters": []
        }
      ],
      "dimensionFilters": []
    },
    "MAP_ID": 303
  }
}
```

Use the returned `REF_ID` to execute further processes on this environment.

### Execute process 

The next example is running a buffer process over the newly created environment and imported layers:

**HTTP Request:**
```http
POST http://localhost:8888/processes/vector_geometry:buffer/execution?MAP=303
Content-Type: application/json
Prefer: respond-async
```

**Request Body Example:**
```
{
  "inputs": {
    "INPUT": "us-states.geojson",
    "OUTPUT": "buffer_output",
    "DISSOLVE": false,
    "DISTANCE": 10,
    "SEGMENTS": 5,
    "END_CAP_STYLE": "Round",
    "JOIN_STYLE": "Round",
    "MITER_LIMIT": 2
  }
}
```

The response headers will include a `X-Job-Realm` that will be required to access job results. To disable this behavior, unset the `QGSWPS_SERVER_ENABLE_JOB_REALM` variable in the WPS service configuration.

### See job progress and access results

**HTTP Request:**
```http
GET http://localhost:8888/jobs
Content-Type: application/json
X-Job-Realm: xxxxxxx
```

**HTTP Request:**
```http
GET http://localhost:8888/jobs/xxxxxxx/results
Content-Type: application/json
X-Job-Realm: xxxxxxx
```

Process results will include a reference to the base Map Server URL needed to access results layers and outputs.

## Map Server

Serves the output of QGIS projects as web-accessible layers and services via WFS/WCS requests.

- `http://localhost:8082/`: Main server endpoint
- `http://localhost:8082/ows/`: OWS endpoint for accessing WFS/WCS services

## Postman Collection

A Postman collection is included in the `test/` folder to help try out some requests.
> NOTE: These are **manual** requests for experimentation and are not automated tests.

## Translations

The `/i18n` folder contains a script to handle translations for QGIS processing algorithms.  

/*% } %*/
