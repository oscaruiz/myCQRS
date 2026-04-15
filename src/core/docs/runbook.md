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
```

Inspect documents:

```javascript
db.books.find().pretty()
db.book_events.find().pretty()
```

---

# 🧹 Reset Databases (Development)

## 1️⃣ Clean PostgreSQL

```bash
docker exec -it mycqrs-postgres psql -U postgres -d mycqrsdb
```

Inside:

```sql
DELETE FROM book_entity;
SELECT * FROM book_entity;
\q
```

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

db.books.find()
db.book_events.find()
```

Both collections should be empty.

---

# 🔥 Smoke Test API

Book IDs are client-assigned UUIDs. Generate one with `uuidgen` (or
`cat /proc/sys/kernel/random/uuid` on Linux).

## Create Book (Command)

```bash
BOOK_ID=$(uuidgen)
curl -X PUT "http://localhost:8080/books/$BOOK_ID" \
-H "Content-Type: application/json" \
-d '{
  "title": "Clean Architecture",
  "author": "Robert C. Martin"
}'
```

---

## Update Book (Command)

```bash
curl -X PATCH "http://localhost:8080/books/$BOOK_ID" \
-H "Content-Type: application/json" \
-d '{
  "title": "Cleaner Architecture",
  "author": "Roberto C. Martino"
}'
```

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

---

# 📌 Development Notes

- Write side persists data in PostgreSQL
- Events are published via your custom EventBus
- Read side projections are stored in MongoDB
- This project follows:
  - CQRS
  - Hexagonal Architecture
  - DDD-inspired aggregates
