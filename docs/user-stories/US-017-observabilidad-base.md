# US-017 — Observabilidad mínima del flujo

**Estado:** NÚCLEO TÉCNICO

## Objetivo

Como ingeniero, quiero relacionar solicitudes y recursos técnicos, para explicar
el throughput, los conflictos y el punto de degradación.

## Señales mínimas

- Métricas: latencia/errores HTTP, reservas exitosas, conflictos, expiraciones,
  pagos por resultado y `oversold_tickets_total`.
- Métricas JVM, HikariCP y PostgreSQL disponibles con herramientas estándar.
- Logs estructurados con `traceId` e identificadores presentes; sin payloads.
- Trazas de reservar → ordenar → pagar. No se exige una plataforma concreta.

## Criterios de aceptación

1. Una compra puede correlacionarse de extremo a extremo.
2. Conflictos incrementan una métrica sin etiquetas de alta cardinalidad.
3. `oversold_tickets_total` permanece en cero.
4. Un timeout simulado muestra el tramo lento.
5. Health y readiness distinguen proceso vivo de dependencia no disponible.

## Dependencias

Se implementa incrementalmente desde US-008.
