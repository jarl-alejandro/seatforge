# US-005 — Publicar y cancelar un evento

**Módulo:** Events · **Prioridad:** P0

## Historia

Como organizador, quiero publicar un evento listo o cancelarlo, para controlar su disponibilidad comercial.

## Reglas

- Solo se publica un `DRAFT` futuro con aforo e inventario consistentes y al menos un precio.
- Publicar dos veces es idempotente.
- Un evento cancelado no acepta nuevas reservas.
- La cancelación no borra órdenes ni auditoría.

## Criterios de aceptación

1. Dado un borrador completo, cuando se publica, entonces cambia a `PUBLISHED` y aparece en catálogo.
2. Dado un borrador incompleto, cuando se publica, entonces permanece `DRAFT` y se informan las invariantes incumplidas.
3. Dado un evento publicado, cuando se cancela, entonces cambia a `CANCELLED` y rechaza nuevas reservas.
4. Dada la misma orden de publicación repetida, entonces el estado y efectos no se duplican.
5. Dado un usuario que no es propietario ni administrador, entonces no puede cambiar el estado.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Publicar evento completo | Transición válida |
| T02 | U | Publicar sin inventario | Transición rechazada |
| T03 | I | Publicar y consultar catálogo | Evento visible |
| T04 | I | Cancelar y reservar | Reserva rechazada |
| T05 | C | Dos publicaciones simultáneas | Un único cambio efectivo |

## Dependencias

US-004, US-007.

