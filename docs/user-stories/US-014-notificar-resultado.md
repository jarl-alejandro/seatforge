# US-014 — Notificar el resultado

**Estado:** DIFERIDA

No se implementan correo, SMS, adaptador simulado ni endpoint de notificaciones.
No modifican la integridad ni el TPS del flujo crítico inicial y añadirían
deduplicación y reintentos sin una necesidad medida.

Será un consumidor útil cuando se estudien outbox y mensajería; hasta entonces el
resultado de la operación se observa en la respuesta, consulta de orden y logs.
