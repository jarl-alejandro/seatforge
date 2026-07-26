# SeatForge

SeatForge es una plataforma educativa de venta de entradas para eventos de alta
demanda. El proyecto comienza como una única aplicación Spring Boot, organizada
como un **monolito modular con arquitectura hexagonal**, para aprender sobre
concurrencia, consistencia, rendimiento, resiliencia y observabilidad sin asumir
prematuramente el coste operativo de los microservicios.

## Estado del proyecto

La implementación sigue el backlog de la [Etapa 1](docs/user-stories/README.md).
La primera historia, [US-001](docs/user-stories/US-001-monolito-modular.md),
establece los límites arquitectónicos sobre los que se construirán las
capacidades de negocio posteriores.

Los ocho módulos son:

- `identity`: identidad, perfiles y autorización.
- `events`: creación, configuración, publicación y catálogo de eventos.
- `inventory`: entradas vendibles, reservas, expiración y confirmación.
- `orders`: órdenes de compra y coordinación del flujo de compra.
- `payments`: pagos simulados, resultados ambiguos y reconciliación.
- `notifications`: comunicaciones derivadas del resultado de una compra.
- `audit`: huella inmutable de operaciones críticas.
- `shared`: primitivas técnicas mínimas y estables; no contiene reglas de negocio.

La estructura objetivo parte del paquete base `com.jarl.seatforge`. Consulta el
[diagrama y las reglas de dependencia](docs/architecture/module-dependencies.md)
y el [ADR del monolito modular](docs/adr/ADR-001-monolito-modular-hexagonal.md).

## Requisitos

- JDK 25.
- Docker con Docker Compose para PostgreSQL local y pruebas de integración.
- No es necesario instalar Gradle: el repositorio incluye Gradle Wrapper.

## Ejecutar localmente

Los comandos se ejecutan desde la raíz del repositorio:

```bash
# Construir e iniciar PostgreSQL; crea la base y el usuario configurados
docker compose up -d --build postgres

# Consultar el estado del contenedor
docker compose ps postgres

# Ejecutar toda la suite, incluidas las pruebas de arquitectura
./seatforgemain/gradlew -p seatforgemain test

# Iniciar la aplicación completa
./seatforgemain/gradlew -p seatforgemain bootRun

# Construir el único artefacto desplegable
./seatforgemain/gradlew -p seatforgemain clean build

# Validar OpenAPI y generar interfaces HTTP y DTOs, sin implementaciones
./seatforgemain/gradlew -p seatforgemain generateApiContract
```

El contenedor usa por defecto la base, usuario y contraseña `seatforge`. Copia
`.env.example` a `.env` para cambiar esos valores. La imagen oficial registra el
usuario y crea la base indicada durante la primera inicialización del volumen.
Flyway aplica las migraciones al arrancar la aplicación y Hibernate solo valida
el esquema resultante. Las migraciones usan el formato obligatorio
`V###__descripcion_en_snake_case.sql`.

Para reiniciar la base desde cero en desarrollo local:

```bash
docker compose down -v
docker compose up -d --build postgres
```

El contrato fuente vive en [`docs/api/openapi.yaml`](docs/api/openapi.yaml). Las
interfaces y DTOs generados se escriben en `seatforgemain/build/generated/openapi`,
se compilan como parte del proyecto y no se versionan. El código generado define
exclusivamente la frontera HTTP; no genera controladores concretos, dominio,
casos de uso ni persistencia.

Para pruebas manuales puede importarse directamente en Postman la colección
[`docs/postman/SeatForge.postman_collection.json`](docs/postman/SeatForge.postman_collection.json).
Configura en sus variables los Client ID/Secret M2M de Auth0; la colección obtiene
y conserva temporalmente los tokens de organizador y comprador, y encadena los
identificadores creados entre las peticiones.

Para ejecutar desde IntelliJ usa la configuración compartida
`SeatForgeApplication (.env)`. Esta configuración carga `.env` como única fuente
local de `AUTH0_ISSUER` y `AUTH0_AUDIENCE`; evita definir nuevamente esas
variables en la configuración temporal porque los valores explícitos tienen
precedencia sobre el archivo.

