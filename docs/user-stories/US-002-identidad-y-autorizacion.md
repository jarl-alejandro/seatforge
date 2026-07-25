# US-002 — Integrar identidad y autorización

**Módulo:** Identity · **Prioridad:** P0

## Historia

Como usuario de SeatForge, quiero autenticarme mediante un proveedor de identidad y operar según mi rol, para que compradores, organizadores y administradores solo accedan a las capacidades permitidas.

## Reglas

- Roles: `BUYER`, `ORGANIZER`, `ADMIN`.
- La aplicación valida tokens; no implementa su propio proveedor de identidad.
- Los identificadores externos se traducen a un `UserId` del dominio.
- Nunca se registran tokens ni secretos.

## Criterios de aceptación

1. Dado un token válido, cuando se invoca un endpoint permitido, entonces la identidad se propaga al caso de uso.
2. Dado un token ausente, inválido o expirado, cuando se invoca un endpoint protegido, entonces se responde `401`.
3. Dado un comprador, cuando intenta crear o publicar eventos, entonces se responde `403`.
4. Dado un organizador, cuando modifica un evento ajeno, entonces la operación se rechaza.
5. Dado un administrador, cuando ejecuta una operación administrativa soportada, entonces queda auditada.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Traducir claims válidos | Se obtiene `UserId` y roles |
| T02 | S/I | Token expirado | `401`, sin ejecutar el caso de uso |
| T03 | S/I | Rol incorrecto | `403` |
| T04 | S/I | Organizador modifica evento ajeno | Acceso denegado |
| T05 | O | Inspeccionar logs de autenticación fallida | No contienen token ni claims sensibles |

## Dependencias

US-001.

