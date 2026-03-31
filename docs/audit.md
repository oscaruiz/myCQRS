# Auditoría Completa de myCQRS

**Fecha:** 2026-03-17
**Branch:** hardening/core-stability-phase1

---

## PARTE 1: Hallazgos de la Auditoría

### 1.1 Implementación CQRS

**Lo que está bien:**
- Separación clara de Command/Query/Event con buses dedicados
- Interceptor chain en el CommandBus (patrón middleware) — bien pensado
- Auto-registro de handlers via `BeanPostProcessor` — elegante y evita boilerplate
- Las meta-anotaciones (`@CommandHandlerComponent`, etc.) son un buen toque de DX

**Problemas concretos:**

| # | Hallazgo | Archivo | Línea | Severidad |
|---|----------|---------|-------|-----------|
| 1 | **`System.out.print` en el EventBus** — logging de debug con `System.out` en código de producción del framework | `core/.../SimpleEventBus.java` | 20, 33 | ALTA |
| 2 | **`System.out.printf` en EventHandler** — mismo problema | `demo/.../BookCreatedEventHandler.java` | 13 | ALTA |
| 3 | **EventBus no tiene `setEventBus()` usado en ningún sitio** — `SimpleCommandBus.setEventBus()` existe pero nunca se llama ni se usa. Campo `eventBus` es dead code | `core/.../SimpleCommandBus.java` | 21-25 | MEDIA |
| 4 | **QueryBus usa `HashMap` no thread-safe** mientras CommandBus y EventBus usan `ConcurrentHashMap` — inconsistencia peligrosa | `core/.../SimpleQueryBus.java` | ~5 | MEDIA |
| 5 | **El invoker del interceptor ignora el `cmd` parameter** — `cmd -> handler.handle(command)` siempre usa el `command` original del closure, no el `cmd` que recibe. Si un interceptor modifica el command, el handler no ve el cambio | `core/.../SimpleCommandBus.java` | 39 | MEDIA |
| 6 | **`GetBookByTitleQueryHandler` es un stub con TODO** — devuelve string hardcodeado | `demo/.../GetBookByTitleQueryHandler.java` | 11 | ALTA |
| 7 | **`QueryTestController` está en test pero no es un test** — es un `@RestController` perdido en `src/test` sin paquete | `demo/src/test/.../QueryTestController.java` | - | MEDIA |
| 8 | **`FindBookByTitleQueryHandlerTest` sin paquete** — clase de test en el root de test sin package declaration | `demo/src/test/.../FindBookByTitleQueryHandlerTest.java` | - | BAJA |

### 1.2 Adherencia a DDD

**Lo que está bien:**
- `BookAggregate` centraliza la lógica de negocio (create, update, delete)
- Factory method `create()` + `rehydrate()` — buen patrón para separar creación de reconstrucción
- Domain events se generan DENTRO del agregado — correcto
- `pullDomainEvents()` — patrón clásico de "collect & flush" bien implementado
- El agregado protege invariantes: no puedes deletear sin ID, no puedes updatear un libro borrado

**Problemas concretos:**

| # | Hallazgo | Archivo | Línea | Severidad |
|---|----------|---------|-------|-----------|
| 9 | **Los eventos de dominio NO tienen timestamp** — un evento sin timestamp es incompleto para cualquier sistema real | `demo/.../BookCreatedEvent.java` | - | ALTA |
| 10 | **Los eventos de dominio NO tienen eventId** — sin identificador único, no hay idempotencia posible | `demo/.../BookCreatedEvent.java` | - | ALTA |
| 11 | **`aggregateId` es `String` en eventos pero `Long` en el agregado** — conversión con `String.valueOf(id)` dispersa por el código. Inconsistencia de tipos | `BookAggregate.java` / eventos | 67, 79 | MEDIA |
| 12 | **`Book.java` (dominio) es un DTO glorificado** — está en `domain/model` pero solo tiene title+author, sin comportamiento. ¿Es un Value Object? ¿Un Read Model? Su rol es ambiguo | `demo/.../Book.java` | - | MEDIA |
| 13 | **El agregado no valida sus invariantes en construcción** — el constructor público acepta cualquier valor incluyendo nulls y strings vacíos. Solo `updateIfPresent` valida blanks | `BookAggregate.java` | 19-24 | MEDIA |
| 14 | **`backfillAggregateIdInDomainEvents` es un hack** — el agregado no debería necesitar reasignar IDs en eventos ya creados. Esto existe porque el ID se genera en la capa de persistencia (auto-increment) | `BookAggregate.java` | 92-100 | MEDIA |
| 15 | **No hay interfaz base para agregados** — si el core framework es genérico, debería ofrecer una clase `AggregateRoot` con `recordEvent`/`pullDomainEvents` | `core/domain/` | - | MEDIA |

