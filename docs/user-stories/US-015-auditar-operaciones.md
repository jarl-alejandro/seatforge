# US-015 — Auditar operaciones críticas

**Estado:** DIFERIDA

No se construye módulo, tabla ni API de auditoría en la primera versión. Para los
experimentos bastan logs estructurados y trazas con `eventId`, `ticketId`,
`reservationId`, `orderId`, `userId` y `traceId`, sin secretos.

Una bitácora inmutable se añadirá únicamente cuando exista un experimento sobre
retención, volumen de escritura, cumplimiento o extracción de eventos. No se
usarán 50 millones de registros sintéticos antes de obtener un baseline real.
