#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[1/3] Backend compile check"
(
  cd "$ROOT_DIR"
  mvn -pl easyarchive-starter -am test -DskipTests
)

echo "[2/3] Backend API contract tests"
(
  cd "$ROOT_DIR"
  mvn -pl easyarchive-starter -Dtest=DashboardControllerContractTest,DashboardSecurityContractTest test
)

echo "[3/3] Frontend smoke check"
(
  cd "$ROOT_DIR/easyarchive-ui"
  npm run smoke
)

echo "Preflight checks passed."