### 1.3 Arquitectura Hexagonal

**Lo que está bien:**
- `BookRepository` (puerto) en dominio, `JpaBookRepository` (adaptador) en infraestructura — correcto
- Los command handlers dependen del puerto, no de la implementación JPA
- La separación física en paquetes `domain/`, `application/`, `infrastructure/` existe

**Fugas y problemas:**

| # | Hallazgo | Archivo | Línea | Severidad |
|---|----------|---------|-------|-----------|
| 16 | **`BookReadRepository` (in-memory HashMap) está en infraestructura pero se usa como si fuera un puerto** — no hay interfaz. `FindBookByTitleQueryHandler` depende directamente de una clase concreta de infraestructura | `demo/.../BookReadRepository.java` + `FindBookByTitleQueryHandler.java` | - | ALTA |
| 17 | **`BookCreatedEventProjection` está en `application/event` pero escribe en `BookReadRepository`** (infraestructura) — la projection debería estar en infraestructura, no en application | `demo/.../BookCreatedEventProjection.java` | - | MEDIA |
| 18 | **El controlador recibe `CreateBookCommand` directamente como `@RequestBody`** — el Command (objeto de aplicación) actúa como DTO de la API. Esto acopla la forma del JSON HTTP a la estructura del command | `BookController.java` | 26 | MEDIA |
| 19 | **No hay capa de error handling HTTP** — sin `@ControllerAdvice`, las excepciones del dominio (`IllegalStateException`) se propagan como 500 al cliente | Ausente | - | ALTA |
| 20 | **Dependencias del parent POM incluyen spring-data-mongodb y spring-web** — estas deberían estar solo en el módulo demo, no en el parent. El core no necesita Mongo ni Web | `pom.xml` (root) | - | MEDIA |

### 1.4 Calidad del Código

| # | Hallazgo | Archivo | Severidad |
|---|----------|---------|-----------|
| 21 | **Credenciales hardcodeadas en config** — `password: password` para PostgreSQL, sin profile de test separado | `application.yml` | ALTA |
| 22 | **No hay profiles de Spring** — un solo `application.yml` para todo. Sin `application-test.yml`, `application-dev.yml` | - | MEDIA |
| 23 | **Emojis en logs del BeanPostProcessor** — `"✅ Registered handler..."`, `"🔍 Post-procesando..."`. Mezcla español/inglés | `CommandHandlerBeanPostProcessor.java`, `EventHandlerBeanPostProcessor.java` | BAJA |
| 24 | **`BookController.createBook` retorna `void`** — debería retornar el ID creado o al menos un 201 Created con Location header | `BookController.java:26` | MEDIA |
| 25 | **GET `/books/{title}` busca por título** — semánticamente, un path variable así sugiere búsqueda por ID. Buscar por título debería ser query param: `GET /books?title=xxx` | `BookController.java:43` | MEDIA |
| 26 | **No hay `@Transactional` en ningún command handler** — si la persistencia falla después de un paso intermedio, no hay rollback | Command handlers | ALTA |

### 1.5 Testing

**Lo que hay:**
- 4 test unitarios del agregado (buenos, cubren invariantes clave)
- 2 test de integración con H2 (command flow, smoke test)
- 1 test unitario del query handler

**Lo que falta es MUCHO más grave que lo que hay:**

