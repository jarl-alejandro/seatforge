# US-014 — Notificar el resultado de compra

**Módulo:** Notifications · **Prioridad:** P1

## Historia

Como comprador, quiero recibir una notificación del resultado de mi compra, para conocer si fue confirmada, rechazada o cancelada.

## Reglas

- En esta fase se usa un adaptador simulado; no se requiere proveedor real.
- La notificación consume un evento interno después del commit.
- Un mismo evento no produce notificaciones duplicadas.
- Un fallo de notificación no revierte una compra confirmada.

## Criterios de aceptación

1. Dada una compra confirmada, después del commit se solicita una notificación con orden y evento correctos.
2. Dado un fallo del adaptador, la orden permanece confirmada y el fallo queda observable/reintentable.
3. Dado un evento duplicado, se produce como máximo una notificación lógica.
4. No se incluyen secretos ni datos sensibles.
5. El adaptador puede reemplazarse sin cambiar dominio ni caso de uso.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Construir mensaje | Contenido mínimo correcto |
| T02 | I | Evento posterior al commit | Se invoca adaptador |
| T03 | F/I | Adaptador caído | Compra intacta; error registrado |
| T04 | I | Evento duplicado | Una notificación |
| T05 | S | Inspeccionar payload | Sin información sensible |

## Dependencias

US-012, US-013.

