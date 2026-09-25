FROM node:20-alpine
WORKDIR /app
# .mjs, not .js: gispublisher/src/templates/import.mjs (staged here by
# import-util.js's copyGeographicDataForImport) uses ESM `import` syntax and
# this directory has no package.json to set "type":"module" for a plain .js
# file -- without the extension, `node import.js` throws "Cannot use import
# statement outside a module" and this container exits 1 immediately, leaving
# the generated app's database empty.
COPY import.mjs .
# ./data is not copied: docker-compose.yml mounts it at /app/data, so the (large) data
# never enters the image or the build context's cache key.
CMD ["node", "import.mjs"]