| # | Hallazgo | Severidad |
|---|----------|-----------|
| 27 | **CERO tests para el módulo core** — el framework CQRS (SimpleCommandBus, SimpleEventBus, SimpleQueryBus, interceptors, BeanPostProcessors) no tiene ni un test | CRÍTICA |
| 28 | **No hay tests del controller** — ni MockMvc ni WebTestClient | ALTA |
| 29 | **No hay tests de las proyecciones Mongo** — las projections son lógica crítica sin cobertura | ALTA |
| 30 | **No hay tests de error paths** — ¿qué pasa cuando el command bus recibe un command sin handler? ¿Cuando la validación falla? | ALTA |
| 31 | **`FindBookByTitleQueryHandlerTest` no tiene package** — puede causar problemas de classpath | MEDIA |
| 32 | **Los tests de integración no verifican eventos** — verifican que el aggregate se guardó pero no que las projections se ejecutaron | MEDIA |

### 1.6 Deuda Técnica Concreta

| Tipo | Descripción | Ubicación |
|------|-------------|-----------|
| TODO | `GetBookByTitleQueryHandler` — handler stub | `GetBookByTitleQueryHandler.java:11` |
| Dead code | `SimpleCommandBus.eventBus` field — nunca usado | `SimpleCommandBus.java:21` |
| Dead code | `QueryTestController` — controller huérfano en tests | `src/test/.../QueryTestController.java` |
| Redundancia | `BookCreatedEventHandler` solo hace `System.out.printf` — no aporta nada real | `BookCreatedEventHandler.java` |
| Inconsistencia | Dos read repositories: `BookReadRepository` (HashMap) y `BookMongoRepository` (Mongo) — ambos activos, sin claridad sobre cuál es el "real" | `infrastructure/repository/` vs `infrastructure/mongo/` |
| Missing | No hay `DeleteBookProjection` ni `UpdateBookReadModelProjection` para el read model in-memory — solo existe `BookCreatedEventProjection` | - |

---

## PARTE 2: Roadmap de Mejoras

### Nivel 1 — Fundamentos (hacer primero)

#### T1.1: Eliminar System.out y usar SLF4J
- **Qué:** Reemplazar todos los `System.out.print/printf` por `Logger` de SLF4J
- **Por qué:** Es la primera red flag que un entrevistador ve. Grita "proyecto de student"
- **Concepto:** Logging framework vs stdout, niveles de log (DEBUG, INFO, WARN, ERROR)
- **Pistas:** `LoggerFactory.getLogger(getClass())`. Los mensajes del EventBus son DEBUG. Los de registro de handlers son INFO
- **Done:** Cero `System.out` en todo el proyecto. `grep -r "System.out" src/` devuelve vacío
- **Dificultad:** Baja

#### T1.2: Resolver el TODO del GetBookByTitleQueryHandler (o eliminarlo)
- **Qué:** Implementar el handler para que lea del read model real, o eliminarlo si es redundante con `FindBookByTitleQuery`
- **Por qué:** Un TODO en producción en un proyecto portfolio es peor que no tener el archivo
- **Concepto:** Decidir si necesitas dos queries para lo mismo. Si no, borra. Si sí, diferéncialos (uno retorna `Book`, otro retorna `String` summary)
- **Pistas:** Si lo mantienes, inyecta `BookMongoRepository` o `BookReadRepository` y haz una consulta real
- **Done:** No queda ningún TODO en el código. Cada query tiene un propósito claro
- **Dificultad:** Baja

#### T1.3: Limpiar dead code y archivos huérfanos
- **Qué:** Eliminar `SimpleCommandBus.eventBus` (dead code), `QueryTestController` (huérfano), `BookCreatedEventHandler` (solo hace println)
- **Por qué:** Dead code genera preguntas incómodas en code review: "¿para qué es esto?"
- **Concepto:** YAGNI, código limpio, código que no existe no tiene bugs
- **Pistas:** Busca todos los usos antes de borrar para confirmar que son dead code
- **Done:** Cero clases sin uso real. Cero campos sin referencias
- **Dificultad:** Baja

#### T1.4: Agregar @ControllerAdvice para error handling HTTP
- **Qué:** Crear un `GlobalExceptionHandler` que mapee excepciones de dominio a respuestas HTTP apropiadas
- **Por qué:** Sin esto, cualquier error del dominio devuelve un 500 con stack trace. Un entrevistador probará tu API y verá eso
- **Concepto:** Exception translation entre capas, HTTP status codes semánticos, problem details (RFC 7807)
- **Pistas:** `IllegalStateException` -> 409 Conflict, `NoSuchElementException` -> 404, `IllegalArgumentException` (validación) -> 400. Retorna un body JSON estructurado, no solo el status code
- **Done:** Prueba manual: `DELETE /books/999` retorna `404` con JSON body legible, no un 500 con stack trace
- **Dificultad:** Baja

