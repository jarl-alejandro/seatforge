# US-012 — Confirmar compra e inventario

**Módulos:** Orders, Inventory · **Prioridad:** P0

## Historia

Como comprador con pago aprobado, quiero que mi compra y entrada queden confirmadas, para obtener una orden final consistente.

## Reglas

- Transiciones: `RESERVED → SOLD` y orden → `CONFIRMED`.
- Solo se confirma la reserva asociada, vigente y del mismo comprador.
- En el monolito, orden e inventario comparten una transacción local bien delimitada.
- Repetir la confirmación no duplica efectos.

## Criterios de aceptación

1. Dado un pago aprobado y reserva válida, cuando se confirma, entonces ticket y orden quedan terminales.
2. Dado un pago no aprobado, entonces se rechaza la confirmación.
3. Dada una reserva expirada, entonces la orden no se marca confirmada y queda en un estado recuperable documentado.
4. Dado un fallo de persistencia entre ambos cambios, entonces la transacción revierte completa.
5. Dadas confirmaciones repetidas o concurrentes, entonces existe una sola venta.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Confirmar agregado válido | Estados terminales correctos |
| T02 | I | Confirmación completa | Commit atómico |
| T03 | F/I | Fallo tras actualizar ticket | Orden y ticket revertidos |
| T04 | C | 20 confirmaciones iguales | Un único efecto |
| T05 | C | Expiración contra confirmación | Sin `CONFIRMED` con ticket disponible |

## Dependencias

US-009, US-011.

