b  # US-018 — Medir carga del monolito

**Estado:** NÚCLEO EXPERIMENTAL

## Objetivo

Como ingeniero de rendimiento, quiero aumentar carga desde un baseline pequeño,
para localizar saturación y distinguirla de errores funcionales.

## Perfiles

1. **Smoke CI:** 1–5 usuarios, menos de un minuto.
2. **Baseline:** 10 usuarios durante 5 minutos.
3. **Escalones:** duplicar usuarios (`10, 20, 40...`) hasta incumplir el umbral.
4. **Contención:** 1, 10 y 100 compradores sobre una entrada.

Spike y soak largos se diseñan después de conocer el baseline; no se fijan ahora
cifras arbitrarias de 5.000 o 10.000 usuarios.

## Criterios de aceptación

1. Cada ejecución registra commit, entorno, dataset, warm-up y configuración.
2. Captura TPS, errores, p50/p95/p99, CPU, memoria, GC, pool y conflictos.
3. La prueba se detiene ante errores funcionales u `oversold_tickets_total > 0`.
4. El informe identifica el primer recurso saturado y propone un siguiente
   experimento, no una optimización por intuición.
5. Script, generador de datos y resultados crudos quedan versionados o enlazados.

## Dependencias

US-008, US-011, US-016, US-017.
