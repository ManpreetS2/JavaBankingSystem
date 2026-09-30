#!/usr/bin/env bash
# Builds an unsigned local app-image with jpackage (non-modular classpath + JavaFX modules).
# macOS produces BankingSystem.app; Linux/Windows produce a platform app-image directory.
# Does not sign or notarize.
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

require_command java "Install a JDK that includes the java launcher (JDK 21+ recommended)."
require_command mvn "Install Apache Maven 3.9+ and ensure 'mvn' is on PATH."
require_command jpackage "Install a full JDK that includes jpackage (not a JRE-only install)."

maven_property() {
  local expression="$1"
  local value
  value="$(mvn --batch-mode -q -DforceStdout help:evaluate -Dexpression="${expression}" 2>/dev/null | tail -n 1)"
  if [[ -z "${value}" || "${value}" == "null object or invalid expression" ]]; then
    echo "ERROR: unable to evaluate Maven expression '${expression}'." >&2
    exit 1
  fi
  printf '%s' "${value}"
}

echo "==> Reading packaging metadata from Maven"
ARTIFACT_ID="$(maven_property project.artifactId)"
PROJECT_VERSION="$(maven_property project.version)"
MAIN_CLASS="$(maven_property app.mainClass)"
APP_NAME="$(maven_property app.packageName)"
APP_VERSION="$(maven_property app.packageVersion)"
FINAL_NAME="${ARTIFACT_ID}-${PROJECT_VERSION}"
MAIN_JAR="${FINAL_NAME}.jar"

DEST="${ROOT}/target/dist"
INPUT="${ROOT}/target/jpackage-input"
MODULE_PATH="${ROOT}/target/jpackage-modules"
APP_LIB="${ROOT}/target/app-lib"

echo "    artifact : ${FINAL_NAME}"
echo "    package  : ${APP_NAME} ${APP_VERSION}"
echo "    main     : ${MAIN_CLASS}"

echo "==> Building project and copying runtime dependencies"
mvn --batch-mode -Ppackage-app clean package -DskipTests

if [[ ! -f "${ROOT}/target/${MAIN_JAR}" ]]; then
  echo "ERROR: expected packaged jar was not produced: target/${MAIN_JAR}" >&2
  exit 1
fi
if [[ ! -d "${APP_LIB}" ]]; then
  echo "ERROR: runtime dependency directory missing: target/app-lib" >&2
  exit 1
fi

echo "==> Separating JavaFX modules from classpath jars"
rm -rf "${INPUT}" "${MODULE_PATH}" "${DEST}"
mkdir -p "${INPUT}/lib" "${MODULE_PATH}" "${DEST}"

cp "${ROOT}/target/${MAIN_JAR}" "${INPUT}/"
shopt -s nullglob
for jar in "${APP_LIB}"/*.jar; do
  base="$(basename "${jar}")"
  size="$(wc -c < "${jar}" | tr -d ' ')"
  case "${base}" in
    javafx-*.jar)
      # Skip empty classifier-less stubs when platform jars are present.
      if [[ "${size}" -lt 1024 ]]; then
        continue
      fi
      cp "${jar}" "${MODULE_PATH}/"
      ;;
    *)
      cp "${jar}" "${INPUT}/lib/"
      ;;
  esac
done

if [[ -z "$(ls -A "${MODULE_PATH}" 2>/dev/null || true)" ]]; then
  echo "ERROR: no JavaFX module jars were prepared under ${MODULE_PATH}" >&2
  exit 1
fi

ICON_FLAG=()
if [[ -f "${ROOT}/src/main/resources/icons/app.icns" ]]; then
  ICON_FLAG=(--icon "${ROOT}/src/main/resources/icons/app.icns")
elif [[ -f "${ROOT}/src/main/resources/icons/app.png" ]]; then
  ICON_FLAG=(--icon "${ROOT}/src/main/resources/icons/app.png")
fi

echo "==> Running jpackage (unsigned app-image)"
JPACKAGE_CMD=(
  jpackage
  --type app-image
  --name "${APP_NAME}"
  --app-version "${APP_VERSION}"
  --vendor "JavaBankingSystem"
  --description "Portfolio desktop banking application"
  --input "${INPUT}"
  --main-jar "${MAIN_JAR}"
  --main-class "${MAIN_CLASS}"
  --dest "${DEST}"
  --module-path "${MODULE_PATH}"
  # java.se covers JDBC/logging/XML; JavaFX + unsupported for SQLite native helpers.
  --add-modules java.se,jdk.unsupported,jdk.localedata,javafx.controls,javafx.fxml
  --java-options "-Dfile.encoding=UTF-8"
)
if ((${#ICON_FLAG[@]})); then
  JPACKAGE_CMD+=("${ICON_FLAG[@]}")
fi
"${JPACKAGE_CMD[@]}"

echo "==> App image created under: ${DEST}"
ls -la "${DEST}"
