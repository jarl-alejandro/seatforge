# ADR-001: Monolito modular con arquitectura hexagonal

- **Estado:** Aceptado
- **Fecha:** 2026-07-25
- **Decisores:** Equipo de ingeniería de SeatForge
- **Historia relacionada:** [US-001](../user-stories/US-001-monolito-modular.md)

## Contexto

SeatForge debe recorrer un flujo de venta de entradas sometido a alta
concurrencia y, más adelante, estudiar fallos y límites de sistemas distribuidos.
Separar servicios desde el inicio introduciría red, despliegues, observabilidad y
consistencia distribuida antes de conocer los límites reales del dominio o del
sistema.

Una aplicación sin límites internos tampoco serviría: permitiría que casos de
uso, persistencia y framework se acoplaran hasta hacer costoso probar alternativas
o extraer una capacidad. Necesitamos autonomía lógica ahora sin pagar todavía el
coste de la distribución física.

## Decisión

SeatForge se implementará inicialmente como **un monolito modular**: un único
proyecto Spring Boot, proceso, build y artefacto desplegable, bajo el paquete base
`com.jarl.seatforge`.

Contendrá los módulos `identity`, `events`, `inventory`, `orders`, `payments`,
`notifications`, `audit` y `shared`. Cada capacidad de negocio se organiza con
arquitectura hexagonal en `domain`, `application` e `infrastructure`.

Adoptamos estas reglas:

1. `domain` es Java puro y no depende de Spring, JPA, HTTP, mensajería ni
   infraestructura.
2. `application` expresa casos de uso y puertos; depende del dominio, no de
   adaptadores concretos.
3. `infrastructure` contiene adaptadores reemplazables y apunta hacia los puertos
   y el dominio del mismo módulo.
4. La única API invocable por otro módulo está en `application.port.in`. Un
   consumidor no accede a dominio, casos de uso concretos, puertos de salida,
   repositorios, adaptadores ni tablas ajenas.
5. Las colaboraciones desacopladas pueden usar eventos en memoria. El contrato
   pertenece al productor, vive en su superficie `application.port.in`,
   transporta hechos inmutables y no expone entidades de persistencia.
6. `shared` es un shared kernel mínimo de primitivas técnicas estables; no aloja
   reglas ni modelos para ocultar la propiedad de una capacidad.
7. ArchUnit verifica los límites y se ejecuta en la misma suite que bloquea la
   integración continua.

El mapa normativo está en
[Módulos y dependencias permitidas](../architecture/module-dependencies.md).

## Alternativas consideradas

### Microservicios desde el inicio

Rechazada. Agrega fallos parciales, contratos remotos, despliegues y consistencia
distribuida sin evidencia de que una capacidad necesite escalar o evolucionar de
forma independiente.

### Monolito por capas técnicas globales

Rechazada. Paquetes globales como `controllers`, `services` y `repositories` no
expresan el negocio, facilitan dependencias cruzadas y dificultan localizar o
extraer una capacidad completa.

### Proyecto Gradle multi-módulo desde el inicio

Pospuesta. Puede reforzar límites en compilación, pero añade estructura antes de
que existan implementaciones suficientes. Las reglas de paquete y ArchUnit dan
una primera barrera automatizada. Se reconsiderará si el coste o la frecuencia
de violaciones demuestra que esa barrera no es suficiente.

## Consecuencias

### Positivas

- Los casos de uso y el dominio se prueban sin iniciar Spring ni adaptadores.
- Los cambios de persistencia, HTTP o proveedores no obligan a modificar reglas
  de negocio.
- Las transacciones locales permiten construir primero un flujo correcto.
- Los límites explícitos dejan una ruta de extracción sin imponerla.
- Un solo artefacto reduce el coste de operación durante el aprendizaje inicial.

### Costes y riesgos

- Todos los módulos comparten proceso, ciclo de despliegue y destino de fallo.
- ArchUnit detecta dependencias estáticas, pero no sustituye la revisión de
  propiedad de datos ni evita acoplamiento semántico.
- `shared` puede convertirse en un módulo comodín; toda ampliación requiere
  justificar consumidores y estabilidad.
- Los eventos en memoria no ofrecen durabilidad. No deben presentarse como una
  garantía equivalente a un broker.
- La extracción futura exigirá diseñar contratos remotos, observabilidad y
  consistencia que hoy no son necesarios.

## Verificación

La decisión se considera vigente mientras:

- el artefacto arranque con todos los módulos en una sola aplicación;
- la suite ArchUnit rechace dependencias de dominio a framework o infraestructura
  y accesos internos entre módulos;
- sea posible reemplazar un adaptador por un fake sin cambiar dominio o caso de
  uso;
- el diagrama de dependencias coincida con los paquetes implementados;
- CI ejecute la suite completa en cada cambio propuesto.

## Disparadores medibles para una extracción

Un módulo no se extrae por tamaño, moda o preferencia. Se abre un ADR de
extracción solo si existe una línea base reproducible y al menos uno de estos
disparadores sostenidos:

1. **Escala independiente:** pruebas de carga demuestran que la capacidad agota
   CPU, memoria, I/O o conexiones mientras el resto conserva margen, y escalar
   toda la aplicación tiene un coste material cuantificado.
2. **Aislamiento de fallos:** incidentes o experimentos controlados muestran que
   la saturación o caída de esa capacidad incumple el SLO de capacidades no
   relacionadas, incluso después de aplicar límites locales como timeouts y
   bulkheads.
3. **Cadencia y autonomía:** datos de entrega durante un periodo acordado muestran
   que el ciclo compartido bloquea despliegues independientes con frecuencia y
   coste relevantes para equipos distintos.
4. **Contención de datos:** métricas de locks, conexiones, latencia o throughput
   identifican al módulo como una fuente estable de contención que no se resuelve
   razonablemente con índices, partición lógica o cambios de transacción.
5. **Requisitos regulatorios o de seguridad:** un control verificable exige
   aislamiento de red, datos, acceso o despliegue que el proceso compartido no
   puede proporcionar.

La propuesta debe comparar antes y después con percentiles de latencia,
throughput, tasa de error, consumo de recursos, coste operativo e impacto en
SLO. También debe presupuestar el coste de red, fallos parciales, versionado de
contratos, consistencia, migración de datos, observabilidad y operación. Si el
beneficio no supera ese coste, el módulo permanece dentro del monolito.

## Notas sobre numeración

Este archivo inicia el registro real de ADR del repositorio. Los identificadores
de ADR mostrados como ejemplos futuros en el roadmap deberán asignarse de nuevo
al momento de crear cada decisión, para mantener identificadores únicos.
