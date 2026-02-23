param(
    [string]$PatchPath = "$PSScriptRoot\\uncommitted_changes.patch"
)

if (-not (Test-Path -Path $PatchPath)) {
    Write-Error "Patch file not found: $PatchPath"
    exit 1
}

# Apply patch to working tree
git apply --whitespace=nowarn $PatchPath
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to apply patch."
    exit $LASTEXITCODE
}

Write-Host "Patch applied successfully."
