# PowerShell script pentru inițializarea bazelor de date multiple în PostgreSQL
# Echivalent PowerShell pentru init-postgres.sh

# Setare strict mode pentru erori
$ErrorActionPreference = "Stop"

function Create-Database {
    param (
        [Parameter(Mandatory=$true)]
        [string]$DatabaseName
    )
    
    Write-Host "Creating database '$DatabaseName'"
    
    $sqlCommand = @"
CREATE DATABASE $DatabaseName;
GRANT ALL PRIVILEGES ON DATABASE $DatabaseName TO $env:POSTGRES_USER;
"@
    
    # Executare psql cu parametrii corecți
    $psqlArgs = @(
        "-v", "ON_ERROR_STOP=1",
        "--username", "$env:POSTGRES_USER",
        "-c", $sqlCommand
    )
    
    & psql @psqlArgs
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create database '$DatabaseName'"
    }
}

# Main logic
if ($env:POSTGRES_MULTIPLE_DATABASES) {
    Write-Host "Multiple database creation requested: $env:POSTGRES_MULTIPLE_DATABASES"
    
    # Split pe virgulă și trim whitespace
    $databases = $env:POSTGRES_MULTIPLE_DATABASES -split ',' | ForEach-Object { $_.Trim() }
    
    foreach ($db in $databases) {
        if ($db) {
            Create-Database -DatabaseName $db
        }
    }
    
    Write-Host "Multiple databases created successfully"
} else {
    Write-Host "No multiple databases specified in POSTGRES_MULTIPLE_DATABASES"
}
