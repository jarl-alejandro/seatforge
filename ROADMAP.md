
# Proyecto central: plataforma de venta de entradas para eventos de alta demanda

Construirás una plataforma similar a Ticketmaster para vender entradas de conciertos, partidos y eventos con aforo limitado.

El sistema deberá soportar escenarios como este:

> Se habilita la venta de 20.000 entradas y 500.000 usuarios intentan comprar durante los primeros minutos.

Este proyecto es especialmente útil porque convierte problemas abstractos de arquitectura en problemas inevitables:

* No puedes vender el mismo asiento dos veces.
* No puedes mantener bloqueada una entrada indefinidamente.
* No puedes confiar únicamente en transacciones locales.
* No puedes escalar agregando pods sin pensar en concurrencia.
* No puedes consultar constantemente PostgreSQL sin saturarlo.
* No puedes asumir que Kafka, Redis, un servicio externo o la red siempre estarán disponibles.
* No puedes optimizar lo que no puedes medir.

El sistema comenzará como una aplicación sencilla y terminará como una plataforma distribuida, observable, resiliente y sometida a pruebas de carga reales.

---

# Arquitectura objetivo

```text
                         ┌─────────────────────┐
                         │ API Gateway / APIM  │
                         │ Auth / Rate Limit   │
                         └──────────┬──────────┘
                                    │
             ┌──────────────────────┼──────────────────────┐
             │                      │                      │
             ▼                      ▼                      ▼
    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
    │ Event Service   │    │ Inventory       │    │ Order Service   │
    │ Eventos         │    │ Asientos/stock  │    │ Compras         │
    └────────┬────────┘    └────────┬────────┘    └────────┬────────┘
             │                      │                      │
             │                      ▼                      ▼
             │               ┌─────────────┐       ┌──────────────┐
             │               │ Redis       │       │ Payment      │
             │               │ Cache/Locks │       │ Service      │
             │               └─────────────┘       └──────┬───────┘
             │                                             │
             └──────────────────────┬──────────────────────┘
                                    ▼
                           ┌─────────────────┐
                           │ Kafka           │
                           │ Eventos dominio │
                           └────────┬────────┘
                                    │
                 ┌──────────────────┼─────────────────┐
                 ▼                  ▼                 ▼
       ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
       │ Notification    │ │ Analytics       │ │ Audit Service   │
       │ Service         │ │ Service         │ │ Trazabilidad    │
       └─────────────────┘ └─────────────────┘ └─────────────────┘
```

No debes comenzar con todos estos componentes. Primero construirás un monolito modular y lo romperás únicamente cuando exista una razón técnica medible.

Ese enfoque es importante: un arquitecto no divide sistemas porque “microservicios es mejor”, sino porque los límites, la escala o la autonomía justifican la separación.

---

# División propuesta de servicios

## 1. Identity Service

Responsable de:

* Registro e inicio de sesión.
* Roles: comprador, organizador y administrador.
* Emisión y validación de tokens.
* Gestión básica de perfiles.

Tecnologías:

* Spring Boot.
* Spring Security.
* OAuth 2.0 / OpenID Connect.
* Keycloak inicialmente.
* PostgreSQL.

No construyas tu propio proveedor de identidad. El aprendizaje relevante será integrar correctamente seguridad, autorización y propagación de identidad.

---

## 2. Event Service

Responsable de:

* Crear eventos.
* Definir escenarios, zonas, precios y fechas.
* Publicar o cancelar eventos.
* Consultar el catálogo disponible.

Este servicio tendrá principalmente operaciones de lectura, por lo que será ideal para aprender:

* Índices.
* Paginación.
* Caché.
* Read replicas.
* Invalidación de caché.
* Consultas optimizadas.

---

## 3. Inventory Service

Es el corazón técnico del proyecto.

Responsable de:

* Mantener el inventario de entradas.
* Reservar temporalmente asientos.
* Liberar reservas expiradas.
* Confirmar entradas después del pago.
* Evitar sobreventa.

Estados posibles:

```text
AVAILABLE → RESERVED → SOLD
                │
                └────────→ AVAILABLE
                    expiración
```

Aquí enfrentarás:

* Condiciones de carrera.
* Bloqueos optimistas.
* Bloqueos pesimistas.
* Contención de base de datos.
* Locks distribuidos.
* Idempotencia.
* Particionamiento.
* Consistencia eventual.

