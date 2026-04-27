param(
    [string]$RootPath = ".",
    [string]$OutputPath = "scheme-snapshot.json",
    [string]$ComparePath = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-NormalizedRelativePath {
    param(
        [string]$BasePath,
        [string]$FullPath
    )
    $base = [System.IO.Path]::GetFullPath($BasePath).TrimEnd('\', '/')
    $full = [System.IO.Path]::GetFullPath($FullPath)
    $rel = $full.Substring($base.Length).TrimStart('\', '/')
    return $rel -replace '\\', '/'
}

function Should-ExcludePath {
    param([string]$RelativePath)
    $p = $RelativePath.ToLowerInvariant()
    if ($p.StartsWith(".git/")) { return $true }
    if ($p.StartsWith("node_modules/")) { return $true }
    if ($p.StartsWith("vim-fe/node_modules/")) { return $true }
    if ($p.StartsWith("vim-fe/dist/")) { return $true }
    if ($p.StartsWith("vim-fe/.angular/")) { return $true }
    if ($p.StartsWith("vim-fe/coverage/")) { return $true }
    if ($p.StartsWith("vim-fe/.cache/")) { return $true }
    if ($p.StartsWith("vim-fe/.next/")) { return $true }
    if ($p.EndsWith(".log")) { return $true }
    return $false
}

function New-Snapshot {
    param([string]$ProjectRoot)

    $rootFull = [System.IO.Path]::GetFullPath($ProjectRoot)
    if (-not (Test-Path -LiteralPath $rootFull)) {
        throw "Root path does not exist: $rootFull"
    }

    $allFiles = Get-ChildItem -LiteralPath $rootFull -Recurse -File
    $records = New-Object System.Collections.Generic.List[object]
    $totalBytes = 0L

    foreach ($file in $allFiles) {
        $relativePath = Get-NormalizedRelativePath -BasePath $rootFull -FullPath $file.FullName
        if (Should-ExcludePath -RelativePath $relativePath) {
            continue
        }

        $hashInfo = Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256
        $size = [int64]$file.Length
        $totalBytes += $size

        $records.Add([pscustomobject]@{
            path   = $relativePath
            sha256 = $hashInfo.Hash.ToLowerInvariant()
            bytes  = $size
        })
    }

    $sorted = $records | Sort-Object path
    $joined = ($sorted | ForEach-Object { "{0}|{1}" -f $_.path, $_.sha256 }) -join "`n"
    $joinedBytes = [System.Text.Encoding]::UTF8.GetBytes($joined)
    $overallBytes = [System.Security.Cryptography.SHA256]::Create().ComputeHash($joinedBytes)
    $overallHash = ([System.BitConverter]::ToString($overallBytes)).Replace("-", "").ToLowerInvariant()

    return [pscustomobject]@{
        schemaVersion = 1
        rootPath      = $rootFull
        createdAtUtc  = (Get-Date).ToUniversalTime().ToString("o")
        machineName   = $env:COMPUTERNAME
        userName      = $env:USERNAME
        fileCount     = ($sorted | Measure-Object).Count
        totalBytes    = $totalBytes
        overallHash   = $overallHash
        files         = $sorted
    }
}

function Compare-Snapshots {
    param(
        [object]$Old,
        [object]$New
    )

    $oldMap = @{}
    foreach ($f in $Old.files) { $oldMap[$f.path] = $f.sha256 }
    $newMap = @{}
    foreach ($f in $New.files) { $newMap[$f.path] = $f.sha256 }

    $added = New-Object System.Collections.Generic.List[string]
    $removed = New-Object System.Collections.Generic.List[string]
    $changed = New-Object System.Collections.Generic.List[string]

    foreach ($path in $newMap.Keys) {
        if (-not $oldMap.ContainsKey($path)) {
            $added.Add($path)
        } elseif ($oldMap[$path] -ne $newMap[$path]) {
            $changed.Add($path)
        }
    }
    foreach ($path in $oldMap.Keys) {
        if (-not $newMap.ContainsKey($path)) {
            $removed.Add($path)
        }
    }

    return [pscustomobject]@{
        oldOverallHash = $Old.overallHash
        newOverallHash = $New.overallHash
        same           = ($Old.overallHash -eq $New.overallHash)
        addedCount     = $added.Count
        removedCount   = $removed.Count
        changedCount   = $changed.Count
        added          = ($added | Sort-Object)
        removed        = ($removed | Sort-Object)
        changed        = ($changed | Sort-Object)
    }
}

$snapshot = New-Snapshot -ProjectRoot $RootPath
$snapshotJson = $snapshot | ConvertTo-Json -Depth 6
$snapshotJson | Set-Content -LiteralPath $OutputPath -Encoding UTF8

Write-Host "Snapshot written to: $OutputPath"
Write-Host "Overall hash: $($snapshot.overallHash)"
Write-Host "Files hashed: $($snapshot.fileCount)"

if (-not [string]::IsNullOrWhiteSpace($ComparePath)) {
    if (-not (Test-Path -LiteralPath $ComparePath)) {
        throw "Compare file not found: $ComparePath"
    }

    $oldSnapshot = Get-Content -LiteralPath $ComparePath -Raw | ConvertFrom-Json
    $comparison = Compare-Snapshots -Old $oldSnapshot -New $snapshot

    Write-Host ""
    Write-Host "Comparison against: $ComparePath"
    Write-Host "Same overall hash: $($comparison.same)"
    Write-Host "Added: $($comparison.addedCount)  Removed: $($comparison.removedCount)  Changed: $($comparison.changedCount)"

    $reportPath = [System.IO.Path]::ChangeExtension($OutputPath, ".compare.json")
    ($comparison | ConvertTo-Json -Depth 6) | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "Comparison report written to: $reportPath"
}
