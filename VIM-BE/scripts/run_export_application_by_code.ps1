# Runs export_application_by_code.sql against MySQL.
# Usage:
#   .\run_export_application_by_code.ps1
#   .\run_export_application_by_code.ps1 -FormCode "CAPF-0144"
#   .\run_export_application_by_code.ps1 -FormCode "CAPF-0144" -Host 127.0.0.1 -Port 3308 -Database velocity_workbench -User root -Password "strongpassword"

param(
    [string]$FormCode = "CAPF-0144",
    [string]$Host = "127.0.0.1",
    [int]$Port = 3308,
    [string]$Database = "velocity_workbench",
    [string]$User = "root",
    [string]$Password = "strongpassword",
    [string]$OutputFile = ""
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDir "export_application_by_code.sql"

if (-not (Test-Path $sqlFile)) {
    throw "SQL file not found: $sqlFile"
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    throw "mysql CLI not found on PATH. Install MySQL client or run export_application_by_code.sql in Workbench."
}

$tempSql = Join-Path $env:TEMP ("export_app_" + [guid]::NewGuid().ToString("N") + ".sql")
$content = Get-Content $sqlFile -Raw
$content = $content -replace "SET @form_code = 'CAPF-0144';", "SET @form_code = '$($FormCode -replace "'", "''")';"
Set-Content -Path $tempSql -Value $content -Encoding UTF8

if ([string]::IsNullOrWhiteSpace($OutputFile)) {
    $safeCode = $FormCode -replace '[^\w\-]', '_'
    $OutputFile = Join-Path $scriptDir ("export_" + $safeCode + "_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".txt")
}

$argList = @(
    "-h", $Host,
    "-P", $Port,
    "-u", $User,
    "-p$Password",
    "--default-character-set=utf8mb4",
    $Database
)

Write-Host "Exporting application: $FormCode"
Write-Host "Database: ${User}@${Host}:${Port}/${Database}"
Write-Host "Output: $OutputFile"

& $mysql.Source @argList 2>&1 | Tee-Object -FilePath $OutputFile

Remove-Item $tempSql -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Done. Review sections:"
Write-Host "  - Application row (status, level, current approver)"
Write-Host "  - Full approval history table"
Write-Host "  - LAST_CORE_TEAM_APPROVED (remarks for Core team)"
Write-Host "  - Raw JSON at end (backup for rollback)"
