# Car Sharing Application - Quick Start Script (NO DATABASE)
# PowerShell script pentru testarea rapidă a integrării SpringDoc OpenAPI fără PostgreSQL

Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  Car Sharing - SpringDoc OpenAPI Testing (No DB)   " -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[INFO] This script starts the application WITHOUT database" -ForegroundColor Yellow
Write-Host "[INFO] Perfect for testing SpringDoc OpenAPI integration" -ForegroundColor Yellow
Write-Host ""

# Check if Maven Wrapper exists
if (-Not (Test-Path ".\mvnw.cmd")) {
    Write-Host "[ERROR] Maven Wrapper (mvnw.cmd) not found!" -ForegroundColor Red
    Write-Host "Please run this script from the project root directory." -ForegroundColor Yellow
    exit 1
}

Write-Host "[INFO] Starting Car Sharing Application..." -ForegroundColor Green
Write-Host "[INFO] Profile: dev-no-db (No Database Required)" -ForegroundColor Green
Write-Host "[INFO] Port: 8080" -ForegroundColor Green
Write-Host ""

Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  Building Application..." -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan

# Build the application
.\mvnw.cmd clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  Starting Spring Boot Application (No Database)..." -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[SUCCESS] Application will start WITHOUT database!" -ForegroundColor Green
Write-Host ""
Write-Host "[INFO] Available endpoints:" -ForegroundColor Yellow
Write-Host "  ✓ Swagger UI:          http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "  ✓ OpenAPI JSON:        http://localhost:8080/v3/api-docs" -ForegroundColor White
Write-Host "  ✓ OpenAPI YAML:        http://localhost:8080/v3/api-docs.yaml" -ForegroundColor White
Write-Host "  ✓ Health Check:        http://localhost:8080/api/v1/health" -ForegroundColor White
Write-Host "  ✓ App Info:            http://localhost:8080/api/v1/health/info" -ForegroundColor White
Write-Host "  ✓ Actuator:            http://localhost:8080/actuator" -ForegroundColor White
Write-Host "  ✓ Actuator Health:     http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "  ✓ Prometheus Metrics:  http://localhost:8080/actuator/prometheus" -ForegroundColor White
Write-Host ""

Write-Host "[INFO] Press Ctrl+C to stop the application" -ForegroundColor Yellow
Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

# Run the application with dev-no-db profile (no database required)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev-no-db"

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  Application Stopped" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