#### T1.5: Tests unitarios para el módulo core
- **Qué:** Crear tests para `SimpleCommandBus`, `SimpleEventBus`, `SimpleQueryBus`, `ValidationCommandInterceptor`
- **Por qué:** EL FRAMEWORK ES TU PRODUCTO. Si el core no tiene tests, nada tiene credibilidad. Es la carencia más grave del repo
- **Concepto:** Testing de infraestructura, test doubles, verificación de contratos
- **Pistas:** Para el CommandBus: test handler registration, test send, test duplicate registration throws, test send without handler throws, test interceptor chain order. Para EventBus: test multiple handlers per event, test no handlers doesn't throw. Para ValidationCommandInterceptor: crea un command con `@NotBlank` y verifica que falla
- **Done:** Cobertura >90% de las clases del core. Cada comportamiento público tiene un test que lo documenta
- **Dificultad:** Media

#### T1.6: Consistencia de tipos en aggregateId
- **Qué:** Decidir: ¿`Long` o `String` para el aggregateId? Y usarlo consistentemente en agregado Y eventos
- **Por qué:** La inconsistencia `Long` en el agregado vs `String` en eventos con `String.valueOf()` disperso es un smell que genera preguntas
- **Concepto:** Value Objects para identidades, consistencia de modelo
- **Pistas:** Considera crear un `BookId` value object. O al menos unifica a un solo tipo. Si usas UUID en el futuro (recomendado), String es mejor. Si te quedas con auto-increment, Long es más natural — pero entonces los eventos también deberían usar Long
- **Done:** Un solo tipo para IDs en todo el flujo. Cero llamadas a `String.valueOf(id)` para conversión de IDs
- **Dificultad:** Media

#### T1.7: Agregar @Transactional a los command handlers
- **Qué:** Anotar los command handlers con `@Transactional` (o mejor, crear un interceptor transaccional en el bus)
- **Por qué:** Sin transacciones, un fallo parcial deja el sistema en estado inconsistente. Cualquier entrevistador que mire los handlers lo notará
- **Concepto:** Boundaries transaccionales, Unit of Work, dónde poner @Transactional en arquitectura hexagonal
- **Pistas:** Hay dos opciones: (a) `@Transactional` directamente en cada handler, o (b) un `TransactionalCommandInterceptor` en el bus que wrappee toda la ejecución del handler en una transacción. La opción (b) es más elegante y más CQRS-like. Piensa en qué pasa si el save funciona pero la publicación de eventos falla
- **Done:** Cada operación de escritura es atómica. Un test que fuerza un fallo después del save verifica rollback
- **Dificultad:** Media

#### T1.8: Fix del BookController — REST API correcta
- **Qué:** (1) `POST /books` retorna 201 con Location header y el ID. (2) `GET /books/{title}` cambia a `GET /books?title=xxx` o a `GET /books/{id}`. (3) Separar el Command del DTO del request
- **Por qué:** REST API design es algo que un entrevistador evalúa en 10 segundos mirando los endpoints
- **Concepto:** RESTful design, separación de DTO de transporte vs objetos de aplicación, HTTP semantics
- **Pistas:** Crea `CreateBookRequest` (ya existe) -> mapea a `CreateBookCommand` en el controller. `commandBus.send()` debería retornar el ID del recurso creado. Usa `ResponseEntity.created(URI)` con el Location header
- **Done:** `curl POST /books` retorna 201 con `Location: /books/1`. GET busca por ID. Query param para buscar por título
- **Dificultad:** Media

---

### Nivel 2 — Semi-profesional (el objetivo)

