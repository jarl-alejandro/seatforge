# US-016 — Comparar control de concurrencia

**Módulo:** Inventory · **Estado:** NÚCLEO EXPERIMENTAL

## Objetivo

Como ingeniero, quiero ejecutar la misma reserva con varias estrategias, para
elegir mediante datos la implementación inicial.

## Estrategias

1. `SELECT ... FOR UPDATE`.
2. Versión optimista.
3. `UPDATE ... WHERE status = 'AVAILABLE'`.

Todas implementan el mismo puerto y contrato HTTP de US-008. Solo una queda activa
por configuración; no se mezclan en producción.

## Criterios de aceptación

1. Cada estrategia produce exactamente un ganador con 100 compradores/entrada.
2. Se comparan 1, 10 y 100 contendientes antes de aumentar carga.
3. Se registran TPS, p50/p95/p99, conflictos y uso del pool con igual dataset,
   warm-up, hardware y configuración.
4. Scripts, datos crudos y comando son reproducibles.
5. Un ADR elige la estrategia y explicita el cuello de botella observado.

## Dependencias

US-008, US-017.
