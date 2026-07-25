# US-003 — Crear un evento

**Módulo:** Events · **Prioridad:** P0

## Historia

Como organizador, quiero crear un evento en estado borrador, para preparar su venta sin hacerlo visible prematuramente.

## Reglas

- Un evento nace en `DRAFT`.
- Requiere nombre, recinto, zona horaria, fecha futura y propietario.
- El dinero se representa con importe y moneda; las fechas se almacenan como instantes UTC.
- El identificador se genera una sola vez y no se reutiliza.

## Criterios de aceptación

1. Dado un organizador autenticado y datos válidos, cuando crea un evento, entonces se persiste en `DRAFT` y se devuelve su identificador.
2. Dada una fecha no futura, cuando se intenta crear, entonces se rechaza con error de dominio.
3. Dados campos obligatorios vacíos, cuando se envía la solicitud, entonces no se persiste nada.
4. Dado un fallo de persistencia, cuando termina la solicitud, entonces no queda un agregado parcial.
5. Dado un evento creado, entonces se emiten métricas y auditoría con `eventId` y `organizerId`.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Crear con valores válidos | Evento `DRAFT` |
| T02 | U | Fecha pasada | Excepción de dominio |
| T03 | U | Nombre vacío | Validación fallida |
| T04 | I | Persistir y recuperar | Conserva invariantes y zona horaria |
| T05 | I | Error de base de datos | Transacción revertida |

## Dependencias

US-001, US-002.

