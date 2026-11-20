# Rebuild VIM-BE and restart the embedded Spring Boot WAR on port 8080.
# Usage (from any shell):
#   powershell -ExecutionPolicy Bypass -File .\rebuild-and-run.ps1
# Optional:
#   .\rebuild-and-run.ps1 -SkipClean
#   .\rebuild-and-run.ps1 -Port 8080

param(
    [int]$Port = 8080,
    [switch]$SkipClean
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }
    $candidates = @(
        "C:\Users\P1\.jdks\ms-17.0.19",
        "$env:USERPROFILE\.jdks\ms-17.0.19"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path (Join-Path $candidate "bin\java.exe")) {
            return $candidate
        }
    }
    $found = Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^(ms|jdk|temurin|corretto).*17' } |
        Select-Object -First 1
    if ($found -and (Test-Path (Join-Path $found.FullName "bin\java.exe"))) {
        return $found.FullName
    }
    throw "Java 17 not found. Set JAVA_HOME to a JDK 17 install."
}

function Stop-BackendOnPort {
    param([int]$ListenPort)

    Write-Host "Stopping anything on port $ListenPort / velocity.war ..."

    $pids = @()
    try {
        $pids += Get-NetTCPConnection -LocalPort $ListenPort -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
    } catch { }

    Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -match '^(java|javaw)\.exe$' -and
            $_.CommandLine -and
            ($_.CommandLine -match 'velocity\.war' -or $_.CommandLine -match 'SpringBootSecurityJwtApplication')
        } |
        ForEach-Object { $pids += $_.ProcessId }

    $pids = $pids | Where-Object { $_ -and $_ -gt 0 } | Select-Object -Unique
    foreach ($procId in $pids) {
        try {
            Stop-Process -Id $procId -Force -ErrorAction Stop
            Write-Host "  stopped PID $procId"
        } catch {
            Write-Host "  could not stop PID $procId : $($_.Exception.Message)"
        }
    }

    Start-Sleep -Seconds 2
}

$javaHome = Resolve-JavaHome
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;" + $env:Path

Write-Host "JAVA_HOME=$javaHome"
cmd /c "java -version 2>&1" | Select-Object -First 1

Stop-BackendOnPort -ListenPort $Port

$mvnw = Join-Path $ProjectRoot "mvnw.cmd"
if (-not (Test-Path $mvnw)) {
    throw "Maven wrapper not found: $mvnw"
}

$buildArgs = @()
if (-not $SkipClean) {
    $buildArgs += "clean"
}
$buildArgs += @("package", "-DskipTests")

Write-Host ""
Write-Host "Building: .\mvnw.cmd $($buildArgs -join ' ')"
& $mvnw @buildArgs
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE"
}

$war = Join-Path $ProjectRoot "target\velocity.war"
if (-not (Test-Path $war)) {
    throw "WAR not found after build: $war"
}

$logsDir = Join-Path $ProjectRoot "logs"
New-Item -ItemType Directory -Force -Path $logsDir | Out-Null
$logFile = Join-Path $logsDir "spring-app.log"
# Fresh log for this run so startup detection is not confused by older lines.
if (Test-Path $logFile) {
    Remove-Item $logFile -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "Starting backend: $war"
Write-Host "  context: http://localhost:$Port/velocity/"
Write-Host "  log:     $logFile"

$javaExe = Join-Path $javaHome "bin\java.exe"
$javaArgs = @(
    "--add-opens", "java.base/java.security=ALL-UNNAMED",
    "-jar", $war,
    "--server.port=$Port",
    "--logging.file.name=$logFile",
    "--app.backend.url=http://localhost:$Port/velocity"
)

$proc = Start-Process -FilePath $javaExe -ArgumentList $javaArgs -WorkingDirectory $ProjectRoot -WindowStyle Hidden -PassThru
Write-Host "  PID: $($proc.Id)"

$deadline = (Get-Date).AddMinutes(2)
$started = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if (Test-Path $logFile) {
        $tail = Get-Content $logFile -Tail 40 -ErrorAction SilentlyContinue
        if ($tail -match "Started SpringBootSecurityJwtApplication") {
            $started = $true
            break
        }
        if ($tail -match "APPLICATION FAILED TO START|Address already in use") {
            throw "Backend failed to start. Check $logFile"
        }
    }
    if ($proc.HasExited) {
        throw "Backend process exited early (code $($proc.ExitCode)). Check $logFile"
    }
}

if (-not $started) {
    Write-Host "Timed out waiting for startup line; process still running (PID $($proc.Id)). Check $logFile"
    exit 0
}

try {
    $status = (Invoke-WebRequest -Uri "http://localhost:$Port/velocity/" -UseBasicParsing -TimeoutSec 10).StatusCode
    Write-Host "Backend is up (HTTP $status): http://localhost:$Port/velocity/"
} catch {
    Write-Host "Startup log OK, but HTTP check failed: $($_.Exception.Message)"
}
