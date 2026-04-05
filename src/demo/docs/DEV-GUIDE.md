# Demo - Development Guide

## Prerequisites

- Docker and Docker Compose
- Make
- Java 17+

## Makefile

All commands are run from `src/demo/`.

| Command          | Description                                      |
|------------------|--------------------------------------------------|
| `make db-up`     | Start PostgreSQL and MongoDB containers           |
| `make db-down`   | Stop and remove the containers                    |
| `make db-status` | Show container status                             |
| `make db-pg`     | Open a `psql` console inside PostgreSQL           |
| `make db-mongo`  | Open a `mongosh` console inside MongoDB           |

## Databases

The project uses two databases following the CQRS pattern:

- **PostgreSQL 15** (write side) - port `5432`
- **MongoDB 7** (read side) - port `27017`

### Start the databases

```bash
make db-up
```

Verify they are running:

```bash
make db-status
```

### Connect to PostgreSQL

```bash
# Via Makefile
make db-pg

# Manually
docker exec -it mycqrs-postgres psql -U postgres -d mycqrsdb
```

Credentials:

| Field    | Value      |
|----------|------------|
| Host     | localhost  |
| Port     | 5432       |
| Database | mycqrsdb   |
| User     | postgres   |
| Password | password   |

### Connect to MongoDB

```bash
# Via Makefile
make db-mongo

# Manually
docker exec -it mycqrs-mongo mongosh
```

The read database is `mycqrs_read` (created automatically when the app starts).

### Stop the databases

```bash
make db-down
```
