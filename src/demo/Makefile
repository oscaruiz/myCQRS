.PHONY: db-up db-down db-pg db-mongo db-status

db-up:
	docker compose up -d

db-down:
	docker compose down

db-pg:
	docker exec -it mycqrs-postgres psql -U postgres -d mycqrsdb

db-mongo:
	docker exec -it mycqrs-mongo mongosh

db-status:
	docker compose ps
