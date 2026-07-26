# US-008 — Reservar una entrada

**Módulo:** Inventory · **Estado:** NÚCLEO

## Objetivo

Como comprador, quiero reservar temporalmente una entrada disponible, para
competir por inventario sin que exista sobreventa.

## Reglas

- `AVAILABLE → RESERVED`; el servidor asigna propietario y TTL.
- `Idempotency-Key` es obligatorio y queda ligado al hash de la solicitud.
- Solo un comprador puede ganar una entrada.

## Criterios de aceptación

1. Una entrada disponible de evento publicado queda reservada para el comprador.
2. Entrada reservada o vendida devuelve `409` sin cambiar su propietario.
3. Misma clave y payload devuelve la misma reserva; payload distinto devuelve `409`.
4. Cien solicitudes concurrentes sobre una entrada producen un éxito y cero
   sobreventa.

## Pruebas mínimas

- Unidad: transición y TTL con reloj inyectable.
- Integración: idempotencia y restricciones.
- Concurrencia: `100 compradores / 1 entrada`.

## Dependencias

US-005, US-006.
