#!/usr/bin/env bash
# Builds an unsigned local app-image with jpackage (non-modular classpath + JavaFX modules).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APP_NAME="BankingSystem"
APP_VERSION="1.0.0"
MAIN_CLASS="com.manpreet.bank.App"
FINAL_NAME="JavaBankingSystem-1.0-SNAPSHOT"
DEST="${ROOT}/target/dist"
INPUT="${ROOT}/target/jpackage-input"
MODULE_PATH="${ROOT}/target/jpackage-modules"

echo "==> Building project and copying runtime dependencies"
mvn --batch-mode -Ppackage-app clean package -DskipTests

echo "==> Separating JavaFX modules from classpath jars"
rm -rf "${INPUT}" "${MODULE_PATH}" "${DEST}"
mkdir -p "${INPUT}/lib" "${MODULE_PATH}" "${DEST}"

cp "${ROOT}/target/${FINAL_NAME}.jar" "${INPUT}/"
shopt -s nullglob
for jar in "${ROOT}/target/app-lib"/*.jar; do
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
  --main-jar "${FINAL_NAME}.jar"
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