#### T2.1: Timestamps y eventId en los eventos de dominio
- **Qué:** Agregar `eventId` (UUID) y `occurredAt` (Instant) a todos los eventos. Considera una clase base `DomainEvent` en el core
- **Por qué:** Todo event-driven system real necesita estos campos. Sin ellos, no puedes ordenar, deduplicar, ni auditar
- **Concepto:** Event metadata, event envelope pattern, inmutabilidad de eventos
- **Pistas:** Crea `DomainEvent` abstracto en core que extienda `Event` y tenga `eventId`, `occurredAt`, `aggregateId`. Los eventos concretos lo extienden. Genera el UUID en el constructor
- **Done:** Todos los eventos tienen ID único y timestamp. Los audit logs en Mongo registran estos campos
- **Dificultad:** Media

#### T2.2: AggregateRoot base class en el core
- **Qué:** Extraer `recordEvent()` y `pullDomainEvents()` a una clase `AggregateRoot<ID>` en el módulo core
- **Por qué:** Es el patrón estándar de DDD. Demuestra que entiendes que el framework debe ser genérico
- **Concepto:** Aggregate Root pattern, genéricos en Java, framework design vs application code
- **Pistas:** `abstract class AggregateRoot<ID>` con los métodos de eventos. `BookAggregate extends AggregateRoot<Long>`. Elimina el hack de backfill si decides generar IDs en el dominio
- **Done:** `BookAggregate` extiende `AggregateRoot`. El método `recordEvent` no se duplica entre agregados
- **Dificultad:** Media

#### T2.3: Generación de IDs en el dominio (UUID)
- **Qué:** Reemplazar auto-increment de JPA por UUIDs generados en el factory method del agregado
- **Por qué:** Elimina el hack de `backfillAggregateIdInDomainEvents` y el `assignId`. El dominio no depende de la persistencia para saber su identidad
- **Concepto:** Domain-driven identity, UUID vs auto-increment, independencia del dominio respecto a infraestructura
- **Pistas:** En `BookAggregate.create()`, genera un UUID. El evento ya tiene su aggregateId desde el momento cero. La entidad JPA usa ese UUID como PK (no auto-generated). Cuidado con la estrategia de UUID en PostgreSQL
- **Done:** `assignId()` y `backfillAggregateIdInDomainEvents()` eliminados. Los eventos siempre tienen aggregateId desde su creación
- **Dificultad:** Media

#### T2.4: Completar las projections del read model
- **Qué:** Agregar projections para `BookUpdatedEvent` y `BookDeletedEvent` tanto en el read repository in-memory como en Mongo. Decidir: ¿mantener ambos read models o solo Mongo?
- **Por qué:** Un read model que solo se crea pero nunca se actualiza ni borra es una demo incompleta
- **Concepto:** Eventual consistency, projection rebuilding, read model lifecycle
- **Pistas:** Simplifica: elimina el `BookReadRepository` in-memory y usa solo Mongo como read store. O viceversa. Tener dos sin motivo claro confunde. Un projection por evento, una colección de read model actualizada
- **Done:** CRUD completo en el read model. Un test que crea, actualiza y borra un libro verifica que el read model refleja todos los cambios
- **Dificultad:** Media

#### T2.5: Tests de integración del controller con MockMvc
- **Qué:** Tests que ejerciten el API REST end-to-end: happy paths, validaciones, error responses
- **Por qué:** Son los tests que más impresionan en un portfolio porque demuestran que la API funciona de verdad
- **Concepto:** MockMvc, @WebMvcTest vs @SpringBootTest, testing de APIs REST, test slicing
- **Pistas:** Usa `@SpringBootTest` + `MockMvc`. Testea: POST con body válido -> 201, POST con body inválido -> 400, GET de libro inexistente -> 404, DELETE + GET -> 404
- **Done:** Cada endpoint tiene al menos un happy path y un error path testeado
- **Dificultad:** Media

#### T2.6: Spring Profiles y configuración apropiada
- **Qué:** Crear `application-dev.yml`, `application-test.yml`. Quitar credenciales del yml base. Usar H2 para tests, PostgreSQL para dev
- **Por qué:** Demuestra madurez en configuración de aplicaciones Spring. Las credenciales hardcodeadas son inaceptables
- **Concepto:** Spring profiles, externalized configuration, 12-factor app
- **Pistas:** `application.yml` tiene defaults seguros. `application-dev.yml` override con PostgreSQL local. Los tests usan `@ActiveProfiles("test")` con H2. Las credenciales reales van en env vars: `${DB_PASSWORD:defaultForDev}`
- **Done:** `application.yml` no contiene credenciales reales. Tests corren sin PostgreSQL. `mvn test` pasa en CI sin Docker
- **Dificultad:** Baja

