# US-003 — Crear un evento vendible

**Módulos:** Events, Inventory · **Estado:** NÚCLEO

## Objetivo

Como organizador, quiero crear un evento borrador con su precio y aforo, para
obtener en una sola operación el dataset mínimo que después será sometido a carga.

## Alcance

- Entrada: `name`, `startsAt`, `price` y `capacity`.
- Moneda única de laboratorio: `USD`.
- El evento nace `DRAFT`; se generan entradas numeradas `1..capacity` en estado
  `AVAILABLE` dentro de la misma transacción.
- No hay recinto, zona horaria, zonas, mapas, importación ni edición.

## Criterios de aceptación

1. Datos válidos crean un evento propio y exactamente `capacity` entradas.
2. Fecha no futura, precio no positivo o capacidad fuera de `1..100000` se rechaza.
3. Un fallo durante la escritura revierte evento e inventario completos.
4. La respuesta devuelve `eventId`, estado y cantidad creada.

## Pruebas mínimas

- Unidad: invariantes de fecha, precio y capacidad.
- Integración: conteo/unicidad y rollback transaccional.

## Dependencias

US-001, US-002.
