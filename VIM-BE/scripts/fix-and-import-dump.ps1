# Fix ROW_FORMAT for large tables and import SQL dump into MySQL (port 3308).
param(
    [string]$SourceDump = "C:\Users\haris\OneDrive\Desktop\Dump20260512 (1).sql",
    [string]$TargetDb = "haris",
    [int]$Port = 3308,
    [string]$MysqlHome = "C:\Program Files\MySQL\MySQL Server 8.4"
)

$ErrorActionPreference = "Stop"
$mysql = Join-Path $MysqlHome "bin\mysql.exe"
$fixedDump = Join-Path $env:TEMP "Dump20260512_fixed.sql"

if (-not (Test-Path $SourceDump)) { throw "Dump not found: $SourceDump" }
if (-not (Test-Path $mysql)) { throw "mysql.exe not found: $mysql" }

Write-Host "Preparing fixed dump (add ROW_FORMAT=DYNAMIC, target DB: $TargetDb)..."
$reader = [System.IO.StreamReader]::new($SourceDump)
$writer = [System.IO.StreamWriter]::new($fixedDump, $false, [System.Text.UTF8Encoding]::new($false))

$writer.WriteLine("SET NAMES utf8mb4;")
$writer.WriteLine("SET FOREIGN_KEY_CHECKS=0;")
$writer.WriteLine("SET UNIQUE_CHECKS=0;")
$writer.WriteLine("SET SESSION innodb_strict_mode=0;")
$writer.WriteLine("CREATE DATABASE IF NOT EXISTS ``$TargetDb`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
$writer.WriteLine("USE ``$TargetDb``;")

$lineCount = 0
while ($null -ne ($line = $reader.ReadLine())) {
    $lineCount++
    if ($lineCount % 50000 -eq 0) { Write-Host "  processed $lineCount lines..." }

    if ($line -match 'CREATE DATABASE\s+IF NOT EXISTS\s+`velocity`') { continue }
    if ($line -match '^\s*USE\s+`velocity`\s*;\s*$') { continue }

    if ($line -match '\)\s*ENGINE=InnoDB' -and $line -notmatch 'ROW_FORMAT') {
        $line = $line -replace '\)\s*ENGINE=InnoDB', ') ENGINE=InnoDB ROW_FORMAT=DYNAMIC'
    }

    $writer.WriteLine($line)
}

$reader.Close()
$writer.Close()
Write-Host "Fixed dump written to: $fixedDump"

Write-Host "Importing (this may take several minutes)..."
$logFile = Join-Path $env:TEMP "mysql-import-haris.log"
$importArgs = @(
    "-h", "127.0.0.1", "-P", "$Port", "-u", "root", "-pdsg123",
    "--max-allowed-packet=1G", "--default-character-set=utf8mb4", $TargetDb
)
cmd /c "`"$mysql`" $($importArgs -join ' ') < `"$fixedDump`" > `"$logFile`" 2>&1"
$importExit = $LASTEXITCODE

if ($importExit -ne 0) {
    Write-Host "Import failed. Last 30 lines of log:"
    Get-Content $logFile -Tail 30
    exit $importExit
}

Write-Host "Import completed successfully."
& $mysql -h 127.0.0.1 -P $Port -u root -pdsg123 -e "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='$TargetDb';" 2>&1
