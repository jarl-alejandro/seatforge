# US-013 — Cancelar y compensar una compra

**Estado:** DIFERIDA

Cancelaciones posteriores, reembolsos, callbacks tardíos y reconciliación no son
necesarios para medir el primer flujo. El rechazo síncrono mínimo se resuelve en
US-011 liberando la entrada.

Esta historia se retomará al introducir fallos entre servicios en las etapas de
resiliencia y saga; implementarla ahora simularía complejidad distribuida dentro
de una transacción local.
