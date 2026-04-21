# MyCQRS Demo

Demo application built on top of a custom CQRS framework (without Axon), using:

- Spring Boot
- PostgreSQL (Write Side)
- MongoDB (Read Side)
- Docker

---

# 🚀 Quickstart

## 1️⃣ Compile demo module

```bash
mvn -pl src/demo -am clean install
```

## 2️⃣ Run application

```bash
mvn spring-boot:run
```

Application runs on:

```
http://localhost:8080
```

---

# 🐳 Databases

## PostgreSQL (Write Side)

Enter container:

```bash
docker exec -it mycqrs-postgres psql -U postgres -d mycqrsdb
```

Inside psql:

```sql
SELECT * FROM book_entity;
SELECT * FROM book_authors;
SELECT * FROM author_entity;
\q
```

---

## MongoDB (Read Side)

Enter container:

```bash
docker exec -it <mongo_container_name> mongosh
```

Inside mongosh:

```javascript
use mycqrs_read
show collections
```

You should see:

```
books
book_events
authors
author_events
```

Inspect documents:

```javascript
db.books.find().pretty()
db.book_events.find().pretty()
db.authors.find().pretty()
db.author_events.find().pretty()
```

---

# 🧹 Reset Databases (Development)

## 1️⃣ Clean PostgreSQL

```bash
docker exec -it mycqrs-postgres psql -U postgres -d mycqrsdb
```

Inside:

```sql
TRUNCATE TABLE book_entity, author_entity, outbox RESTART IDENTITY CASCADE;
\q
```

`book_authors` is truncated implicitly via the `ON DELETE CASCADE` FK.

---

## 2️⃣ Clean MongoDB

```bash
docker exec -it <mongo_container_name> mongosh
```

Inside:

```javascript
use mycqrs_read
db.books.deleteMany({})
db.book_events.deleteMany({})
db.authors.deleteMany({})
db.author_events.deleteMany({})
```

All four collections should be empty.

---

# 🔥 Smoke Test API

Book and Author IDs are client-assigned UUIDs. Generate them with `uuidgen`
(or `cat /proc/sys/kernel/random/uuid` on Linux).

## Create Author (Command)

```bash
AUTHOR_ID=$(uuidgen)
curl -X PUT "http://localhost:8080/authors/$AUTHOR_ID" \
-H "Content-Type: application/json" \
-d '{
  "firstName": "Robert",
  "lastName": "Martin",
  "birthYear": 1952
}'
```

---

## Rename Author (Command)

```bash
curl -X PATCH "http://localhost:8080/authors/$AUTHOR_ID" \
-H "Content-Type: application/json" \
-d '{
  "firstName": "Uncle Bob",
  "lastName": "Martin"
}'
```

---

## Delete Author (Command, soft-delete)

```bash
curl -X DELETE "http://localhost:8080/authors/$AUTHOR_ID"
```

---

## Create Book (Command)

Books are created without authors; attach them afterwards.

```bash
BOOK_ID=$(uuidgen)
curl -X PUT "http://localhost:8080/books/$BOOK_ID" \
-H "Content-Type: application/json" \
-d '{
  "title": "Clean Architecture"
}'
```

---

## Update Book (Command)

```bash
curl -X PATCH "http://localhost:8080/books/$BOOK_ID" \
-H "Content-Type: application/json" \
-d '{
  "title": "Cleaner Architecture"
}'
```

---

## Add Author to Book (Command)

```bash
curl -X POST "http://localhost:8080/books/$BOOK_ID/authors/$AUTHOR_ID"
```

Responses:
- `200 OK` on success (idempotent — a repeated call is a no-op).
- `404 Not Found` if the author does not exist (`AuthorNotFoundException`).
- `409 Conflict` if the author is soft-deleted (`AuthorRetiredException`).

---

## Remove Author from Book (Command)

```bash
curl -X DELETE "http://localhost:8080/books/$BOOK_ID/authors/$AUTHOR_ID"
```

`204 No Content`. Removing a non-referenced author is a legitimate no-op.

---

## Delete Book (Command)

```bash
curl -X DELETE "http://localhost:8080/books/$BOOK_ID"
```

---

## Query Book (Read side)

```bash
curl "http://localhost:8080/books/$BOOK_ID"
curl "http://localhost:8080/books?title=Clean%20Architecture"
```

Response shape:

```json
{
  "id": "…",
  "title": "Clean Architecture",
  "authors": [
    { "authorId": "…", "fullName": "Uncle Bob Martin", "retired": false }
  ]
}
```

---

## Query Author (Read side)

```bash
curl "http://localhost:8080/authors/$AUTHOR_ID"
```

Response shape:

```json
{
  "id": "…",
  "firstName": "Uncle Bob",
  "lastName": "Martin",
  "birthYear": 1952,
  "deleted": false,
  "books": [
    { "bookId": "…", "title": "Clean Architecture" }
  ]
}
```

---

# 📌 Development Notes

- Write side persists aggregates in PostgreSQL; `book_entity`, `book_authors`
  and `author_entity` live there. No FK between `book_authors.author_id` and
  `author_entity.id` — cross-aggregate consistency is eventual, enforced via
  events, not via the schema.
- Events are published transactionally into the `outbox` table and polled
  asynchronously into the internal event bus.
- Read side projections denormalize bi-directionally: each Book embeds a
  summary of its authors, and each Author embeds a summary of its books.
  Author rename / soft-delete propagate into every embedded reference.
- This project follows:
  - CQRS
  - Hexagonal Architecture
  - DDD-inspired aggregates
