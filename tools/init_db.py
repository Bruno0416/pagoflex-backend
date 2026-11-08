#!/usr/bin/env python3
"""Inicializa MySQL de Docker Compose sin perder bases ni tablas existentes.

Solo requiere la biblioteca estándar de Python y Docker Compose v2.
La clave administrativa debe coincidir con la del volumen MySQL existente.
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IDENTIFIER = re.compile(r"[A-Za-z][A-Za-z0-9_]*\Z")
SERVICE_KEY = re.compile(r"([A-Z][A-Z0-9_]*)_(DB|USER|PASS)\Z")
SYSTEM_DATABASES = {"mysql", "sys", "information_schema", "performance_schema"}


class InitError(RuntimeError):
    pass


@dataclass(frozen=True)
class Service:
    prefix: str
    database: str
    user: str
    password: str


def read_env(path: Path) -> dict[str, str]:
    """Lee valores dotenv sin ejecutar comandos ni expandir variables.

    Las comillas simples conservan los caracteres especiales ($, # y barras).
    Usar comillas simples cuando una contraseña contenga $.
    """
    values: dict[str, str] = {}
    # 1. leer el archivo de variables
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as exc:
        raise InitError(f"Cannot read env file: {path}") from exc
    # 2. validar el formato y las claves definidas
    for number, line in enumerate(lines, 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        match = re.fullmatch(r"(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)", line)
        if not match:
            raise InitError(f"Invalid dotenv assignment at line {number}")
        key, raw = match.groups()
        if key in values:
            raise InitError(f"Duplicate variable {key} at line {number}")
        # 3. procesar valores con o sin comillas
        single = raw.startswith("'")
        if raw.startswith(("'", '"')):
            quote = raw[0]
            chars = []
            index = 1
            while index < len(raw):
                char = raw[index]
                if char == quote:
                    break
                if quote == '"' and char == "\\":
                    index += 1
                    if index >= len(raw):
                        raise InitError(f"Unclosed quote in {key}")
                    char = {"n": "\n", "r": "\r", "t": "\t", '"': '"', "\\": "\\"}.get(raw[index], "\\" + raw[index])
                chars.append(char)
                index += 1
            suffix = raw[index + 1:].strip()
            if index == len(raw) or (suffix and not suffix.startswith("#")):
                raise InitError(f"Invalid quoted value in {key}")
            value = "".join(chars)
        else:
            value = re.split(r"\s+#", raw, maxsplit=1)[0].rstrip()
        if not single and "$" in value:
            raise InitError(f"Use single quotes for literal $ in {key}; interpolation is not supported")
        if any(c in value for c in "\x00\r\n"):
            raise InitError(f"{key} must be a single-line value without NUL")
        values[key] = value
    return values


def definitions(values: dict[str, str]) -> list[Service]:
    # 1. validar los datos de conexión administrativa
    for key in ("DB_HOST", "DB_PORT", "DB_USER", "DB_PASSWORD"):
        if not values.get(key):
            raise InitError(f"Missing or empty {key}")
    if values["DB_HOST"] != "mysql" or values["DB_PORT"] != "3306" or values["DB_USER"] != "root":
        raise InitError("This bootstrap requires Compose DB_HOST=mysql, DB_PORT=3306, DB_USER=root")
    # 2. agrupar las variables por servicio
    groups: dict[str, dict[str, str]] = {}
    for key, value in values.items():
        if key.startswith("DB_"):
            continue
        match = SERVICE_KEY.fullmatch(key)
        if match:
            prefix, kind = match.groups()
            groups.setdefault(prefix, {})[kind] = value
    result = []
    databases: set[str] = set()
    users: set[str] = set()
    # 3. verificar los nombres y las credenciales de cada servicio
    for prefix, group in sorted(groups.items()):
        if set(group) != {"DB", "USER", "PASS"} or not all(group.values()):
            raise InitError(f"{prefix} requires non-empty {prefix}_DB, {prefix}_USER and {prefix}_PASS")
        database, user = group["DB"], group["USER"]
        if not IDENTIFIER.fullmatch(database) or len(database) > 64 or database.lower() in SYSTEM_DATABASES:
            raise InitError(f"Invalid/reserved database name for {prefix}")
        if not IDENTIFIER.fullmatch(user) or len(user) > 32 or user.lower() in {"root", "mysql", "mysql_sys",
                                                                                "mysql_session", "mysql_infoschema"}:
            raise InitError(f"Invalid/reserved username for {prefix}")
        if database.casefold() in databases or user.casefold() in users:
            raise InitError(f"Duplicate database or username in {prefix}")
        databases.add(database.casefold())
        users.add(user.casefold())
        # 4. construir las definiciones de los servicios
        result.append(Service(prefix, database, user, group["PASS"]))
    if not result:
        raise InitError("No complete *_DB / *_USER / *_PASS groups found")
    return result


def literal(value: str) -> str:
    # 1. escapar las comillas para la sesión sql
    return "'" + value.replace("'", "''") + "'"


class ComposeMySQL:
    def __init__(self, env_file: Path, values: dict[str, str], timeout: int):
        self.command = ["docker", "compose", "--project-directory", str(ROOT / "infrastructure"), "--env-file",
                        str(env_file), "-f", str(ROOT / "infrastructure" / "compose.yml")]
        # 1. dar prioridad a los valores del archivo env
        self.env = {**os.environ, **values}
        self.admin_password = values["DB_PASSWORD"]
        self.timeout = timeout

    def start(self) -> None:
        # 1. iniciar mysql y esperar el healthcheck
        try:
            proc = subprocess.run(
                self.command + ["up", "-d", "--no-deps", "--wait", "--wait-timeout", str(self.timeout), "mysql"],
                env=self.env, capture_output=True, text=True, timeout=self.timeout + 120)
        except FileNotFoundError as exc:
            raise InitError("Docker with the Compose v2 plugin is required") from exc
        except subprocess.TimeoutExpired as exc:
            raise InitError("Timed out starting MySQL; applications were not started") from exc
        if proc.returncode:
            raise InitError(
                "Cannot start healthy MySQL; check Docker/Compose availability, image pull and mysql container status")

    def sql(self, sql: str, user: str = "root", password: str | None = None,
            tcp: bool = False) -> subprocess.CompletedProcess:
        secret = self.admin_password if password is None else password
        client = ["mysql", "--no-defaults", "--batch", "--binary-mode", "--raw", "--skip-column-names",
                  "--default-character-set=utf8mb4", "--connect-timeout=10"]
        client += ["--protocol=TCP", "--host=mysql", "--port=3306", "--get-server-public-key"] if tcp else [
            "--protocol=SOCKET"]
        client += ["--user=" + user]
        # 1. pasar la clave por la entrada estándar sin exponerla
        shell = 'IFS= read -r MYSQL_PWD; export MYSQL_PWD; exec "$@"'
        try:
            return subprocess.run(self.command + ["exec", "-T", "mysql", "sh", "-c", shell, "init-db", *client],
                                  input=secret + "\nSET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES';\n" + sql + "\n",
                                  env=self.env, capture_output=True, text=True, timeout=30)
        except subprocess.TimeoutExpired as exc:
            raise InitError("Timed out communicating with MySQL") from exc

    def admin(self, sql: str) -> str:
        proc = self.sql(sql)
        if proc.returncode:
            # 1. ocultar los detalles sensibles ante un error administrativo
            raise InitError(
                "MySQL administrative operation failed; verify DB_PASSWORD against the existing volume. No volume was reset")
        return proc.stdout.strip()


def bootstrap(db: ComposeMySQL, services: list[Service]) -> None:
    # 1. verificar el acceso administrativo
    db.admin("SELECT 1;")
    # 2. comprobar la configuración de mysql
    if db.admin("SELECT @@partial_revokes;") != "1":
        raise InitError("MySQL must run with partial_revokes=ON for literal database-scoped grants")
    # 3. verificar o crear cuentas y bases de cada servicio
    for service in services:
        account = literal(service.user) + "@'%'"
        exists = db.admin(
            "SELECT COUNT(*) FROM mysql.user WHERE User=" + literal(service.user) + " AND Host='%';") == "1"
        if exists:
            probe = db.sql("SELECT CURRENT_USER();", service.user, service.password, tcp=True)
            if probe.returncode:
                # 3. cambiar la clave solo ante un rechazo de autenticación
                if not re.search(r"ERROR 1045\b", probe.stderr):
                    raise InitError(f"{service.prefix}: no se pudo validar la cuenta")
                db.admin(f"ALTER USER {account} IDENTIFIED BY {literal(service.password)};")
                print(f"{service.prefix}: contraseña sincronizada")
            elif probe.stdout.strip() != service.user + "@%":
                raise InitError(f"{service.prefix}: a more-specific MySQL account shadows the service ")
            else:
                print(f"{service.prefix}: credenciales existentes validas")
        else:
            db.admin(f"CREATE USER {account} IDENTIFIED BY {literal(service.password)};")
            print(f"{service.prefix}: usuario creado")
        db.admin(
            f"CREATE DATABASE IF NOT EXISTS `{service.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
        db.admin(f"GRANT ALL PRIVILEGES ON `{service.database}`.* TO {account};")
        # 4. confirmar las credenciales efectivas
        verified = db.sql(f"USE `{service.database}`; SELECT CURRENT_USER(), DATABASE();", service.user,
                          service.password, tcp=True)
        if verified.returncode or verified.stdout.strip() != service.user + "@%\t" + service.database:
            raise InitError(f"{service.prefix}: final service connection verification failed")
        print(f"{service.prefix}: database and service connection verified")


def main(argv: list[str] | None = None) -> int:
    # 1. leer las opciones de ejecución
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, default=ROOT / "infrastructure/.env")
    parser.add_argument("--timeout", type=int, default=180, help="MySQL health wait in seconds")
    parser.add_argument("--dry-run", action="store_true",
                        help="Validate env without starting Docker or changing databases")
    args = parser.parse_args(argv)
    # 2. validar el entorno y las variables de los servicios
    try:
        if args.timeout < 1:
            raise InitError("--timeout must be positive")
        env_file = args.env_file if args.env_file.is_absolute() else ROOT / args.env_file
        values = read_env(env_file)
        services = definitions(values)
        print("Configured services: " + ", ".join(s.prefix for s in services))
        if args.dry_run:
            print("Configuration valid; Docker and databases were not changed")
            return 0
        # 3. iniciar mysql y preparar las bases
        db = ComposeMySQL(env_file, values, args.timeout)
        db.start()
        bootstrap(db, services)
        print("Initialization complete. Only MySQL was started; existing data was preserved")
        return 0
    except (InitError, OSError) as exc:
        # 4. informar errores sin exponer credenciales
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
