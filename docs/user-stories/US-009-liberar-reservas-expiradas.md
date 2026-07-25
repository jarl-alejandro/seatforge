# US-009 — Liberar reservas expiradas

**Módulo:** Inventory · **Prioridad:** P0

## Historia

Como plataforma, quiero liberar reservas que superaron su tiempo límite, para recuperar inventario sin vender y evitar bloqueos indefinidos.

## Reglas

- Transición válida: `RESERVED → AVAILABLE` por expiración.
- Una entrada `SOLD` nunca se libera.
- El proceso es reentrante e idempotente.
- La decisión usa tiempo del servidor inyectable para pruebas.

## Criterios de aceptación

1. Dada una reserva expirada, cuando corre el liberador, entonces queda disponible y se limpian sus datos de reserva.
2. Dada una reserva vigente, entonces no cambia.
3. Dada una confirmación concurrente, entonces la entrada termina `SOLD` o `AVAILABLE` según una única transición válida, nunca en un estado corrupto.
4. Dado un reinicio durante el lote, entonces la siguiente ejecución continúa de forma segura.
5. Cada ejecución registra revisadas, liberadas, conflictos, duración y errores.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Reloj posterior a expiración | Se libera |
| T02 | U | Reserva aún vigente | No cambia |
| T03 | I | Lote mixto | Solo expiran las elegibles |
| T04 | C | Expirar mientras se confirma | Una transición ganadora válida |
| T05 | F | Fallo y reejecución | Sin doble efecto |

## Dependencias

US-008.

