# US-010 — Crear una orden idempotente

**Módulo:** Orders · **Estado:** NÚCLEO

## Objetivo

Como comprador, quiero crear una orden desde mi reserva vigente, para iniciar un
pago sin duplicar órdenes durante reintentos.

## Reglas

- La orden nace `PENDING` y captura entrada, precio `USD` y comprador.
- Una reserva admite una sola orden.
- `Idempotency-Key` es obligatorio.

## Criterios de aceptación

1. Solo el propietario de una reserva vigente puede crear la orden.
2. Misma clave y payload devuelve el mismo `orderId`; payload distinto, `409`.
3. Solicitudes concurrentes sobre una reserva crean como máximo una orden.
4. La orden puede consultarse únicamente por su propietario.

## Pruebas mínimas

- Integración: expiración, propiedad e idempotencia.
- Concurrencia: `50 solicitudes / 1 reserva`.

## Dependencias

US-008.