---

## 4. Order Service

Responsable de:

* Crear la orden.
* Mantener el estado de la compra.
* Coordinar inventario y pago.
* Manejar reintentos.
* Evitar órdenes duplicadas.

Estados:

```text
PENDING
RESERVING
AWAITING_PAYMENT
PAID
CONFIRMED
CANCELLED
FAILED
```

Este servicio será el lugar ideal para implementar una Saga.

---

## 5. Payment Service

Primero será un simulador. Más adelante actuará como adaptador hacia un proveedor externo ficticio.

Responsable de:

* Solicitar pagos.
* Recibir callbacks.
* Simular latencia, timeouts y errores.
* Aplicar idempotencia.
* Gestionar reembolsos.

Nunca almacenes datos reales de tarjetas.

Este componente te permitirá trabajar con sistemas externos que:

* Responden tarde.
* Responden dos veces.
* Cobran, pero el cliente no recibe la respuesta.
* Devuelven errores intermitentes.
* Aplican límites de consumo.

---

## 6. Notification Service

Responsable de:

* Confirmaciones de compra.
* Avisos de reserva.
* Cancelaciones.
* Recordatorios.

Consumirá eventos desde Kafka.

Es un excelente servicio para aprender:

* Consumidores idempotentes.
* Dead Letter Queues.
* Reintentos.
* Procesamiento asíncrono.
* Garantías de entrega.
* Outbox Pattern.

---

## 7. Audit Service

Responsable de registrar:

* Quién realizó una operación.
* Qué recurso fue modificado.
* Estado anterior y posterior.
* Trace ID.
* Fecha y origen.

No debe bloquear la operación principal. Consumirá eventos asíncronos.

---

## 8. Analytics Service

Consumirá eventos como:

```text
TicketReserved
TicketSold
PaymentApproved
PaymentRejected
OrderCancelled
```

Permitirá calcular:

* Ventas por minuto.
* Conversión.
* Abandono de reservas.
* Tiempo promedio de compra.
* Eventos con mayor demanda.

Aquí puedes experimentar con agregaciones, procesamiento de eventos y almacenamiento orientado a lectura.

---

# Roadmap práctico

## Etapa 1: monolito modular correcto

Construye inicialmente una sola aplicación Spring Boot con módulos claramente separados:

```text
com.worldticket
 ├── identity
 ├── events
 ├── inventory
 ├── orders
 ├── payments
 ├── notifications
 └── shared
```

Usa arquitectura hexagonal dentro de cada módulo:

```text
inventory
 ├── domain
 │    ├── model
 │    ├── service
 │    └── event
 ├── application
 │    ├── port.in
 │    ├── port.out
 │    └── usecase
 └── infrastructure
      ├── web
      ├── persistence
      └── messaging
```

## Desafíos

Debes implementar:

* Crear evento.
* Registrar asientos.
* Reservar entrada.
* Pagar.
* Confirmar compra.
* Liberar reservas expiradas.

## Cómo romperlo

Crea una entrada disponible y ejecuta 100 solicitudes concurrentes intentando reservarla.

Una implementación ingenua hará algo parecido a esto:

```java
Ticket ticket = repository.findById(ticketId).orElseThrow();

if (ticket.isAvailable()) {
    ticket.reserve(userId);
    repository.save(ticket);
}
```

Probablemente varios usuarios conseguirán reservar el mismo asiento.

## Aprendizaje obligatorio

Debes probar al menos tres soluciones:

1. Bloqueo pesimista con `SELECT FOR UPDATE`.
2. Bloqueo optimista con `@Version`.
3. Actualización atómica:

```sql
UPDATE ticket
SET status = 'RESERVED',
    reserved_by = :userId
WHERE id = :ticketId
  AND status = 'AVAILABLE';
```

Después compara:

* Throughput.
* Latencia.
* Número de conflictos.
* Uso de conexiones.
* Contención.
* Comportamiento con 10, 100 y 1.000 usuarios concurrentes.

No elijas una solución por intuición. Elige basándote en mediciones.

---

# Etapa 2: comprender la base de datos

En esta etapa PostgreSQL será deliberadamente tu cuello de botella.

## Datos de prueba

Genera:

* 100.000 eventos.
* 20 millones de entradas.
* 5 millones de órdenes.
* 50 millones de registros de auditoría.

Puedes usar Testcontainers, scripts SQL o un generador Java.

