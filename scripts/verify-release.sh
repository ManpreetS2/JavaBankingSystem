#!/usr/bin/env bash
# Single local release-verification path: tests then unsigned app-image packaging.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

require_command() {
  local name="$1"
  local hint="$2"
  if ! command -v "${name}" >/dev/null 2>&1; then
    echo "ERROR: required command '${name}' was not found on PATH." >&2
    echo "${hint}" >&2
    exit 1
  fi
}

require_command mvn "Install Apache Maven 3.9+ and ensure 'mvn' is on PATH."

echo "==> Running automated tests"
mvn --batch-mode clean test

echo "==> Packaging unsigned desktop app-image"
"${ROOT}/scripts/package-app.sh"

echo "==> Release verification complete"
echo "    Tests: BUILD SUCCESS"
echo "    Package output: ${ROOT}/target/dist/"
