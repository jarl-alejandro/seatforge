# US-011 — Procesar un pago simulado

**Módulo:** Payments · **Prioridad:** P0

## Historia

Como comprador, quiero procesar un pago simulado de mi orden, para completar el flujo y estudiar latencia, timeouts, reintentos y resultados ambiguos sin usar tarjetas reales.

## Reglas

- Resultados configurables: aprobado, rechazado, timeout y error.
- Nunca se reciben ni persisten datos reales de tarjeta.
- Cada solicitud exige idempotencia y guarda hash de request, resultado y estado.
- “Resultado desconocido” es distinto de “rechazado”.

## Criterios de aceptación

1. Dada una orden pendiente, cuando el simulador aprueba, entonces registra un único pago aprobado.
2. Cuando rechaza, la orden puede iniciar compensación y no se confirma inventario.
3. Dada una respuesta perdida tras procesar, cuando se reintenta con la misma clave, entonces se devuelve el resultado original sin doble cobro.
4. Dada la misma clave con importe distinto, entonces se rechaza.
5. La latencia y tasa de fallos son configurables en entorno de prueba y visibles en métricas.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Aprobar pago | Resultado persistible |
| T02 | I | Reintento tras respuesta perdida | Un cargo lógico |
| T03 | F/I | Timeout antes de conocer resultado | Estado `UNKNOWN` o equivalente |
| T04 | C | Callbacks/reintentos simultáneos | Un resultado terminal |
| T05 | O | Latencia artificial de 5 s | Traza identifica el tiempo |

## Dependencias

US-010.