#### T2.7: CI/CD con GitHub Actions
- **Qué:** Crear `.github/workflows/ci.yml` que ejecute `mvn verify` en cada push y PR
- **Por qué:** Un proyecto portfolio sin CI es como un coche sin ruedas. Es lo mínimo esperado
- **Concepto:** CI/CD, GitHub Actions, build automation, quality gates
- **Pistas:** Job simple: checkout, setup-java 21, cache maven, `mvn verify`. Después puedes agregar: checkstyle, coverage report con JaCoCo, badge en README
- **Done:** Badge verde en el README. Cada PR tiene un check que debe pasar
- **Dificultad:** Baja

#### T2.8: Dockerfile multi-stage
- **Qué:** Crear un Dockerfile para la demo app con build multi-stage (Maven build + JRE runtime)
- **Por qué:** Demuestra que puedes contenerizar tu aplicación. Es esperado en cualquier posición moderna
- **Concepto:** Docker multi-stage builds, JRE vs JDK, image optimization
- **Pistas:** Stage 1: `maven:3.9-eclipse-temurin-21` para build. Stage 2: `eclipse-temurin:21-jre-alpine` para runtime. Copia solo el JAR final
- **Done:** `docker build -t mycqrs . && docker run mycqrs` arranca la app. La imagen pesa <200MB
- **Dificultad:** Baja

#### T2.9: Validación robusta en el agregado
- **Qué:** El constructor y factory method del agregado deben validar invariantes (title not blank, author not blank, etc.)
- **Por qué:** Un agregado que acepta estado inválido viola el principio más básico de DDD: "un agregado siempre está en estado válido"
- **Concepto:** Guard clauses, invariant protection, fail-fast principle
- **Pistas:** En `create()`: si title o author son blank, lanza `IllegalArgumentException` con mensaje descriptivo. El constructor privado o package-private. Solo `create()` y `rehydrate()` como entry points
- **Done:** `BookAggregate.create(null, "author")` lanza excepción. Test unitario que lo verifica
- **Dificultad:** Baja

#### T2.10: Limpiar dependencias del parent POM
- **Qué:** Mover spring-data-mongodb, spring-web, spring-data-jpa del parent al módulo demo donde realmente se usan
- **Por qué:** El core no debería heredar dependencias de infraestructura
- **Concepto:** Dependency management, module boundaries, minimal dependencies
- **Pistas:** El parent POM usa `<dependencyManagement>` para versiones. Cada módulo declara solo lo que necesita
- **Done:** `mvn dependency:tree` del core no muestra MongoDB, Web, ni JPA
- **Dificultad:** Baja

---

### Nivel 3 — Avanzado (stretch goals)

#### T3.1: Publicación de eventos asíncrona
- **Qué:** Cambiar la publicación de eventos de síncrona a asíncrona con garantía de entrega
- **Por qué:** En producción, los eventos síncronos bloquean el command handler. La asincronía es el paso natural
- **Concepto:** Async event processing, thread pools, error handling en contextos asíncronos
- **Pistas:** Dos opciones: (a) `@Async` de Spring con un executor dedicado, o (b) implementar un `AsyncEventBus` que use `CompletableFuture`. Piensa: ¿qué pasa si un handler async falla? ¿Quién se entera?
- **Done:** Los event handlers ejecutan en un thread separado del command handler. Un test verifica que el command retorna antes de que el handler termine
- **Dificultad:** Media

#### T3.2: Outbox Pattern
- **Qué:** En lugar de publicar eventos directamente, guardarlos en una tabla "outbox" en la misma transacción que el agregado, y un poller los publica después
- **Por qué:** Resuelve el problema de atomicidad: "qué pasa si el save funciona pero el publish falla". Es el patrón gold-standard para sistemas event-driven
- **Concepto:** Transactional outbox, at-least-once delivery, polling publisher, CDC
- **Pistas:** Tabla `outbox_events(id, aggregate_id, event_type, payload, published, created_at)`. El command handler guarda el evento en outbox dentro de la misma transacción. Un `@Scheduled` job poll la tabla y publica los no-publicados. Marca como publicados después
- **Done:** Los eventos se persisten atómicamente con el agregado. Un test que mata el proceso entre save y publish verifica que el evento no se pierde
- **Dificultad:** Alta