## Cómo romperlo

Crea consultas sin índices:

```sql
SELECT *
FROM ticket
WHERE event_id = ?
  AND status = 'AVAILABLE'
ORDER BY price;
```

Ejecuta carga concurrente y observa cómo aumenta el tiempo de respuesta.

## Trabajo obligatorio

Aprende a utilizar:

```sql
EXPLAIN ANALYZE
```

Analiza:

* Sequential Scan.
* Index Scan.
* Bitmap Index Scan.
* Coste estimado frente al tiempo real.
* Filas estimadas frente a filas reales.
* Sort en memoria frente a disco.

Crea índices como:

```sql
CREATE INDEX idx_ticket_event_status_price
ON ticket(event_id, status, price);
```

Después evalúa si realmente mejora la consulta.

## Desafíos adicionales

* Implementa paginación con `OFFSET`.
* Demuestra por qué se vuelve lenta en páginas profundas.
* Reemplázala por keyset pagination.
* Reduce consultas N+1.
* Compara `JOIN FETCH`, Entity Graph y proyecciones.
* Configura HikariCP incorrectamente y provoca agotamiento del pool.
* Ajusta el pool basándote en el límite real de conexiones de PostgreSQL.

Una regla importante:

> Más conexiones no significan automáticamente más rendimiento.

Un pool demasiado grande puede aumentar la competencia por CPU, memoria y bloqueos en PostgreSQL.

---

# Etapa 3: caché y consistencia

Incorpora Redis para acelerar:

* Catálogo de eventos.
* Detalle de eventos.
* Disponibilidad aproximada.
* Configuraciones.
* Rate limiting.

## Patrón inicial

Implementa Cache-Aside:

```text
1. Consultar Redis.
2. Si existe, retornar.
3. Si no existe, consultar PostgreSQL.
4. Guardar en Redis con TTL.
5. Retornar.
```

## Cómo romperlo

Elimina la caché y ejecuta 10.000 consultas por minuto al evento más popular.

Luego activa Redis y compara:

* Latencia p50, p95 y p99.
* CPU del servicio.
* CPU de PostgreSQL.
* Número de conexiones.
* Cache hit ratio.

## Problemas que debes provocar

### Cache stampede

Expira una clave muy popular cuando miles de usuarios están consultándola.

Todos llegarán simultáneamente a PostgreSQL.

Debes experimentar con:

* TTL con jitter.
* Single-flight.
* Locks temporales.
* Refresh ahead.
* Caché stale-while-revalidate.

### Datos obsoletos

Actualiza el precio de un evento, pero no invalidez la caché.

Define qué nivel de inconsistencia es aceptable para:

* Nombre del evento.
* Precio.
* Disponibilidad.
* Estado de una entrada.

No todo dato admite la misma estrategia.

La disponibilidad exacta de un asiento no debería depender exclusivamente de una caché eventualmente consistente.

---

# Etapa 4: separación del Inventory Service

Extrae el módulo de inventario como primer microservicio.

Es el mejor candidato porque:

* Tiene reglas de concurrencia particulares.
* Requiere escalado diferente.
* Su carga cambia abruptamente.
* Necesita independencia frente al catálogo.

## Primer error deliberado

Haz que Order Service llame de forma síncrona a Inventory Service:

```text
Order → Inventory → Payment → Notification
```

Después introduce:

* 2 segundos de latencia en Payment.
* 10 % de respuestas con error.
* 5 % de timeouts.
* Caída completa del servicio.

Observarás propagación de fallos y agotamiento de threads.

## Trabajo obligatorio

Configura timeouts explícitos:

* Connection timeout.
* Read timeout.
* Write timeout.
* Timeout total de la operación.

Una llamada sin timeout es una espera potencialmente infinita desde la perspectiva operativa.

---

# Etapa 5: resiliencia

Usa Resilience4j.

Implementa:

* Circuit Breaker.
* Retry.
* Time Limiter.
* Bulkhead.
* Rate Limiter.

## Desafíos

### Reintentos peligrosos

Haz que Payment Service procese correctamente el pago, pero pierda la respuesta.

Order Service realizará un retry.

Sin idempotencia, cobrarás dos veces.

Implementa:

```http
Idempotency-Key: 2a0d8c67-...
```

Payment Service deberá guardar:

```text
idempotency_key
request_hash
response
status
created_at
```

