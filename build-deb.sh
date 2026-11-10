#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# build-deb.sh
# Compila el proyecto con Maven y genera un .deb con jpackage.
# Requisitos: JDK 17+ (con jpackage), Maven, dpkg-deb (viene en Ubuntu).
# ============================================================

APP_NAME="Cerrajeria"
APP_VERSION="1.0.0"
MAIN_CLASS="com.cerrajeria.Launcher"
JAR_NAME="lista-precios-1.0.0.jar"

# Rutas
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="${ROOT_DIR}/target"
INPUT_DIR="${TARGET_DIR}/app"
DIST_DIR="${TARGET_DIR}/dist"
ICON_PATH="${ROOT_DIR}/src/main/resources/icono.png"
ENV_FILE="${ROOT_DIR}/.env"

echo "==> Limpiando y compilando con Maven..."
mvn -f "${ROOT_DIR}/pom.xml" clean package

# El maven-dependency-plugin ya deja las dependencias en target/app.
# Pero nos aseguramos de que el JAR principal también esté ahí.
if [[ ! -f "${INPUT_DIR}/${JAR_NAME}" ]]; then
    echo "==> Copiando JAR principal a ${INPUT_DIR}..."
    cp "${TARGET_DIR}/${JAR_NAME}" "${INPUT_DIR}/"
fi

# === MAGIA PARA EL .ENV ===
# Si existe un .env en la raíz del proyecto, lo copiamos a la carpeta de entrada
# para que viaje dentro del .deb instalado.
if [[ -f "${ENV_FILE}" ]]; then
    echo "==> Copiando archivo .env al paquete..."
    cp "${ENV_FILE}" "${INPUT_DIR}/.env"
else
    echo "==> Aviso: No se encontró el archivo .env en la raíz. La app podría fallar al conectar a la BD."
fi
# ==========================

# Verificamos que jpackage esté disponible
if ! command -v jpackage &>/dev/null; then
    echo "ERROR: 'jpackage' no está en el PATH. Instalá un JDK 17+ (por ej. sudo apt install openjdk-17-jdk)."
    exit 1
fi

# Armamos el comando de jpackage
JPACKAGE_CMD=(
    jpackage
    --type deb
    --name "${APP_NAME}"
    --app-version "${APP_VERSION}"
    --input "${INPUT_DIR}"
    --main-jar "${JAR_NAME}"
    --main-class "${MAIN_CLASS}"
    --dest "${DIST_DIR}"
    --linux-shortcut
    --linux-menu-group "Utility"
    --linux-deb-maintainer "tu-email@ejemplo.com"
    # Esta línea es la que hace que la app lea el .env de su propia carpeta
    --java-options "-Duser.dir=\$APPDIR" 
)

# El icono es opcional: si no existe, jpackage usa uno por defecto.
if [[ -f "${ICON_PATH}" ]]; then
    JPACKAGE_CMD+=(--icon "${ICON_PATH}")
else
    echo "==> Aviso: no se encontró ${ICON_PATH}. Se usará el icono por defecto."
fi

echo "==> Generando paquete .deb con jpackage..."
mkdir -p "${DIST_DIR}"
"${JPACKAGE_CMD[@]}"

echo ""
echo "✅ Listo. El .deb está en: ${DIST_DIR}/"
ls -lh "${DIST_DIR}/"
