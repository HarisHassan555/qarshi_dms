# Run this script in PowerShell AS ADMINISTRATOR (right-click -> Run as administrator).
# Configures MySQL 8.4 for this project: port 3308, database "velocity", password from application.properties.

$ErrorActionPreference = "Stop"

$mysqlHome = "C:\Program Files\MySQL\MySQL Server 8.4"
$dataDir   = "C:\ProgramData\MySQL\MySQL Server 8.4\Data"
$myIni     = Join-Path $mysqlHome "my.ini"
$service   = "MySQL84"
$port      = 3308
$dbName    = "velocity"
$rootPass  = "dsg123"

if (-not (Test-Path "$mysqlHome\bin\mysqld.exe")) {
    Write-Error "MySQL Server not found at $mysqlHome. Install MySQL Server 8.4 first."
}

New-Item -ItemType Directory -Force -Path $dataDir | Out-Null

$iniContent = @"
[mysqld]
basedir=$($mysqlHome -replace '\\','/')
datadir=$($dataDir -replace '\\','/')
port=$port
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
innodb_default_row_format=DYNAMIC
innodb_strict_mode=0

[mysql]
default-character-set=utf8mb4

[client]
port=$port
default-character-set=utf8mb4
"@

Set-Content -Path $myIni -Value $iniContent -Encoding ASCII
Write-Host "Wrote $myIni"

$svc = Get-Service -Name $service -ErrorAction SilentlyContinue
if (-not $svc) {
    if (-not (Test-Path (Join-Path $dataDir "mysql"))) {
        Write-Host "Initializing data directory (first run only)..."
        & "$mysqlHome\bin\mysqld.exe" --defaults-file="$myIni" --initialize-insecure
    }
    Write-Host "Installing Windows service '$service'..."
    & "$mysqlHome\bin\mysqld.exe" --install $service --defaults-file="$myIni"
} elseif (-not (Test-Path (Join-Path $dataDir "mysql"))) {
    Write-Error "Service exists but data directory is empty. Remove service and re-run, or run MySQL Installer."
}

Write-Host "Starting MySQL on port $port..."
Start-Service -Name $service
Start-Sleep -Seconds 3

Write-Host "Setting root password and creating database '$dbName'..."
$sql = @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPass';
CREATE DATABASE IF NOT EXISTS $dbName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
"@

& "$mysqlHome\bin\mysql.exe" -h 127.0.0.1 -P $port -u root --skip-password -e $sql

Write-Host ""
Write-Host "MySQL is ready."
Write-Host "  Port:     $port"
Write-Host "  Database: $dbName"
Write-Host "  User:     root"
Write-Host "  Password: $rootPass"
Write-Host ""
Write-Host "Test: mysql -h 127.0.0.1 -P $port -u root -p$dbName -e ""SELECT 1"""
