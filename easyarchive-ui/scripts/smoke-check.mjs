import fs from "node:fs";
import path from "node:path";

const root = process.cwd();

const requiredFiles = [
  "src/router/index.ts",
  "src/layouts/AppLayout.vue",
  "src/views/LoginView.vue",
  "src/views/DashboardView.vue",
  "src/views/DatasourceView.vue",
  "src/views/TaskListView.vue",
  "src/views/TaskDetailView.vue",
  "src/views/UserView.vue",
  "src/utils/http.ts",
  "src/api/dashboard.ts",
  "src/api/task.ts"
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
  'path: "/login"',
  'path: "/"',
  'path: "dashboard"',
  'path: "datasources"',
  'path: "tasks"',
  'path: "tasks/:taskId"',
  'path: "users"'
];
const missingRoutes = routeFragments.filter((fragment) => !routerSource.includes(fragment));
if (missingRoutes.length) {
  console.error("Smoke failed: missing route definitions");
  for (const route of missingRoutes) {
    console.error(`- ${route}`);
  }
  process.exit(1);
}

const httpSource = fs.readFileSync(path.join(root, "src/utils/http.ts"), "utf8");
if (!httpSource.includes("axios.create")) {
  console.error("Smoke failed: axios client not detected in src/utils/http.ts");
  process.exit(1);
}
if (!httpSource.includes("AUTH_EXPIRED_EVENT")) {
  console.error("Smoke failed: auth expired event contract missing in src/utils/http.ts");
  process.exit(1);
}

console.log("Smoke passed: key UI files and route/http contracts are present.");
