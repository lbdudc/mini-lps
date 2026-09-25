/**
 * Saves a Blob as a file: what a download button does with data it fetched itself
 * (through the app's HTTP client, so errors and the server URL work the same as
 * everywhere else).
 */
export function saveBlob(blob, fileName) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.style.display = "none";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
