# US-007 — Generar inventario vendible

**Estado:** ABSORBIDA EN US-003

No habrá comando ni endpoint separado para generar inventario. La separación
permitía reintentos, regeneraciones e importaciones que no necesita el laboratorio.

US-003 crea el evento y sus entradas de forma atómica. Se conserva en base de
datos la restricción única `(event_id, number)` porque sí es relevante para
integridad, concurrencia y futuros experimentos de escritura por lotes.
