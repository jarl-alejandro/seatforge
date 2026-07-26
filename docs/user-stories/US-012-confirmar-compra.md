# US-012 — Confirmar compra e inventario

**Estado:** ABSORBIDA EN US-011

No se expone un segundo endpoint para confirmar. En un monolito con una sola base
de datos, separar `pagar` y `confirmar` crea estados y coordinación innecesarios.

US-011 realiza pago aprobado, orden `CONFIRMED` y entrada `SOLD` en una única
transacción local. Esta frontera será revisada al extraer `payments` o `inventory`,
momento en que outbox, idempotencia distribuida y saga sí serán objetivos reales.