#### T3.3: Event Store (Event Sourcing lite)
- **Qué:** Implementar un event store donde los eventos son la fuente de verdad y el estado se reconstruye reproduciendo eventos
- **Por qué:** Es el siguiente nivel de CQRS. Demuestra entendimiento profundo del patrón completo
- **Concepto:** Event sourcing, event replay, snapshot pattern, temporal queries
- **Pistas:** `EventStore` interface con `save(aggregateId, events, expectedVersion)` y `load(aggregateId)`. El agregado tiene `apply(Event)` que muta el estado. `rehydrate` reproduce todos los eventos. La tabla JPA es el event store, no la entidad Book
- **Done:** Puedes reconstruir el estado de cualquier BookAggregate reproduciendo sus eventos. `load()` no lee de una tabla "books" sino del event store
- **Dificultad:** Alta

#### T3.4: Read/Write Store separados reales
- **Qué:** El write store (PostgreSQL/Event Store) y el read store (MongoDB) están genuinamente separados, con proyecciones que mantienen el read model eventualmente consistente
- **Por qué:** Es la promesa completa de CQRS: optimizar lectura y escritura independientemente
- **Concepto:** Eventual consistency, projection rebuilding, read model optimization
- **Pistas:** Las queries NUNCA tocan PostgreSQL. Solo leen de Mongo. Los commands NUNCA leen de Mongo. Solo escriben en PostgreSQL. Las projections son el puente
- **Done:** Un test que verifica que el QueryHandler solo usa MongoRepository. Un test que verifica que el CommandHandler solo usa JpaRepository. Separación verificable
- **Dificultad:** Media

#### T3.5: Idempotencia en command handlers
- **Qué:** Implementar deduplicación de commands para garantizar que procesar el mismo command dos veces no cause efectos duplicados
- **Por qué:** En sistemas distribuidos, los mensajes pueden llegar duplicados. La idempotencia es esencial
- **Concepto:** Idempotency keys, deduplication store, at-least-once processing
- **Pistas:** Cada command tiene un `commandId` (UUID generado por el caller). Un interceptor verifica si ese ID ya fue procesado (tabla `processed_commands`). Si sí, retorna el resultado cached. Si no, ejecuta y guarda
- **Done:** Enviar el mismo CreateBookCommand dos veces con el mismo commandId solo crea un libro
- **Dificultad:** Alta

#### T3.6: Sagas / Process Managers
- **Qué:** Implementar una saga que coordine un flujo multi-step (ej: "cuando se crea un libro, notificar al autor y reservar ISBN")
- **Por qué:** Demuestra que entiendes coordinación de procesos distribuidos
- **Concepto:** Saga pattern, compensating transactions, process manager, choreography vs orchestration
- **Pistas:** Crea un segundo agregado (ej: `NotificationAggregate`). Una saga escucha `BookCreatedEvent`, envía un command para crear la notificación. Si falla, envía un command compensatorio
- **Done:** Un flujo multi-aggregate coordinado por una saga, con tests que verifican tanto el happy path como la compensación
- **Dificultad:** Alta

---

## PARTE 3: Interview Readiness

### 3.1 Preguntas que te van a hacer

**Sobre CQRS:**
1. "¿Por qué CQRS y no un CRUD simple?" — Debes poder articular: separación de modelos de lectura/escritura, escalabilidad independiente, optimización de queries sin afectar el modelo de dominio
2. "¿Cuáles son los trade-offs de CQRS?" — Complejidad adicional, eventual consistency, duplicación de datos. Sé honesto sobre cuándo NO usarlo
3. "¿Por qué tus eventos son síncronos?" — Ten una respuesta: "es la primera iteración, el roadmap incluye async + outbox para garantía de entrega"
4. "¿Qué pasa si el publish de un evento falla después del save?" — Hoy no tienes respuesta. Después de T2.7/T3.2 la tendrás

