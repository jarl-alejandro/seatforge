# US-011 — Simular pago y completar compra

**Módulos:** Payments, Orders, Inventory · **Estado:** NÚCLEO

## Objetivo

Como comprador, quiero simular el pago de mi orden, para completar el flujo y
provocar aprobación, rechazo o timeout de forma reproducible.

## Alcance

- El request elige `APPROVED`, `DECLINED` o `TIMEOUT`; esto es una API de
  laboratorio, no un proveedor real.
- `Idempotency-Key` es obligatorio. No existen tarjetas ni datos bancarios.
- `APPROVED` cambia pago a aprobado, orden a `CONFIRMED` y entrada a `SOLD` en
  una transacción local.
- `DECLINED` deja orden `DECLINED` y libera la entrada.
- `TIMEOUT` responde `504` sin mutar estados; no se modela reconciliación aún.

## Criterios de aceptación

1. Aprobación confirma exactamente una orden y una entrada.
2. Rechazo libera la entrada y no puede terminar como venta.
3. Timeout artificial es configurable y observable.
4. Un reintento con misma clave no duplica efectos; otra solicitud para una orden
   terminal devuelve su resultado terminal.
5. Un fallo entre cambios revierte la transacción completa.

## Pruebas mínimas

- Integración por escenario, rollback e idempotencia.
- Concurrencia: veinte pagos iguales generan un solo efecto terminal.

## Dependencias

US-009, US-010.
