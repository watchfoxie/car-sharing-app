# Makefile pentru Car Sharing App - Docker operations
.PHONY: help build up down restart logs clean ps health test-build

# Default target
.DEFAULT_GOAL := help

# Variables
COMPOSE_FILE = docker-compose.yaml
DOCKER_COMPOSE = docker compose -f $(COMPOSE_FILE)

help: ## Arată acest mesaj de ajutor
	@echo "Car Sharing App - Docker Management"
	@echo ""
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

build: ## Build toate imaginile Docker
	$(DOCKER_COMPOSE) build --no-cache

build-fast: ## Build toate imaginile Docker (cu cache)
	$(DOCKER_COMPOSE) build

up: ## Pornește toate serviciile
	$(DOCKER_COMPOSE) up -d

up-infra: ## Pornește doar infrastructura (Postgres, Redis, Kafka, Keycloak, Prometheus, Zipkin)
	$(DOCKER_COMPOSE) up -d postgres redis zookeeper kafka keycloak prometheus zipkin

down: ## Oprește toate serviciile
	$(DOCKER_COMPOSE) down

down-volumes: ## Oprește toate serviciile și șterge volume-urile
	$(DOCKER_COMPOSE) down -v

restart: ## Restart toate serviciile
	$(DOCKER_COMPOSE) restart

restart-%: ## Restart un serviciu specific (ex: make restart-car-service)
	$(DOCKER_COMPOSE) restart $*

logs: ## Arată logs pentru toate serviciile
	$(DOCKER_COMPOSE) logs -f

logs-%: ## Arată logs pentru un serviciu specific (ex: make logs-api-gateway)
	$(DOCKER_COMPOSE) logs -f $*

ps: ## Listează serviciile care rulează
	$(DOCKER_COMPOSE) ps

health: ## Verifică health status pentru toate serviciile
	@echo "=== Infrastructure Services ==="
	@curl -s http://localhost:5432 > /dev/null 2>&1 && echo "✓ PostgreSQL: UP" || echo "✗ PostgreSQL: DOWN"
	@redis-cli -h localhost -p 6379 -a redis_dev_password ping > /dev/null 2>&1 && echo "✓ Redis: UP" || echo "✗ Redis: DOWN"
	@curl -s http://localhost:9092 > /dev/null 2>&1 && echo "✓ Kafka: UP" || echo "✗ Kafka: DOWN"
	@curl -s http://localhost:9090/health/ready > /dev/null 2>&1 && echo "✓ Keycloak: UP" || echo "✗ Keycloak: DOWN"
	@curl -s http://localhost:9091/-/healthy > /dev/null 2>&1 && echo "✓ Prometheus: UP" || echo "✗ Prometheus: DOWN"
	@curl -s http://localhost:9411/health > /dev/null 2>&1 && echo "✓ Zipkin: UP" || echo "✗ Zipkin: DOWN"
	@echo ""
	@echo "=== Platform Services ==="
	@curl -s http://localhost:8761/actuator/health | grep -q '"status":"UP"' && echo "✓ Discovery Service: UP" || echo "✗ Discovery Service: DOWN"
	@curl -s http://localhost:8888/actuator/health | grep -q '"status":"UP"' && echo "✓ Config Service: UP" || echo "✗ Config Service: DOWN"
	@curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"' && echo "✓ API Gateway: UP" || echo "✗ API Gateway: DOWN"
	@echo ""
	@echo "=== Business Services ==="
	@curl -s http://localhost:8081/actuator/health | grep -q '"status":"UP"' && echo "✓ Identity Adapter: UP" || echo "✗ Identity Adapter: DOWN"
	@curl -s http://localhost:8082/actuator/health | grep -q '"status":"UP"' && echo "✓ Car Service: UP" || echo "✗ Car Service: DOWN"
	@curl -s http://localhost:8083/actuator/health | grep -q '"status":"UP"' && echo "✓ Pricing Rules Service: UP" || echo "✗ Pricing Rules Service: DOWN"
	@curl -s http://localhost:8084/actuator/health | grep -q '"status":"UP"' && echo "✓ Rental Service: UP" || echo "✗ Rental Service: DOWN"
	@curl -s http://localhost:8085/actuator/health | grep -q '"status":"UP"' && echo "✓ Feedback Service: UP" || echo "✗ Feedback Service: DOWN"
	@echo ""
	@echo "=== Frontend ==="
	@curl -s http://localhost:4200/health > /dev/null 2>&1 && echo "✓ Frontend App: UP" || echo "✗ Frontend App: DOWN"

clean: ## Curăță toate resursele Docker (containere, imagini, volume-uri)
	$(DOCKER_COMPOSE) down -v --rmi all --remove-orphans
	docker system prune -af --volumes

test-build: ## Testează doar build-ul (fără pornire)
	$(DOCKER_COMPOSE) build
	@echo "Build completed successfully!"

shell-%: ## Deschide shell în container (ex: make shell-api-gateway)
	$(DOCKER_COMPOSE) exec $* /bin/sh

db-migrate: ## Rulează Flyway migrations manual
	@echo "Migrations rulează automat la pornirea serviciilor"
	@echo "Pentru re-migrare, vezi logs: make logs-car-service"

db-psql: ## Conectează la PostgreSQL via psql
	docker exec -it car-sharing-postgres psql -U carsharing -d car_sharing_db

redis-cli: ## Conectează la Redis via redis-cli
	docker exec -it car-sharing-redis redis-cli -a redis_dev_password

kafka-topics: ## Listează Kafka topics
	docker exec -it car-sharing-kafka kafka-topics --bootstrap-server localhost:9092 --list

monitoring: ## Deschide interfețele de monitoring
	@echo "Opening monitoring dashboards..."
	@echo "Prometheus: http://localhost:9091"
	@echo "Zipkin: http://localhost:9411"
	@echo "Keycloak: http://localhost:9090 (admin/admin)"
	@echo "Eureka: http://localhost:8761"

urls: ## Arată toate URL-urile disponibile
	@echo "=== Car Sharing App - Service URLs ==="
	@echo ""
	@echo "Frontend:"
	@echo "  http://localhost:4200"
	@echo ""
	@echo "API Gateway:"
	@echo "  http://localhost:8080"
	@echo "  http://localhost:8080/swagger-ui.html"
	@echo ""
	@echo "Microservices (direct access):"
	@echo "  Identity Adapter:       http://localhost:8081/swagger-ui.html"
	@echo "  Car Service:            http://localhost:8082/swagger-ui.html"
	@echo "  Pricing Rules Service:  http://localhost:8083/swagger-ui.html"
	@echo "  Rental Service:         http://localhost:8084/swagger-ui.html"
	@echo "  Feedback Service:       http://localhost:8085/swagger-ui.html"
	@echo ""
	@echo "Platform Services:"
	@echo "  Eureka Dashboard:       http://localhost:8761"
	@echo "  Config Server:          http://localhost:8888"
	@echo ""
	@echo "Infrastructure:"
	@echo "  Keycloak Admin:         http://localhost:9090 (admin/admin)"
	@echo "  Prometheus:             http://localhost:9091"
	@echo "  Zipkin:                 http://localhost:9411"
	@echo ""
	@echo "Test Users:"
	@echo "  Admin:   admin@carsharing.local / admin123"
	@echo "  Manager: manager@carsharing.local / manager123"
	@echo "  User:    user@carsharing.local / user123"
