#!/bin/bash
set -e

# ---------- Cargar .env ----------
ENV_FILE="$(dirname "$0")/.env"
if [ ! -f "$ENV_FILE" ]; then
  echo "❌ No se encontró $ENV_FILE"
  exit 1
fi
set -a            # exporta automáticamente todo lo que se defina
source "$ENV_FILE"
set +a

# Validación mínima
: "${DB_NAME:?Falta DB_NAME en .env}"
: "${DB_USER:?Falta DB_USER en .env}"
: "${DB_PASS:?Falta DB_PASS en .env}"
DB_HOST="${DB_HOST:-localhost}"
DB_CHARSET="${DB_CHARSET:-utf8mb4}"
DB_COLLATION="${DB_COLLATION:-utf8mb4_unicode_ci}"

# ---------- Instalación ----------
echo "=== MariaDB ==="
sudo apt install -y mariadb-server mariadb-client
sudo service mariadb start

# Esperar a que el socket esté listo (evita el race condition)
for i in {1..30}; do
  if sudo mysqladmin ping --silent 2>/dev/null; then break; fi
  sleep 1
done

# ---------- Configuración de la base ----------
setup_db() {
  echo "=== Configurando base de datos '$DB_NAME' y usuario '$DB_USER' ==="
  # sudo mysql usa auth_socket => no necesitamos password de root
  sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`
  CHARACTER SET ${DB_CHARSET}
  COLLATE ${DB_COLLATION};

CREATE USER IF NOT EXISTS '${DB_USER}'@'${DB_HOST}' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'${DB_HOST}' IDENTIFIED BY '${DB_PASS}';

GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'${DB_HOST}';
FLUSH PRIVILEGES;
SQL

  # Carga opcional del esquema/datos iniciales
  if [ -f "./schema.sql" ]; then
    echo "=== Cargando schema.sql ==="
    sudo mysql "$DB_NAME" < ./schema.sql
  fi
}
setup_db

echo "✅ Base de datos lista: ${DB_NAME} (usuario: ${DB_USER}@${DB_HOST})"
