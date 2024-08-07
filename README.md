# Mini LPS

![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)
![Node.js Version](https://img.shields.io/badge/node-%3E%3D%2012.0.0-brightgreen.svg)

## Description

This project is a tool to generate a product from a specification. The product is a web application that includes a web client and a backend. The web client is a Vue application that uses Leaflet to display maps and interacts with the backend to get the data. The backend is a SpringBoot application that serves the data to the client.

## Usage

* Install deps: `npm install`
* Generate product from specification:
  * Cleaning the previous files: `npm run generate products/spec.json [output folder (default 'output')]`
  * Maintaining the previous files: `npm run update products/spec.json [output folder (default 'output')]`
* Clean an output folder: `npm run clean [output folder (default 'output')]`
  * Only works when output folder is inside the current folder
  * Removes every file except the folder `node_modules` (to prevent having to install it again)

It is possible to prevent the linter and prettier execution over the generated code adding the argument *no-lint*
at the end of the generating/updating command. Example: `npm run generate products/spec.json no-lint`
  
### Automatic execution of the linter before a commit

In order for the linter to run automatically, it is necessary to install the dependencies in the root folder.
To achieve this, it is necessary:

* Go to the root directory.
* Execute the command `npm install`

This will cause that, before a commit is executed, the linter applies the styles automatically in all those files that were modified.

## Product specification

### Extra options

* client_deploy_url: full URL where the web client will be accessible, including protocol, domain, port (if needed) and subdomains. URL should not finish with slash (/)
* geoserver_url: full URL to the GeoServer instance
* server_deploy_url: full URL without protocol where the backend will be accessible, including domain, port (if needed) and subdomains. URL should not finish with slash (/)
* server_deploy_port: port where the backed will be launched on. This port does not need to match any part of the server URL, but it is a local port

Default values:

```
{
  "basicData": {
    "extra": {
      "client_deploy_url": "http://localhost:1234",
      "client_deploy_port": "1234",
      "geoserver_url": "http://localhost:9001/geoserver",
      "server_deploy_url": "localhost:8080",
      "server_deploy_port": "8080"
    }
  }
}
```

## Feature model

A visual representation of the Featore Model can be found in the file `src/platform/model.png`.

 ![Image of the FM](./src/platform/model.png)

## Authors

| Name                   | Email                       |
| ---------------------- | --------------------------- |
| David De Castro        | <david.decastro@udc.es>     |
| Alejandro Cortiñas     | <alejandro.cortinas@udc.es> |
| Victor Lamas           | <victor.lamas@udc.es>       |
| María Isabel Limaylla  | <maria.limaylla@udc.es>     |

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
