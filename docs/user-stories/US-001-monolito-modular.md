# US-001 — Establecer el monolito modular hexagonal

**Tipo:** habilitador arquitectónico · **Prioridad:** P0

## Historia

Como equipo de ingeniería, quiero organizar SeatForge como un monolito modular con arquitectura hexagonal y límites de dominio explícitos, para evolucionar cada capacidad sin acoplarla al framework y poder extraer módulos solo cuando las mediciones lo justifiquen.

## Valor de aprendizaje

Practicar Screaming Architecture, inversión de dependencias, DDD pragmático y verificación automática de límites.

## Alcance

- Módulos: `identity`, `events`, `inventory`, `orders`, `payments`, `notifications`, `audit` y `shared`.
- En cada módulo: `domain`, `application` e `infrastructure`.
- Puertos de entrada/salida y casos de uso nombrados con lenguaje de negocio.
- Comunicación interna mediante APIs de aplicación o eventos en memoria; nunca mediante repositorios ajenos.
- Pruebas de arquitectura con ArchUnit.

## Criterios de aceptación

1. Dado el código del monolito, cuando se ejecutan las pruebas de arquitectura, entonces el dominio no depende de Spring, JPA, HTTP ni infraestructura.
2. Dado un módulo consumidor, cuando necesita una capacidad de otro módulo, entonces usa su API pública y no clases internas ni tablas ajenas.
3. Dado un adaptador reemplazable, cuando cambia su implementación, entonces el caso de uso y el dominio no requieren cambios.
4. Dado el artefacto construido, cuando se inicia, entonces todos los módulos se despliegan como una sola aplicación.
5. Dado un paquete nuevo, cuando su nombre se inspecciona, entonces expresa una capacidad del negocio y no una agrupación genérica como `controllers/services/repositories`.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Agregado de dominio sin contexto Spring | Puede instanciarse y probarse como Java puro |
| T02 | I | Arranque del contexto completo | La aplicación inicia con todos los módulos |
| T03 | I | Sustituir un puerto por un fake | El caso de uso funciona sin adaptador real |
| T04 | I | Regla ArchUnit dominio → infraestructura | La compilación de pruebas falla ante una dependencia prohibida |
| T05 | I | Acceso directo entre repositorios de módulos | La regla automática lo rechaza |

## Entregables y evidencia

- Diagrama de módulos y dependencias permitidas.
- ADR sobre monolito modular y reglas de extracción futura.
- Pruebas ArchUnit en CI.
- README con lenguaje ubicuo inicial.

## Dependencias

Ninguna.

