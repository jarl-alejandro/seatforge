# US-017 — Observar el flujo de extremo a extremo

**Módulo:** transversal · **Prioridad:** P1

## Historia

Como operador e ingeniero, quiero correlacionar solicitudes, reglas de negocio y recursos técnicos, para explicar con evidencia dónde falla o se degrada una compra.

## Señales mínimas

- Métricas: reservas, conflictos, expiraciones, órdenes, pagos, confirmaciones y sobreventa.
- Técnicas: latencia HTTP, JVM, HikariCP y consultas relevantes.
- Logs JSON con `traceId`, `spanId`, `orderId`, `eventId`, `ticketId`, `userId`, servicio y ambiente.
- Trazas del flujo reservar → ordenar → pagar → confirmar.

## Criterios de aceptación

1. Dada una compra, se puede seguir su recorrido mediante un mismo contexto de traza.
2. Dado un conflicto de reserva, aumenta exactamente una métrica etiquetada con cardinalidad acotada.
3. `oversold_tickets_total` permanece en cero en toda prueba válida.
4. Dada una compra artificialmente lenta, la traza identifica el tramo responsable.
5. Ninguna señal contiene credenciales ni datos sensibles.
6. El sistema expone health/readiness sin ejecutar consultas costosas.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | O/I | Compra completa | Traza continua |
| T02 | O/C | 100 conflictos | Contadores coherentes |
| T03 | O | Pago con 5 s | Span de pago concentra latencia |
| T04 | S/O | Escaneo de logs/trazas | Sin secretos |
| T05 | O | Caída de PostgreSQL | Health y error distinguibles |

## Dependencias

US-001; se implementa incrementalmente junto a las demás historias.

