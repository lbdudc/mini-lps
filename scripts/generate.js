const exec = require("child_process").exec;
const Os = require("os");
const mode = process.argv[2];
const noLint = process.argv.findIndex((opt) => opt === "no-lint") !== -1;
var startTime = null;

/**
 * node generate.js clean [folder = output]
 * node generate.js update product.json [folder = output]
 * node generate.js generate product.json [folder = output]
 */

let productJsonFile, outputFolder;

if (mode === "clean") {
  outputFolder = process.argv[4] || "output";
} else {
  productJsonFile = process.argv[3];
  outputFolder =
    process.argv[4] && process.argv[4] !== "no-lint"
      ? process.argv[4]
      : "output";
  outputFolder = outputFolder.replace(/"/g, "");
}

// comprobar si está fuera del current directory, que entonces no deja
if (
  mode === "clean" ||
  (mode === "generate" && !outputFolder.startsWith(".."))
) {
  console.log(`Cleaning ${outputFolder}`);
  exec(
    `del-cli ${outputFolder}/.gitlab-ci.yml ${outputFolder}/deploy/** ${outputFolder}/client/* ${outputFolder}/server/* ${outputFolder}/client/.* ${outputFolder}/server/.* !${outputFolder}/client/. !${outputFolder}/server/. !${outputFolder}/client/.. !${outputFolder}/server/.. !${outputFolder}/client/node_modules !${outputFolder}/server/gradlew`,
    cb
  );
}

setTimeout(() => {
  if (mode !== "clean") {
    startTime = Date.now();
    exec(
      `npx spl-js-engine --featureModel src/platform/model.xml --config src/platform/config.json --code src/platform/code --extra src/platform/extra.js --output ${outputFolder} --modelTransformation src/platform/transformation.js --product ${productJsonFile}`,
      cb
    );
  }
}, 500);

function cb(err, stdout, stderr) {
  if (stdout) {
    console.log(
      `${stdout.replace("\n", "")} in ${
        (Date.now() - startTime) / 1000
      } seconds`
    );
    if (!noLint) {
      console.log("\n\n######################################");
      console.log("Executing linter and prettier over output folder\n\n");
      exec(
        `vue-cli-service lint ${outputFolder}/client/`,
        (error, stdout, stderr) => {
          console.log("\n\n######################################");
          console.log("Starting... eslint");
          if (error) console.error(error);
          if (stdout) console.log(stdout);
          if (stderr) console.error(stderr);
          console.log("Finished to lint code");
        }
      );
      exec(
        `prettier --ignore-unknown --write \"${outputFolder}/client/\"`,
        (error, stdout, stderr) => {
          console.log("\n\n######################################");
          console.log("Starting... prettier");
          if (error) console.error(error);
          if (stdout) console.log(stdout);
          if (stderr) console.error(stderr);
          console.log("Finished to prettier code");
        }
      );
      if (Os.platform() == "win32") {
        exec(
          `cd ${outputFolder}/server && gradlew compileJava`,
          (error, stdout, stderr) => {
            console.log("\n\n######################################");
            console.log("Starting... compileJava");
            if (error) console.error(error);
            if (stdout) console.log(stdout);
            if (stderr) console.error(stderr);
            console.log("Finished compileJava");
          }
        );
      } else {
        exec(
          `cd ${outputFolder}/server && chmod +x gradlew && ./gradlew compileJava`,
          (error, stdout, stderr) => {
            console.log("\n\n######################################");
            console.log("Starting... compileJava");
            if (error) console.error(error);
            if (stdout) console.log(stdout);
            if (stderr) console.error(stderr);
            console.log("Finished compileJava");
          }
        );
      }
    }
  }
  if (stderr) console.log(stderr);
  if (err) {
    console.log(err);
  }
}
