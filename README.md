<div align="center">

# PagoFlex - Backend

Backend de **PagoFlex** desarrollado con Java 21, Spring Boot 4.1.2 y Maven.

<p>
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-555555?style=for-the-badge&amp;logo=openjdk&amp;logoColor=white&amp;labelColor=ED8B00">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring-Boot-555555?style=for-the-badge&amp;logo=springboot&amp;logoColor=white&amp;labelColor=6DB33F">
  <img alt="Maven" src="https://img.shields.io/badge/Apache-Maven-555555?style=for-the-badge&amp;logo=apachemaven&amp;logoColor=white&amp;labelColor=C71A36">
</p>

<p>
  <img alt="Docker Compose" src="https://img.shields.io/badge/Docker-Compose-555555?style=for-the-badge&amp;logo=docker&amp;logoColor=white&amp;labelColor=2496ED">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-Database-555555?style=for-the-badge&amp;logo=mysql&amp;logoColor=white&amp;labelColor=4479A1">
  <img alt="Python 3" src="https://img.shields.io/badge/Python-3-555555?style=for-the-badge&amp;logo=python&amp;logoColor=white&amp;labelColor=3776AB">
</p>

<p><strong>Integrantes</strong></p>

<p>
  <a href="https://github.com/Bruno0416">Bruno Valladares</a>
  &nbsp;·&nbsp;
  <a href="https://github.com/Adonysjr">Adonys Maceo</a>
</p>

<p>
  <a href="#estructura-del-repositorio"><strong>Estructura</strong></a> ·
  <a href="#estado-de-los-servicios"><strong>Estado</strong></a> ·
  <a href="#requisitos-previos"><strong>Requisitos</strong></a> ·
  <a href="#inicializar-mysql"><strong>MySQL</strong></a> ·
  <a href="#iniciar-los-servicios"><strong>Servicios</strong></a> ·
  <a href="#health-checks"><strong>Health checks</strong></a> ·
  <a href="#tests"><strong>Tests</strong></a>
</p>

</div>

---

## Estructura del repositorio

```text
pagoflex-backend/
├── libs/
│   ├── core/                # excepciones y utilidades compartidas
│   ├── contracts/           # contratos entre microservicios
│   ├── security/            # autenticacion JWT y seguridad interna
│   └── web/                 # manejo comun de errores HTTP
├── services/
│   ├── api-gateway/         # entrada HTTP y validacion de JWT
│   ├── usuarios-service/    # autenticacion, sesiones y perfiles
│   ├── email-service/       # envio interno de correos
│   ├── reconocimientos-service/ # logros por historial de pagos
│   └── eureka-server/       # descubrimiento de servicios
├── infrastructure/
│   ├── compose.yml         # contenedores y red interna
│   └── .env.example        # ejemplo de variables de entorno
├── tools/
│   └── init_db.py          # preparacion de bases de datos y usuarios MySQL
└── pom.xml                 # proyecto Maven raiz
```

## Estado de los servicios

| Servicio | Estado | Alcance |
|---|---|---|
| `api-gateway` | Implementado | Entrada HTTP, enrutamiento, validación JWT y protección de rutas internas. |
| `eureka-server` | Implementado | Registro y descubrimiento de microservicios. |
| `usuarios-service` | Implementado | Registro, autenticación JWT, sesiones en Redis, recuperación de contraseña, perfil, avatar y consultas internas de personas. |
| `email-service` | Implementado | Envío interno de correos y plantillas de correo. |
| `reconocimientos-service` | Implementado; integración pendiente | Reconocimientos por primer pago a tiempo y tres meses al día; consulta hechos de pago de Cobranza a través del contrato interno. |
| `cobranza-service` | **No implementado** | Empresas, compromisos, cuotas, pagos y comprobantes. |
| `soporte-service` | **No implementado** | Reportes, asignaciones y seguimiento de casos. |
| `mensajeria-service` | **No implementado** | Mensajes asociados a la atención de casos. |

Los servicios del compañero todavía no están integrados en este repositorio. Los contratos compartidos en `libs/contracts` están preparados para la integración con Cobranza, pero **el funcionamiento completo entre servicios requiere realizar y validar ese merge**. El RUT se almacena sin puntos y con guion, por ejemplo `12345678-5`.

El registro de agentes y ejecutivos usa un código de empresa. Su validación depende de la implementación compatible del contrato en Cobranza.

## Requisitos previos

- Docker y Docker Compose.
- Python 3 para el script de inicialización de MySQL.
- Java 21 y Maven (o el wrapper `./mvnw`) si se desea compilar o ejecutar pruebas fuera de Docker.

## Inicializar MySQL

Primero, prepara las variables de entorno desde la raíz del repositorio:

```bash
cp infrastructure/.env.example infrastructure/.env
```

Edita `infrastructure/.env` con las credenciales y parámetros de tu entorno.

Después, el siguiente script crea o verifica la base de datos y el usuario de cada servicio configurado:

```bash
python3 tools/init_db.py --dry-run
python3 tools/init_db.py
```

La primera orden revisa el entorno sin efectuar cambios; la segunda prepara MySQL y las credenciales de los servicios configurados. **El script no carga los datos del prototipo SQLite ni crea las tablas de negocio.** Actualmente Hibernate administra la creación o actualización de tablas en `usuarios-service` y `reconocimientos-service` mediante `spring.jpa.hibernate.ddl-auto: update`. La estrategia definitiva de esquema y datos se definirá después del merge.

## Iniciar los servicios

Desde la raíz del repositorio:

```bash
docker compose --env-file infrastructure/.env -f infrastructure/compose.yml up -d --build
```

## Health checks

Se conserva Spring Boot Actuator con el endpoint `/actuator/health`. Permite revisar el estado básico de cada servicio
sin agregar endpoints adicionales de métricas o administración. El health del gateway puede consultarse localmente así:

```bash
curl http://localhost:8080/actuator/health
```

Los demás health checks permanecen accesibles solo donde la configuración de red y seguridad lo permita, sin rutas
públicas adicionales en el gateway.

## Tests

Con Java 21 y Maven disponibles:

```bash
bash ./mvnw -B clean verify
```

Para probar solo los módulos modificados y sus dependencias:

```bash
bash ./mvnw -B -pl services/usuarios-service,services/reconocimientos-service -am test
```
