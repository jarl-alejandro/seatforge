# US-016 — Comparar estrategias de control de concurrencia

**Módulo:** Inventory · **Prioridad:** P0

## Historia

Como ingeniero de plataforma, quiero ejecutar la misma reserva con bloqueo pesimista, optimista y actualización atómica, para elegir una estrategia mediante evidencia y no intuición.

## Hipótesis

Las tres estrategias preservarán la integridad, pero diferirán en throughput, latencia, conflictos, uso del pool y contención bajo distintos niveles de competencia.

## Estrategias

1. `SELECT ... FOR UPDATE`.
2. Control optimista con versión.
3. `UPDATE ... WHERE status = 'AVAILABLE'`.

Cada estrategia implementa el mismo puerto y conserva la semántica de US-008.

## Criterios de aceptación

1. Dada cualquiera de las estrategias, 1.000 compradores sobre una entrada producen exactamente un ganador.
2. Dadas cargas de 10, 100 y 1.000 usuarios concurrentes, se capturan throughput, p50/p95/p99, conflictos, conexiones activas/pendientes y errores.
3. Las pruebas usan el mismo hardware, dataset, warm-up y perfil.
4. Los resultados pueden repetirse mediante un comando documentado.
5. Un ADR selecciona la estrategia inicial, explica límites y conserva los datos crudos.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | C | 10 usuarios/1 ticket por estrategia | 1 ganador |
| T02 | C/P | 100 usuarios/1 ticket | Métricas comparables |
| T03 | C/P | 1.000 usuarios/1 ticket | 0 sobreventa |
| T04 | C/P | Baja contención, muchos tickets | Throughput comparado |
| T05 | F | Timeout/rollback del ganador | Estado recuperable |
| T06 | O | Contar conflictos | Métrica coincide con resultados |

## Evidencia obligatoria

- Scripts y configuración de carga versionados.
- CSV/JSON crudo y resumen en Markdown.
- Versión de JVM, PostgreSQL, esquema y tamaño del pool.
- ADR de selección; no se acepta “parece más rápido”.

## Dependencias

US-008, US-017.

