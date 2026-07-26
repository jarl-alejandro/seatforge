# US-002 — Autenticación local mínima

**Módulo:** Identity · **Estado:** NÚCLEO

## Objetivo

Como autor de pruebas, quiero usar identidades fijas de comprador y organizador,
para ejercer autorización y propiedad sin construir un sistema de usuarios.

## Alcance

- La configuración local define tokens opacos de laboratorio asociados a
  `userId` y rol `BUYER` u `ORGANIZER`.
- No hay registro, login, refresh token, perfiles, `ADMIN` ni proveedor OIDC.
- Nunca se registran tokens.

## Criterios de aceptación

1. Un token configurado propaga `userId` y rol al caso de uso.
2. Token ausente o inválido responde `401`; rol incorrecto responde `403`.
3. Un organizador no modifica eventos ajenos y un comprador no publica eventos.
4. La solución puede sustituirse después por OIDC sin cambiar el dominio.

## Pruebas mínimas

- Integración: `401`, `403`, operación permitida y propiedad ajena.
- Seguridad: tokens ausentes de logs y respuestas.

## Dependencias

US-001.
