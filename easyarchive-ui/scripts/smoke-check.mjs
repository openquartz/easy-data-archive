import fs from "node:fs";
import path from "node:path";

const root = process.cwd();

const requiredFiles = [
  "src/router/index.ts",
  "src/views/ArchiveGroupDetailView.vue",
  "src/api/archiveGroup.ts"
];

const missingFiles = requiredFiles.filter((file) => !fs.existsSync(path.join(root, file)));
if (missingFiles.length) {
  console.error("Smoke failed: missing files");
  for (const file of missingFiles) {
    console.error(`- ${file}`);
  }
  process.exit(1);
}

const routerSource = fs.readFileSync(path.join(root, "src/router/index.ts"), "utf8");
const routeFragments = [
  'path: "archive/groups/:id/detail"',
  'name: "archive-group-detail"'
];
const missingRoutes = routeFragments.filter((fragment) => !routerSource.includes(fragment));
if (missingRoutes.length) {
  console.error("Smoke failed: missing route definitions");
  for (const route of missingRoutes) {
    console.error(`- ${route}`);
  }
  process.exit(1);
}
if (routerSource.includes("ArchiveGroupDetailPlaceholder")) {
  console.error("Smoke failed: archive-group-detail route still uses placeholder component");
  process.exit(1);
}

const archiveGroupApiSource = fs.readFileSync(path.join(root, "src/api/archiveGroup.ts"), "utf8");
if (!archiveGroupApiSource.includes("getArchiveGroupOverviewApi")) {
  console.error("Smoke failed: archive group overview API function missing");
  process.exit(1);
}
if (!archiveGroupApiSource.includes("/overview")) {
  console.error("Smoke failed: archive group overview API path contract missing");
  process.exit(1);
}

console.log("Smoke passed: archive group detail file, route, and overview API contracts are present.");
