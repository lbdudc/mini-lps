const fs = require("fs");
const archiver = require("archiver");

if (!fs.existsSync("./dist")) {
  fs.mkdirSync("./dist");
}

const output = fs.createWriteStream("dist/platform.zip");
const archive = archiver("zip", {
  zlib: { level: 9 },
});

output.on("close", function () {
  console.log(archive.pointer() + " total bytes");
  console.log(
    "archiver has been finalized and the output file descriptor has closed."
  );
});

archive.on("error", function (err) {
  throw err;
});

archive.on("warning", function (err) {
  throw err;
});

archive.pipe(output);
archive.glob("**/*", {
  cwd: "src/platform",
  dot: true,
});

archive.finalize();
