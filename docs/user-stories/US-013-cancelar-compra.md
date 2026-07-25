# US-013 — Cancelar y compensar una compra

**Módulos:** Orders, Inventory, Payments · **Prioridad:** P0

## Historia

Como plataforma, quiero cancelar una compra fallida y ejecutar la compensación adecuada, para liberar inventario cuando sea seguro y conservar estados trazables.

## Reglas

- Pago rechazado: cancelar orden y liberar reserva.
- Pago desconocido: no liberar ni reintentar ciegamente; marcar para reconciliación.
- Orden confirmada no se cancela con este caso de uso.
- Toda compensación es idempotente.

## Criterios de aceptación

1. Dado un pago rechazado, cuando se compensa, entonces la orden queda `CANCELLED` y la entrada vuelve a `AVAILABLE`.
2. Dado un timeout con resultado desconocido, entonces no se libera automáticamente la entrada hasta reconciliar o vencer una política explícita.
3. Dada una compensación repetida, entonces no duplica cambios.
4. Dado un callback aprobado tardío sobre una orden cancelada, entonces se detecta el conflicto y se deriva a reconciliación.
5. Cada transición conserva motivo, instante y correlación.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Rechazo de pago | Cancelación permitida |
| T02 | I | Cancelar y liberar | Cambios atómicos |
| T03 | F | Resultado desconocido | Estado no destructivo |
| T04 | C | Callback tardío y cancelación | Conflicto controlado |
| T05 | I | Repetir compensación | Sin efectos adicionales |

## Dependencias

US-011, US-012.

