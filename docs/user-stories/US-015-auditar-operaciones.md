# US-015 — Auditar operaciones críticas

**Módulo:** Audit · **Prioridad:** P1

## Historia

Como administrador, quiero consultar una huella inmutable de operaciones críticas, para investigar concurrencia, fallos y cambios de estado.

## Reglas

- Registra actor, acción, recurso, estado anterior/posterior, instante, origen y `traceId`.
- Se auditan publicación/cancelación, reserva/liberación, pago y confirmación.
- La auditoría no contiene tokens, tarjetas ni secretos.
- Los registros no se actualizan ni borran desde la API funcional.

## Criterios de aceptación

1. Dada una operación crítica exitosa, existe un registro correlacionable.
2. Dado un intento rechazado relevante, se registra el motivo sin datos sensibles.
3. Dado un fallo de auditoría, la política de consistencia está documentada y probada.
4. Solo administradores pueden consultar auditoría.
5. Las consultas son paginadas y permiten filtrar por recurso, actor y rango temporal.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | I | Reservar y confirmar | Secuencia auditable |
| T02 | S | Comprador consulta auditoría | `403` |
| T03 | S/O | Buscar secretos en registros | Ninguno presente |
| T04 | I | Filtrar por `ticketId`/`orderId` | Solo coincidencias |
| T05 | P | Consultar con 50 millones sintéticos | Plan y latencia documentados |

## Dependencias

US-002 y los casos de uso auditados.