La misma clave con el mismo payload debe devolver el mismo resultado.

La misma clave con un payload diferente debe ser rechazada.

### Circuit Breaker

Haz que Payment falle durante dos minutos.

Observa cómo cada llamada intenta llegar al servicio y consume recursos.

Después configura un Circuit Breaker y mide:

* Fallos evitados.
* Latencia durante la caída.
* Tiempo de recuperación.
* Número de llamadas en estado half-open.

### Bulkhead

Configura Payment con una latencia extrema.

Sin aislamiento, puede consumir todos los threads y afectar incluso consultas simples.

Separa pools para evitar que un proveedor lento derribe toda la aplicación.

---

# Etapa 6: Kafka y consistencia eventual

Introduce Kafka para eventos asíncronos.

Order Service publicará:

```text
OrderCreated
PaymentApproved
PaymentRejected
OrderConfirmed
OrderCancelled
```

Notification y Analytics consumirán estos eventos.

## Primer fallo deliberado

Guarda la orden en PostgreSQL y luego publica en Kafka:

```java
orderRepository.save(order);
kafkaTemplate.send("orders", event);
```

Provoca una caída entre ambas operaciones.

Resultado:

* La orden existe.
* El evento nunca fue publicado.
* Los demás servicios no se enteran.

## Solución: Transactional Outbox

Guarda dentro de la misma transacción:

```text
orders
outbox_events
```

Después un publicador independiente enviará los eventos pendientes a Kafka.

Puedes comenzar con polling y después probar Debezium CDC.

## Segundo fallo deliberado

Haz que Notification Service procese un evento correctamente, pero falle antes de confirmar el offset.

Kafka lo entregará nuevamente.

El consumidor debe ser idempotente.

Ejemplo:

```sql
CREATE TABLE processed_events (
    consumer_name VARCHAR(100),
    event_id UUID,
    processed_at TIMESTAMP,
    PRIMARY KEY (consumer_name, event_id)
);
```

---

# Etapa 7: Saga de compra

Una compra cruza varios límites:

```text
1. Reservar entrada.
2. Crear orden.
3. Procesar pago.
4. Confirmar entrada.
5. Enviar notificación.
```

No existe una única transacción ACID que cubra todos los servicios.

Implementa una Saga.

## Opción recomendada para aprender

Empieza con orquestación desde Order Service.

```text
Order Service
    ├── solicita reserva
    ├── solicita pago
    ├── confirma inventario
    └── cancela o compensa si algo falla
```

## Casos obligatorios

Debes manejar:

* Reserva exitosa y pago rechazado.
* Pago aprobado y confirmación de inventario fallida.
* Timeout de pago con estado desconocido.
* Orden cancelada mientras llega un callback tardío.
* Evento duplicado.
* Evento fuera de orden.
* Reserva que expira durante el pago.

Aquí aprenderás una idea esencial:

> En sistemas distribuidos, “falló” y “no conozco el resultado” no significan lo mismo.

---

# Etapa 8: observabilidad real

Instrumenta desde el principio con OpenTelemetry.

Stack sugerido:

* Micrometer.
* Prometheus.
* Grafana.
* OpenTelemetry Collector.
* Tempo o Jaeger para trazas.
* Loki o Elasticsearch para logs.
* Alertmanager.

## Métricas técnicas

Registra:

```text
http_server_requests_seconds
jvm_memory_used_bytes
jvm_gc_pause_seconds
process_cpu_usage
hikaricp_connections_active
hikaricp_connections_pending
kafka_consumer_lag
redis_cache_hits
redis_cache_misses
```

## Métricas de negocio

Estas suelen ser más importantes que las técnicas:

```text
ticket_reservations_total
ticket_reservation_conflicts_total
orders_created_total
orders_confirmed_total
payments_failed_total
oversold_tickets_total
reservation_expiration_total
```

El valor de `oversold_tickets_total` siempre debería ser cero.

## Logging

Todos los logs deben incluir:

```text
traceId
spanId
orderId
eventId
ticketId
userId
service
environment
```

No incluyas tokens, tarjetas ni datos sensibles.

## Desafío

Provoca que una compra tarde cinco segundos.

Usa tracing para responder:

* ¿Dónde se consumió el tiempo?
* ¿Fue CPU, red, base de datos o espera?
* ¿Qué consulta fue lenta?
* ¿Hubo retry?
* ¿Qué servicio originó la latencia?
* ¿La operación terminó o quedó en estado ambiguo?

