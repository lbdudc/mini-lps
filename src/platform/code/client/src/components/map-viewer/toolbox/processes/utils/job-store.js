/*% if (feature.MV_Processes) { %*/
import properties from "@/properties";

/*
 * Persistence for the toolbox job history: one list per app, in the browser's
 * localStorage. Every access is guarded, because storage can be blocked (private
 * windows, site data disabled) or full; the history then only lives in memory.
 */

const MAX_JOBS = 100;

/* fields that only make sense while the page is open */
const TRANSIENT_FIELDS = ["loading", "hidden"];

const jobsKey = () => `gema:toolbox:jobs:v1:${properties.APP_NAME}`;
const cleanupKey = () => `gema:toolbox:cleanup:v1:${properties.APP_NAME}`;

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    const value = JSON.parse(raw);
    return Array.isArray(value) ? value : fallback;
  } catch (e) {
    return fallback;
  }
}

function write(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (e) {
    /* history stays in memory only */
  }
}

export function storageKey() {
  return jobsKey();
}

export function loadJobs() {
  return read(jobsKey(), []).filter((job) => job && job.jobID);
}

export function saveJobs(jobs) {
  const newest = [...jobs]
    .sort((a, b) => String(b.created).localeCompare(String(a.created)))
    .slice(0, MAX_JOBS)
    .map((job) => {
      const record = { ...job };
      TRANSIENT_FIELDS.forEach((field) => delete record[field]);
      return record;
    });
  write(jobsKey(), newest);
}

/* processing environments (QGIS projects) whose deletion had to wait for a job */
export function loadPendingCleanup() {
  return read(cleanupKey(), []);
}

export function savePendingCleanup(ids) {
  write(cleanupKey(), ids);
}

/* a job that will not change any more */
export function isFinalStatus(status) {
  return ["successful", "failed", "dismissed", "expired"].includes(status);
}
/*% } %*/
