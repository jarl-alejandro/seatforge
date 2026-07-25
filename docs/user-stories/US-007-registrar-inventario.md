# US-007 — Generar el inventario vendible

**Módulo:** Inventory · **Prioridad:** P0

## Historia

Como organizador, quiero generar las entradas disponibles a partir del aforo configurado, para que cada unidad vendible tenga identidad y estado controlados.

## Reglas

- Estado inicial: `AVAILABLE`.
- Cada `Ticket` pertenece a un único evento y asiento.
- No se regenera inventario ya existente sin una operación administrativa explícita.
- PostgreSQL es la fuente de verdad.

## Criterios de aceptación

1. Dada una configuración válida, cuando se genera inventario, entonces existe exactamente una entrada por asiento.
2. Dado un reintento con la misma configuración, entonces no se crean duplicados.
3. Dado un fallo a mitad del lote, entonces no queda inventario parcialmente publicado.
4. Dado un evento ajeno o no modificable, entonces se rechaza la generación.
5. Dado el inventario creado, el conteo por estado coincide con el aforo.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Crear ticket | Estado `AVAILABLE` |
| T02 | I | Generar 20.000 tickets | Unicidad y conteo correctos |
| T03 | I | Repetir comando | Sin duplicados |
| T04 | F/I | Fallo durante escritura | Rollback o recuperación documentada |
| T05 | P | Escritura individual vs lote | Comparativa reproducible |

## Dependencias

US-004.