No aceptes “el sistema está lento” como diagnóstico.

Debes llegar a una causa verificable.

---

# Etapa 9: escalabilidad horizontal

Empaqueta los servicios en Docker y despliega en Kubernetes.

Primero puedes usar:

* Kind.
* Minikube.
* K3d.

Después puedes trasladarlo a AKS.

## Prueba inicial

Despliega una única réplica del Inventory Service y ejecuta una prueba de carga.

Después escala a cinco réplicas.

## Problema deliberado

Mantén reservas o sesiones en memoria local.

Cuando el balanceador envíe solicitudes a pods distintos, el sistema comenzará a comportarse de forma inconsistente.

Debes eliminar estado local o moverlo a almacenamiento compartido.

## HPA

Configura escalado por CPU y comprueba sus limitaciones.

Un servicio puede estar saturado por:

* Pool de conexiones.
* Latencia de base de datos.
* Kafka lag.
* Threads bloqueados.
* Red.
* Rate limits externos.

Aunque la CPU sea baja.

Después experimenta con métricas personalizadas:

* Requests por segundo.
* Longitud de cola.
* Kafka consumer lag.
* Número de conexiones pendientes.
* Latencia p95.

---

# Etapa 10: límites físicos

Esta etapa separa a un desarrollador competente de uno realmente fuerte.

## CPU

Crea una operación intencionalmente costosa:

* Generación de reportes.
* Cálculo de precios.
* Serialización de grandes respuestas.
* Compresión.
* Firma criptográfica.

Mide:

* CPU por pod.
* Tiempo de usuario y sistema.
* Thread count.
* Throughput.
* Latencia p99.

Usa Java Flight Recorder y async-profiler para encontrar hotspots.

## Memoria

Provoca:

* Caché sin límite.
* Lectura completa de archivos grandes.
* Acumulación de objetos.
* Colas internas sin capacidad.
* Retención accidental de referencias.

Analiza:

* Heap.
* Metaspace.
* Direct memory.
* GC pauses.
* Allocation rate.
* Old generation.
* Heap dump.

No reduzcas todos los problemas de memoria a “aumentar el heap”.

## Red

Simula:

* Latencia de 100, 300 y 1.000 ms.
* Pérdida de paquetes.
* Conexiones reiniciadas.
* DNS lento.
* Payloads grandes.

Herramientas:

* Toxiproxy.
* `tc`.
* Chaos Mesh.
* LitmusChaos.

## I/O

Compara:

* Procesamiento síncrono.
* Procesamiento asíncrono.
* Escritura individual.
* Escritura por lotes.
* Compresión.
* Tamaño de payload.
* Pools pequeños y grandes.

---

# Plan de “romper el sistema”

Ejecuta estas pruebas como ejercicios formales.

| Experimento                    | Fallo provocado          | Resultado que debes estudiar |
| ------------------------------ | ------------------------ | ---------------------------- |
| 1 asiento, 1.000 compradores   | Condición de carrera     | Sobreventa y contención      |
| Consulta sin índice            | Sequential scan          | CPU e I/O de PostgreSQL      |
| Clave popular expirada         | Cache stampede           | Sobrecarga repentina de BD   |
| Payment con 5 s de latencia    | Thread starvation        | Saturación en cascada        |
| Retry sin idempotencia         | Doble cobro              | Semántica de reintentos      |
| Caída después del commit       | Evento perdido           | Transactional Outbox         |
| Consumidor cae antes del ack   | Evento duplicado         | Idempotencia                 |
| Kafka detenido                 | Acumulación de outbox    | Recuperación y backpressure  |
| Redis detenido                 | Cache miss masivo        | Fallback y degradación       |
| Pod reiniciado durante compra  | Estado parcial           | Saga y compensaciones        |
| Escalado de 1 a 10 pods        | Saturación de PostgreSQL | Límite compartido            |
| Pool de 200 conexiones por pod | Colapso de BD            | Presupuesto de conexiones    |
| Carga superior al inventario   | Thundering herd          | Rate limiting y cola virtual |

---

# Pruebas de carga

Utiliza Gatling, ya que estás trabajando con Java.

Diseña al menos cuatro perfiles.

## Carga estable

```text
100 usuarios durante 20 minutos
```

Objetivo: conocer el comportamiento normal.

## Incremento gradual