**Sobre DDD:**
5. "¿Por qué usas un agregado en lugar de una entidad JPA directamente?" — Encapsulación de reglas de negocio, generación de eventos dentro del dominio, independencia de la persistencia
6. "¿Cómo manejas la identidad del agregado?" — Hoy es un punto débil (backfill hack). Después de T2.3, tu respuesta es sólida
7. "¿Qué invariantes protege tu agregado?" — Puedes listar: no deletear sin ID, no deletear dos veces, no actualizar borrado, no emitir update sin ID

**Sobre arquitectura:**
8. "¿Cómo sé que tu hexagonal architecture realmente funciona?" — "El core module no tiene dependencia en Spring Data, JPA, ni Mongo. Puedo reemplazar la persistencia sin tocar el dominio"
9. "¿Por qué un módulo separado para el core?" — Reutilización, testing independiente, forzar la separación de dependencias a nivel de build

**Sobre el código:**
10. "¿Cómo funciona tu auto-registro de handlers?" — Explica el BeanPostProcessor: Spring lo llama para cada bean, inspecciona genéricos, extrae tipos, registra en el bus
11. "¿Por qué interceptors en vez de AOP?" — Explícito vs mágico. Los interceptors son testables independientemente y visibles en el flujo

### 3.2 Decisiones que debes saber defender

| Decisión | Defensa | Debilidad actual |
|----------|---------|------------------|
| Bus propio vs Spring ApplicationEventPublisher | Control total, entiendes cada línea, no dependes de magia de Spring | Pero re-inventas la rueda. Ten claro que es pedagógico |
| BeanPostProcessor para auto-registro | Elimina boilerplate de registro manual, convention over configuration | Más difícil de debuggear que registro explícito |
| Interceptor pattern en CommandBus | Extensible, composable, testable. Validación es solo el primer caso de uso | Solo tienes un interceptor. Agrega logging o transaccional para que se vea el valor |
| Módulo core separado | El framework es reusable e independiente del dominio Book | No tiene tests propios, lo que debilita este argumento |
| MongoDB como read store | Demuestra CQRS real con stores separados | Las projections están incompletas (solo create) |

### 3.3 Red Flags para un entrevistador

Ordenadas de más a menos grave:

1. **`System.out` en producción** — Señal inequívoca de código no profesional. Fix inmediato
2. **Cero tests en el módulo core** — "Construiste un framework pero no lo testeaste"
3. **TODO en código** — `GetBookByTitleQueryHandler` con un TODO visible es peor que no existir
4. **Sin error handling HTTP** — Una API que devuelve 500 con stack traces no está lista para demo
5. **Credenciales en application.yml** — `password: password` hardcodeado. Menor en un proyecto personal, pero un entrevistador consciente de seguridad lo nota
6. **Sin CI/CD** — En 2026, un repo sin pipeline es inusual para alguien que quiere demostrar profesionalismo
7. **Projections incompletas** — El read model solo se crea, nunca se actualiza ni borra. La historia CQRS queda a medias
8. **Sin @Transactional** — Cualquiera que lea los handlers y conozca Spring lo notará

### 3.4 Plan de acción recomendado

**Semana 1:** T1.1 + T1.2 + T1.3 + T1.4 (limpieza básica, 1-2 días)
**Semana 2:** T1.5 + T1.7 + T1.8 (tests core + transacciones + REST fix, 3-4 días)
**Semana 3:** T2.1 + T2.2 + T2.3 + T2.9 (DDD sólido: base class, UUIDs, validación)
**Semana 4:** T2.4 + T2.5 + T2.6 (projections completas, tests controller, profiles)
**Semana 5:** T2.7 + T2.8 + T2.10 (CI/CD, Docker, dependencias)
**Después:** Nivel 3 según lo que quieras demostrar en la entrevista

---

## Veredicto General

La base arquitectónica es sólida — la separación Command/Query/Event, el agregado con domain events, y los buses con auto-registro están bien pensados. El problema es que está al 60% de completitud: hay stubs, dead code, logging con println, tests insuficientes, y features a medias (projections). Un entrevistador vería potencial pero también descuido. El Nivel 1 es urgente y alcanzable en una semana.
