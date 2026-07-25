# US-004 — Configurar zonas, asientos y precios

**Módulos:** Events, Inventory · **Prioridad:** P0

## Historia

Como organizador, quiero definir zonas, asientos y precios de un evento borrador, para establecer su aforo e inventario comercial.

## Reglas

- Un código de asiento es único dentro del evento.
- El precio debe ser positivo y usar una moneda soportada.
- La capacidad declarada coincide con la suma de asientos vendibles.
- Un evento publicado no admite cambios estructurales en esta fase.

## Criterios de aceptación

1. Dado un evento propio en `DRAFT`, cuando se agrega una zona válida, entonces queda asociada al evento.
2. Dado un código de asiento repetido, cuando se importa, entonces se rechaza la operación sin duplicados parciales.
3. Dado un precio cero, negativo o moneda inválida, entonces se rechaza.
4. Dado un lote válido, cuando se registra, entonces la cantidad final coincide con el aforo.
5. Dado un evento publicado, cuando se intenta alterar aforo o precio, entonces se rechaza.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Crear zona y precio válidos | Value objects válidos |
| T02 | U | Precio negativo | Error de dominio |
| T03 | I | Importar lote con duplicado | Rollback completo |
| T04 | I | Importar 20.000 asientos | Conteo e IDs correctos |
| T05 | P | Medir importación por lote | Evidencia de tiempo, memoria y escrituras |

## Dependencias

US-003.