```text
100 → 500 → 1.000 → 5.000 usuarios
```

Objetivo: descubrir el punto de degradación.

## Spike

```text
100 → 10.000 usuarios en 10 segundos
```

Objetivo: simular la apertura de ventas.

## Soak test

```text
500 usuarios durante 4 horas
```

Objetivo: descubrir fugas de memoria, conexiones y degradación progresiva.

Para cada prueba registra:

* Throughput.
* Error rate.
* p50.
* p90.
* p95.
* p99.
* CPU.
* Memoria.
* GC.
* Conexiones de base de datos.
* Cache hit ratio.
* Kafka lag.

El promedio no es suficiente. Un sistema puede tener un promedio de 200 ms y, al mismo tiempo, un p99 de 15 segundos.

---

# Tecnologías recomendadas

```text
Java 21
Spring Boot 3.x
Spring Security
Spring Data JPA
PostgreSQL
Redis
Kafka
Resilience4j
OpenTelemetry
Micrometer
Prometheus
Grafana
Tempo o Jaeger
Loki
Testcontainers
Gatling
Docker
Kubernetes
Helm
GitHub Actions o Azure Pipelines
```

Para este proyecto evitaría comenzar con WebFlux. Primero domina concurrencia, pools, backpressure, rendimiento y observabilidad con el modelo tradicional de Spring MVC.

Después puedes implementar una versión específica del API de catálogo o del Payment Service con WebFlux y comparar resultados. WebFlux no convierte automáticamente un sistema en rápido o escalable.

---

# Orden de implementación

## Nivel 1: fundamentos sólidos

1. Monolito modular.
2. Arquitectura hexagonal.
3. PostgreSQL.
4. Reserva concurrente.
5. Pruebas unitarias e integración.
6. Testcontainers.
7. Gatling básico.

## Nivel 2: rendimiento

1. Índices.
2. Planes de ejecución.
3. Pool de conexiones.
4. Redis.
5. Paginación eficiente.
6. Profiling de JVM.
7. Pruebas de carga avanzadas.

## Nivel 3: sistemas distribuidos

1. Extraer Inventory Service.
2. Extraer Payment Service.
3. Kafka.
4. Outbox.
5. Consumidores idempotentes.
6. Saga.
7. Consistencia eventual.

## Nivel 4: producción

1. OpenTelemetry.
2. Prometheus y Grafana.
3. Circuit Breaker.
4. Bulkhead.
5. Rate limiting.
6. Kubernetes.
7. HPA.
8. Chaos testing.
9. SLO y alertas.

---

# Definition of Done de nivel profesional

No consideres terminada una funcionalidad solo porque responde `200 OK`.

Cada capacidad importante debe incluir:

* Regla de negocio probada.
* Prueba unitaria.
* Prueba de integración.
* Prueba concurrente cuando corresponda.
* Métricas.
* Logs estructurados.
* Trazabilidad.
* Timeout.
* Estrategia de retry.
* Idempotencia cuando aplique.
* Manejo de fallos.
* Prueba de carga.
* Documentación de decisiones.

También debes mantener Architecture Decision Records:

```text
ADR-001: Uso de bloqueo optimista para reservas
ADR-002: Redis no es fuente de verdad del inventario
ADR-003: Uso de Transactional Outbox
ADR-004: Saga orquestada desde Order Service
ADR-005: Presupuesto máximo de conexiones por servicio
```

---

# Resultado esperado

Al terminar, no solo tendrás un portafolio con microservicios. Serás capaz de explicar con evidencia:

* Por qué una reserva es segura bajo concurrencia.
* Qué ocurre cuando un servicio se cae a mitad de una compra.
* Cómo evitar dobles pagos.
* Cómo detectar una consulta lenta.
* Cómo distinguir saturación de CPU, memoria, red o I/O.
* Cuándo utilizar caché y cuándo no.
* Por qué escalar pods puede empeorar la base de datos.
* Cómo medir la capacidad máxima del sistema.
* Cómo diseñar degradación controlada.
* Cómo investigar una operación mediante una traza distribuida.
* Qué garantías reales ofrece Kafka y cuáles debes implementar tú.

Ese conocimiento es el que te acerca a un nivel senior fuerte y, posteriormente, a un rol de arquitectura: no memorizar patrones, sino entender qué problema resuelven, qué costo introducen y cómo demostrar que funcionan.
