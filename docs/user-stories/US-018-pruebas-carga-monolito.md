# US-018 — Ejecutar perfiles de carga reproducibles

**Tipo:** habilitador de rendimiento · **Prioridad:** P1

## Historia

Como ingeniero de rendimiento, quiero ejecutar perfiles de carga reproducibles sobre el monolito, para descubrir su punto de degradación y relacionarlo con CPU, memoria, GC, base de datos y contención.

## Perfiles

1. Estable: 100 usuarios durante 20 minutos.
2. Gradual: 100 → 500 → 1.000 → 5.000.
3. Spike: 100 → 10.000 en 10 segundos.
4. Soak: 500 usuarios durante 4 horas.

Los perfiles largos pueden ejecutarse fuera de la CI de cada commit; debe existir un smoke profile corto.

## Criterios de aceptación

1. Cada perfil define dataset, warm-up, ramp-up, duración, assertions y ambiente.
2. Se registran throughput, error rate, p50/p90/p95/p99, CPU, memoria, GC, conexiones y conflictos.
3. El informe diferencia saturación, degradación y fallo funcional.
4. Una prueba por encima del inventario no genera sobreventa.
5. Los resultados incluyen commit, configuración, versión de herramientas y fecha.
6. Los umbrales iniciales se consideran baseline revisable, no promesa de producción.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | P | Smoke en CI | Script y endpoints válidos |
| T02 | P | Carga estable | Baseline sin crecimiento anómalo |
| T03 | P | Incremento gradual | Punto de degradación identificado |
| T04 | C/P | Spike de apertura | 0 sobreventa; conflictos medidos |
| T05 | P | Soak | Fugas o estabilidad documentadas |
| T06 | F/P | Pool pequeño/saturado | Causa visible en métricas |

## Entregables

- Simulaciones Gatling versionadas.
- Dataset o generador determinista.
- Plantilla de informe de resultados.
- Dashboard o exportación de métricas correlacionada.
- Lista de experimentos posteriores: índices, paginación, N+1 y ajuste de HikariCP.

## Dependencias

US-008, US-012, US-016, US-017.

