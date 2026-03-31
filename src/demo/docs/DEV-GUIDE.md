# Demo - Guia de desarrollo

## Requisitos previos

- Docker y Docker Compose
- Make
- Java 17+

## Makefile

Todos los comandos se ejecutan desde `src/demo/`.

| Comando          | Descripcion                                      |
|------------------|--------------------------------------------------|
| `make db-up`     | Levanta los contenedores de PostgreSQL y MongoDB  |
| `make db-down`   | Detiene y elimina los contenedores                |
| `make db-status` | Muestra el estado de los contenedores             |
| `make db-pg`     | Abre una consola `psql` dentro de PostgreSQL      |
| `make db-mongo`  | Abre una consola `mongosh` dentro de MongoDB      |

## Bases de datos

El proyecto usa dos bases de datos siguiendo el patron CQRS:

- **PostgreSQL 15** (write side) - puerto `5432`
- **MongoDB 7** (read side) - puerto `27017`

### Levantar las bases de datos

```bash
make db-up
```

Verifica que esten corriendo:

```bash
make db-status
```

### Conectarse a PostgreSQL

```bash
# Via Makefile
make db-pg

# Manualmente
docker exec -it mycqrs-postgres psql -U postgres -d mycqrsdb
```

Credenciales:

| Campo    | Valor      |
|----------|------------|
| Host     | localhost  |
| Puerto   | 5432       |
| Base     | mycqrsdb   |
| Usuario  | postgres   |
| Password | password   |

### Conectarse a MongoDB

```bash
# Via Makefile
make db-mongo

# Manualmente
docker exec -it mycqrs-mongo mongosh
```

La base de datos de lectura es `mycqrs_read` (se crea automaticamente al arrancar la app).

### Detener las bases de datos

```bash
make db-down
```
