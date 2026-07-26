# US-009 — Liberar reservas expiradas

**Módulo:** Inventory · **Estado:** NÚCLEO TÉCNICO

## Objetivo

Como plataforma, quiero liberar reservas vencidas en lotes pequeños, para
recuperar inventario y estudiar carreras con la confirmación.

## Alcance

- Job interno periódico; no expone endpoint de negocio.
- `RESERVED → AVAILABLE` solo cuando `expiresAt <= now`.
- Lotes configurables, reentrantes e idempotentes; reloj inyectable.

## Criterios de aceptación

1. Libera solo reservas expiradas y nunca una entrada `SOLD`.
2. Un reinicio permite reejecutar sin doble efecto.
3. Una carrera expiración/confirmación termina en `AVAILABLE` o `SOLD`, nunca en
   un estado intermedio ni en orden confirmada con entrada disponible.
4. Registra duración, candidatas, liberadas y conflictos.

## Pruebas mínimas

- Unidad con reloj fijo, integración por lote y prueba concurrente con confirmación.

## Dependencias

US-008.