La autorización Auth0 usa los permisos RBAC del claim OAuth estándar `scope`
(texto separado por espacios). SeatForge también acepta el claim `permissions`
cuando está habilitado en Auth0. Un token con scopes de comprador y organizador
expone ambos roles, pero cada ruta continúa exigiendo su permiso concreto.

La integración continua ejecuta la misma suite mediante
[GitHub Actions](.github/workflows/ci.yml). Una regla ArchUnit rota debe hacer
fallar el mismo job que las demás pruebas; no existe una vía de construcción que
omita las pruebas de arquitectura.

## Lenguaje ubicuo inicial

| Término | Significado en SeatForge |
|---|---|
| **Evento** | Experiencia comercial que un organizador prepara, publica o cancela y que ocurre en una fecha determinada. |
| **Organizador** | Actor que crea y administra sus eventos. |
| **Comprador** | Actor que reserva una entrada y completa una compra. |
| **Zona** | Agrupación de asientos o cupos de un evento que comparte condiciones comerciales. |
| **Asiento** | Lugar identificable dentro del aforo de un evento. |
| **Entrada** (`Ticket`) | Unidad vendible del inventario. `Entrada` es el término funcional; `Ticket` puede aparecer en nombres técnicos ya establecidos. |
| **Inventario** | Conjunto de entradas que pueden reservarse y venderse para un evento. |
| **Reserva** | Tenencia temporal y exclusiva de una entrada por un comprador; expira si la compra no se completa. |
| **Orden** | Registro del intento de compra que conserva el importe y la entrada acordados. |
| **Pago** | Resultado de solicitar el cobro simulado de una orden; puede ser aprobado, rechazado o desconocido. |
| **Compra confirmada** | Resultado terminal en el que la orden está confirmada y la entrada está vendida. |
| **Compensación** | Acción que revierte de forma segura efectos de una compra que no puede completarse. |
| **Reconciliación** | Resolución explícita de un resultado ambiguo, sin reintentar cobros ni liberar entradas a ciegas. |
| **Notificación** | Comunicación no bloqueante sobre el resultado de la compra. |
| **Registro de auditoría** | Evidencia inmutable y correlacionable de una operación crítica. |

Estados y roles se expresan en el código con nombres en inglés (`DRAFT`,
`PUBLISHED`, `AVAILABLE`, `RESERVED`, `SOLD`, `BUYER`, `ORGANIZER`, `ADMIN`),
pero conservan el significado definido por este lenguaje de negocio.

## Guardrails arquitectónicos

1. Se construye y despliega **una sola aplicación** con todos los módulos.
2. El código se organiza primero por capacidad de negocio, no en paquetes
   globales como `controllers`, `services` o `repositories`.
3. Dentro de un módulo, `infrastructure` puede depender de `application` y
   `domain`; `application` puede depender de `domain`; `domain` no depende de
   Spring, JPA, HTTP ni infraestructura.
4. Un módulo solo consume otro módulo mediante su superficie pública en
   `application.port.in`, que también aloja los contratos de eventos de
   integración. Nunca accede a su repositorio, adaptador, modelo interno o
   tablas.
5. Los contratos de eventos pertenecen al módulo que los publica; `shared` solo
   define la abstracción técnica mínima de evento y no se convierte en un
   contenedor de modelos de negocio compartidos.
6. Las dependencias prohibidas se validan automáticamente con ArchUnit y forman
   parte de la suite de CI.
7. Un módulo se extraerá únicamente cuando mediciones y necesidades operativas
   lo justifiquen; no por preferencia tecnológica.

## Documentación

- [Roadmap técnico](ROADMAP.md)
- [Backlog de historias](docs/user-stories/README.md)
- [Dependencias permitidas entre módulos](docs/architecture/module-dependencies.md)
- [ADR-001: monolito modular hexagonal](docs/adr/ADR-001-monolito-modular-hexagonal.md)
- [Licencia](LICENSE)
