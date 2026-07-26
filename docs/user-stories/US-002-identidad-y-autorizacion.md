# US-002 — Identidad y autorización mínima con Auth0

**Módulo:** Identity · **Estado:** NÚCLEO

## Objetivo

Como autor de pruebas, quiero usar dos identidades técnicas fijas, comprador y
organizador, emitidas por Auth0, para ejercer autenticación, autorización y
propiedad sin construir registro, login ni gestión de usuarios.

## Alcance

- SeatForge se registra en Auth0 como una API OAuth 2.0 con un `audience`
  propio y tokens de acceso JWT firmados con `RS256`.
- Dos aplicaciones **Machine to Machine** representan a los actores de
  laboratorio mediante Client Credentials:
  - `seatforge-organizer-lab`;
  - `seatforge-buyer-lab`.
- El claim `sub` del token se propaga como identificador estable del actor.
- Auth0 tiene RBAC habilitado y añade al access token los permisos concedidos:
  - organizador: `create:events`, `publish:events`;
  - comprador: `reserve:tickets`, `create:orders`, `read:orders`, `pay:orders`.
- El adaptador de seguridad traduce `sub` y permisos a una identidad interna
  con rol `ORGANIZER` o `BUYER`. Los casos de uso y el dominio no conocen JWT,
  Auth0 ni Spring Security.
- No hay usuarios humanos, registro, pantalla de login, refresh tokens,
  perfiles ni rol `ADMIN`.
- Client secrets y access tokens solo se suministran mediante configuración
  externa; nunca se almacenan en Git ni se registran en logs o respuestas.
- Las pruebas automatizadas validan tokens controlados localmente y no dependen
  de la disponibilidad de Auth0 ni solicitan tokens reales.

## Criterios de aceptación

1. Un access token válido propaga su `sub` y el rol derivado de sus permisos al
   caso de uso.
2. La API valida firma, algoritmo, expiración, `issuer` y `audience` antes de
   aceptar un token.
3. Token ausente, malformado, expirado, con firma inválida, `issuer` incorrecto
   o `audience` incorrecto responde `401`.
4. Un token válido sin el permiso requerido responde `403`.
5. El organizador puede crear y publicar eventos; el comprador puede reservar,
   crear y consultar sus órdenes y solicitar el pago.
6. Un comprador no crea ni publica eventos, y un organizador no ejecuta
   operaciones reservadas al comprador.
7. Un organizador no modifica eventos pertenecientes a otro `sub`, y un
   comprador no accede a órdenes pertenecientes a otro `sub`.
8. La identidad que reciben los casos de uso es propia de SeatForge; sustituir
   Auth0 por otro emisor OAuth 2.0 compatible no exige cambiar el dominio.
9. Client secrets, access tokens y el header `Authorization` no aparecen en
   logs, mensajes de error ni cuerpos de respuesta.

## Pruebas mínimas

- Integración de seguridad:
  - petición permitida con token de organizador;
  - petición permitida con token de comprador;
  - token ausente o inválido produce `401`;
  - permiso insuficiente produce `403`;
  - recurso perteneciente a otro actor produce `403`.
- Validación JWT: firma, expiración, `issuer` y `audience` incorrectos.
- Arquitectura: dominio y casos de uso no dependen de Auth0, JWT ni Spring
  Security.
- Seguridad: secretos, tokens y `Authorization` están ausentes de logs y
  respuestas.
- Determinismo: la suite completa se ejecuta sin acceder a Auth0 ni a Internet.

## Configuración externa requerida

- `AUTH0_ISSUER`: URL del emisor, incluido el esquema y `/` final.
- `AUTH0_AUDIENCE`: identificador de la API registrada en Auth0.
- Los Client IDs y Client Secrets pertenecen únicamente a clientes locales,
  scripts o herramientas de carga que solicitan tokens; el resource server no
  los necesita para validar peticiones.

## Fuera de alcance

- Crear una Regular Web Application, SPA o interfaz de login.
- Administrar usuarios, roles o permisos de Auth0 desde SeatForge.
- Usar Auth0 Management API.
- Validar identidad exclusivamente en API Gateway/APIM.
- Diseñar todavía audiencias o credenciales independientes por microservicio.

## Dependencias

US-001.
