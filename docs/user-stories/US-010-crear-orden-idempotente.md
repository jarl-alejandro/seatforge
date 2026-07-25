# US-010 — Crear una orden idempotente

**Módulo:** Orders · **Prioridad:** P0

## Historia

Como comprador con una reserva vigente, quiero crear una orden una sola vez, para iniciar el pago sin generar compras duplicadas por reintentos.

## Reglas

- Una orden nace `PENDING` o `AWAITING_PAYMENT` según el caso de uso acordado.
- Solo el dueño de una reserva vigente puede crearla.
- Importe, moneda y entrada se capturan como snapshot.
- Una reserva admite como máximo una orden activa.
- Se exige `Idempotency-Key`.

## Criterios de aceptación

1. Dada una reserva vigente propia, cuando se crea la orden, entonces se almacena con total correcto.
2. Dada una reserva expirada, ajena o inexistente, entonces no se crea.
3. Dada la misma clave y payload, múltiples reintentos devuelven la misma orden.
4. Dada la misma clave con payload diferente, entonces se rechaza.
5. Dadas peticiones concurrentes sobre la misma reserva, existe como máximo una orden activa.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Calcular snapshot | Total y moneda correctos |
| T02 | I | Crear con reserva vigente | Orden persistida |
| T03 | I | Reintentar misma clave | Mismo `orderId` |
| T04 | C | 50 creaciones simultáneas | Una orden activa |
| T05 | I | Clave con payload distinto | Conflicto |

## Dependencias

US-008.

