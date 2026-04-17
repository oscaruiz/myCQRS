# PRD — Optimistic Locking (@Version) en BookAggregate

**Rama:** `feat/optimistic-locking`

**Objetivo:** Que requests concurrentes al mismo Book fallen con 409 Conflict
en vez de sobreescribir silenciosamente (lost update).

**Contexto:** Hoy, si dos requests concurrentes hacen `PATCH /books/{id}` al
mismo libro, ambas cargan el mismo estado del agregado, ambas mutan, y la
segunda en hacer `save()` sobreescribe silenciosamente el cambio de la primera.
No hay ningún mecanismo que lo detecte. `AGENTS.md` lo declara como requisito
pendiente. README lo lista en "Future improvements".

---

## Decisiones de diseño

- `@Version` SOLO en `BookEntity` (infraestructura), NO en `BookAggregate` (dominio). YAGNI.
  El versionado optimista es un mecanismo de persistencia, no lógica de negocio. Si mañana
  necesitas que el agregado conozca su versión (event sourcing, versión en eventos), el campo
  sube al dominio. No antes.
- `BookAggregate.rehydrate()` NO recibe version. `toAggregate()` en `JpaBookRepository` ignora
  el campo. Deliberado y defendible.
- No ETag en respuestas HTTP (candidato para Day 16+ con OpenAPI si quieres conditional requests).
- No retry automático en el cliente.
- No version en el dominio.
- Optimistic sobre pessimistic: no queremos locks de base de datos bloqueando threads en requests HTTP.

---

## Flujo de la excepción

Cuando JPA detecta conflicto de versión:

```
Hibernate lanza jakarta.persistence.OptimisticLockException
  → Spring lo envuelve en ObjectOptimisticLockingFailureException
    → burbujea desde JpaBookRepository.save()
      → handler
        → TransactionalCommandInterceptor (hace rollback)
          → SimpleCommandBus.send()
            → controller
              → GlobalExceptionHandler → HTTP 409
```

---

## Tareas

### - [x] 1. Persistencia: añadir @Version a BookEntity + migración Flyway + schema H2

- Añadir campo `private Long version` con `@Version` a `BookEntity`.
- Crear migración `V3__add_book_version.sql`:
  ```sql
  ALTER TABLE book_entity ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  ```
- Actualizar `src/demo/src/test/resources/schema.sql` (H2) con el mismo campo
  en la tabla `book_entity`. (Este archivo desaparece en Day 11 con Testcontainers
  Postgres, pero hoy existe y debe ser consistente.)
- Verificar que `mvn test` sigue verde (no rompe tests existentes).
- NO tocar `BookAggregate`, ni `rehydrate()`, ni el mapeo `toAggregate()`.

### - [x] 2. Exception handler: GlobalExceptionHandler → 409 Conflict

- Añadir handler para `ObjectOptimisticLockingFailureException` en
  `GlobalExceptionHandler` que devuelva HTTP 409 con body:
  ```java
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiError> handleOptimisticLock(
          ObjectOptimisticLockingFailureException ex) {
      ApiError error = generateApiError(HttpStatus.CONFLICT,
          "The resource was modified by another request. Please retry.");
      return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }
  ```
- Import: `org.springframework.orm.ObjectOptimisticLockingFailureException`.
- Verificar que compila y tests existentes siguen verdes.

### - [x] 3. Test MockMvc: verificar HTTP 409

- Test en `BookControllerTest` (o clase nueva) que simula que
  `commandBus.send()` lanza `ObjectOptimisticLockingFailureException` y
  verifica que el controller devuelve 409 con body `ApiError` correcto.
- Usar `@WebMvcTest` / MockMvc existente con `@MockBean CommandBus`.
- Verificar con `mvn test`.

### - [x] 4. Test integración: detección de conflicto secuencial

- Cargar el mismo `BookAggregate` dos veces (simulando dos requests que
  leyeron el mismo estado), mutar ambos, guardar el primero (éxito), guardar
  el segundo (debe lanzar `ObjectOptimisticLockingFailureException`).
