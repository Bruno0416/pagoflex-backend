#!/usr/bin/env bash
set -Eeuo pipefail

# PagoFlex - inicialización idempotente de MySQL.
# Crea bases, sincroniza credenciales y asigna permisos.
# Nunca elimina bases, tablas o registros.

: "${DB_USER:?Falta DB_USER}"
: "${DB_PASSWORD:?Falta DB_PASSWORD}"

export MYSQL_PWD="$DB_PASSWORD"

DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"

mysql_admin() {
    mysql \
        --protocol=tcp \
        --host="$DB_HOST" \
        --port="$DB_PORT" \
        --user="$DB_USER" \
        --batch \
        --skip-column-names \
        "$@"
}

sql_quote() {
    printf '%s' "$1" | sed \
        -e 's/\\/\\\\/g' \
        -e "s/'/''/g"
}

valid_identifier() {
    [[ "$1" =~ ^[a-zA-Z][a-zA-Z0-9_]*$ ]]
}

init_database() {
    local prefix="$1"
    local db_key="${prefix}_DB"
    local user_key="${prefix}_USER"
    local pass_key="${prefix}_PASS"

    local database="${!db_key:-}"
    local username="${!user_key:-}"
    local password="${!pass_key:-}"

    if [[ -z "$database" || -z "$username" || -z "$password" ]]; then
        echo "[db-init] ERROR: configuración incompleta: $prefix" >&2
        exit 1
    fi

    if ! valid_identifier "$database" || ! valid_identifier "$username"; then
        echo "[db-init] ERROR: identificador SQL inválido: $prefix" >&2
        exit 1
    fi

    if [[ "${#username}" -gt 32 || "$username" == root ]]; then
        echo "[db-init] ERROR: usuario SQL no permitido: $prefix" >&2
        exit 1
    fi

    if [[ "$password" == *$'\n'* || "$password" == *$'\r'* ]]; then
        echo "[db-init] ERROR: contraseña con saltos de línea" >&2
        exit 1
    fi

    local user_sql password_sql
    user_sql="$(sql_quote "$username")"
    password_sql="$(sql_quote "$password")"

    echo "[db-init] Inicializando $database ..."

    # No mostrar sentencias SQL: contienen contraseñas.
    if ! mysql_admin >/dev/null 2>&1 <<SQL
CREATE DATABASE IF NOT EXISTS \`$database\`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '$user_sql'@'%'
    IDENTIFIED WITH caching_sha2_password BY '$password_sql';

ALTER USER '$user_sql'@'%'
    IDENTIFIED WITH caching_sha2_password BY '$password_sql';

GRANT ALL PRIVILEGES ON \`$database\`.*
    TO '$user_sql'@'%';
SQL
    then
        echo "[db-init] ERROR: falló la inicialización de $database" >&2
        exit 1
    fi

    # Verificar con las credenciales reales de cada microservicio.
    if ! MYSQL_PWD="$password" mysql \
        --protocol=tcp \
        --host="$DB_HOST" \
        --port="$DB_PORT" \
        --user="$username" \
        --database="$database" \
        --execute="SELECT 1" >/dev/null 2>&1; then
        echo "[db-init] ERROR: conexión del usuario de $prefix rechazada" >&2
        exit 1
    fi

    echo "[db-init] OK: $database"
}

echo "[db-init] Verificando conexión administrativa..."

if ! mysql_admin --execute="SELECT 1" >/dev/null 2>&1; then
    echo "[db-init] ERROR: no se pudo autenticar como root." >&2
    echo "[db-init] Revisa DB_USER, DB_PASSWORD y los permisos del volumen existente." >&2
    exit 1
fi

init_database USUARIOS
init_database COBRANZA
init_database RECONOCIMIENTOS
init_database SOPORTE
init_database MENSAJERIA

echo "[db-init] Inicialización completada correctamente."
