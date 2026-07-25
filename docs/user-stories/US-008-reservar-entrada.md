# US-008 — Reservar temporalmente una entrada

**Módulo:** Inventory · **Prioridad:** P0

## Historia

Como comprador, quiero reservar temporalmente una entrada disponible, para disponer de un tiempo limitado en el que completar la compra sin que otro comprador la adquiera.

## Reglas

- Transición válida: `AVAILABLE → RESERVED`.
- La reserva registra comprador, instante de creación y expiración.
- Solo una reserva activa puede poseer una entrada.
- El servidor determina el tiempo; no confía en el reloj del cliente.
- Una petición repetida con la misma clave idempotente devuelve el mismo resultado.

## Criterios de aceptación

1. Dada una entrada disponible de un evento publicado, cuando se reserva, entonces queda `RESERVED` para ese comprador hasta una expiración futura.
2. Dada una entrada ya reservada o vendida, cuando otro comprador intenta reservarla, entonces se responde conflicto y no cambia el propietario.
3. Dada una entrada inexistente o de evento cancelado, entonces se rechaza.
4. Dadas solicitudes concurrentes, entonces como máximo una obtiene la reserva.
5. Dada una repetición con la misma clave y payload, entonces retorna el resultado original; con payload diferente, se rechaza.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Reservar disponible | Estado, propietario y TTL correctos |
| T02 | U | Reservar vendida | Error de dominio |
| T03 | I | Persistir reserva | Versión y tiempos correctos |
| T04 | C | 100 compradores/1 entrada | 1 éxito, 99 conflictos, 0 sobreventa |
| T05 | C/P | 1.000 compradores/1 entrada | Integridad y métricas registradas |
| T06 | I | Reusar clave con otro payload | `409` o `422` documentado |

## Dependencias

US-005, US-007.

