# GEMA LPS

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