- NO usar threads — es secuencial: la segunda instancia tiene `version=0`
  cuando la DB ya tiene `version=1`.
- Importante: la detección de conflicto ocurre a nivel de `BookEntity`, no
  de `BookAggregate`. El test debe operar a nivel de repository, no de
  command bus, para aislar el mecanismo.
- Usar `@SpringBootTest` con H2.
- Verificar con `mvn test`.

### - [ ] 5. Test integración: concurrencia real con CountDownLatch

- Dos hilos ejecutan `commandBus.send(new UpdateBookCommand(...))` al mismo
  libro simultáneamente.
- `CountDownLatch` sincroniza el arranque: ambos hilos esperan en el latch
  antes de enviar el comando, para maximizar la probabilidad de colisión.
- Verificar que exactamente uno tiene éxito y el otro lanza excepción.
- Usar `assertTimeoutPreemptively` con timeout generoso (5-10s).
- Este test es timing-dependent por naturaleza. Aceptar que el modo de fallo
  exacto puede variar (podría ser `ObjectOptimisticLockingFailureException`
  directamente, o envuelta en otra excepción según cómo propague el bus).
  Verificar el tipo raíz con `Throwable.getCause()` si es necesario.
- Verificar con `mvn test`.

### - [ ] 6. ADR 0004: Optimistic Locking

- Crear `docs/adr/0004-optimistic-locking.md` documentando:
    - **Por qué optimistic sobre pessimistic:** no queremos locks de DB
      retenidos durante requests HTTP. Pessimistic locking (`SELECT ... FOR UPDATE`)
      bloquea filas mientras el request está en vuelo; bajo carga, eso escala mal
      y puede causar deadlocks.
    - **Dónde vive el campo version:** `BookEntity` (infraestructura), no
      `BookAggregate` (dominio). YAGNI — el dominio no necesita conocer la
      versión de persistencia hoy. Si event sourcing o conditional requests
      lo requieren mañana, se migra.
    - **Qué implica para el cliente:** un 409 significa "tu estado estaba stale,
      re-lee y reintenta". El cliente debe hacer GET + retry del comando con
      datos frescos.
    - **Qué NO resuelve:** conflictos semánticos donde ambas escrituras son
      "válidas" individualmente pero incompatibles juntas (ej: dos usuarios
      cambian campos distintos del mismo libro — ambos pasan validación pero
      uno sobreescribe al otro). Para eso se necesitan merge strategies o
      granularidad de campos, que está fuera de alcance.
    - **Alternativas rechazadas:** pessimistic locking, merge at field level,
      CAS (compare-and-swap) con ETag en HTTP.
- Seguir formato de ADRs existentes (0001-0003): Status, Date, Context,
  Decision, Alternatives considered and rejected, Consequences.

---

## Lo que NO entra

- No sube `version` al dominio (`BookAggregate`).
- No devuelve `ETag` / `If-Match` en respuestas HTTP.
- No implementa retry automático.
- No resuelve conflictos semánticos (merge strategies).
- No cambia el contrato de `BookRepository` (port de dominio).

---

## Criterio de éxito global

- `mvn verify` verde.
- `GlobalExceptionHandler` mapea conflicto de versión a HTTP 409.
- Test secuencial demuestra que `@Version` detecta el conflicto.
- Test de concurrencia demuestra que lost updates no son posibles bajo
  requests simultáneos reales.
- ADR 0004 escrito y consistente con el formato de ADRs existentes.

---

## Notas para Day 11

Cuando Testcontainers Postgres reemplace H2:
- Eliminar el campo `version` del `schema.sql` de test (el archivo entero se borra).
- Verificar que el test de concurrencia (`CountDownLatch`) sigue verde sobre
  Postgres real — el comportamiento de `@Version` es idéntico, pero los
  tiempos de lock pueden variar.
