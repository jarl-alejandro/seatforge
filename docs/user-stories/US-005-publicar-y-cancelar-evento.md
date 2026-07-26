# US-005 — Publicar un evento

**Módulo:** Events · **Estado:** NÚCLEO

## Objetivo

Como organizador, quiero publicar mi evento borrador, para habilitar consultas y
reservas.

## Alcance

- Única transición: `DRAFT → PUBLISHED`.
- Publicar repetidamente devuelve el mismo estado sin duplicar efectos.
- No se implementa cancelación de eventos en esta etapa.

## Criterios de aceptación

1. El propietario puede publicar un evento futuro con inventario disponible.
2. Un evento publicado aparece en catálogo y acepta reservas.
3. Otro usuario recibe `403`; un evento inexistente devuelve `404`.
4. Dos publicaciones concurrentes dejan un solo estado final válido.

## Pruebas mínimas

- Unidad: transición válida e idempotente.
- Integración/concurrencia: propiedad y dos publicaciones simultáneas.

## Dependencias

US-003.
