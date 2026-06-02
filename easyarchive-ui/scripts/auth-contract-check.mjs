import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const authApiPath = path.join(root, "src/api/auth.ts");
const authApiSource = fs.readFileSync(authApiPath, "utf8");

const expectations = [
  {
    fragment: 'http.post<LoginResponse>("auth/login", payload)',
    message: "loginApi must use a relative auth/login path so axios baseURL /api/v1 is preserved."
  },
  {
    fragment: 'http.post<void>("auth/logout")',
    message: "logoutApi must use a relative auth/logout path so axios baseURL /api/v1 is preserved."
  },
  {
    fragment: 'http.post<UserProfile>("auth/me")',
    message: "meApi must POST to auth/me to match the backend controller contract."
  }
];

const failures = expectations.filter(({ fragment }) => !authApiSource.includes(fragment));

if (failures.length > 0) {
  console.error("Auth contract check failed:");
  for (const failure of failures) {
    console.error(`- ${failure.message}`);
  }
  process.exit(1);
}

console.log("Auth contract check passed.");
