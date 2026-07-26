# US-006 — Consultar catálogo e inventario

**Módulos:** Events, Inventory · **Estado:** NÚCLEO

## Objetivo

Como comprador, quiero listar eventos publicados y sus entradas, para elegir una
entrada concreta que reservar.

## Alcance

- Listado público paginado, ordenado por `startsAt,eventId`.
- Detalle de evento y listado paginado de entradas.
- Sin búsqueda por texto, filtros de recinto, disponibilidad aproximada ni
  catálogo cacheado.

## Criterios de aceptación

1. Solo aparecen eventos `PUBLISHED` y futuros.
2. `page` empieza en cero y `size` se limita a `1..100`.
3. El orden estable evita duplicados u omisiones entre páginas sin escrituras.
4. Un evento inexistente o no público devuelve `404`.
5. El DTO no expone entidades de persistencia.

## Pruebas mínimas

- Integración: visibilidad, paginación estable, límites y `404`.

## Dependencias

US-005.
