# Copies WordCraft Trunk build (dist/web) into Angular assets for /excelpro (Word mode).
# Rebuild first (from wordcraft repo, with Rust + trunk installed):
#   cd apps/wordcraft-web
#   trunk build --release
param(
    [string]$WordcraftRoot = "$env:USERPROFILE\Desktop\DSG\wordcraft",
    [string]$Dest = "$PSScriptRoot\..\src\assets\wordcraft"
)

$src = Join-Path $WordcraftRoot "dist\web"
if (-not (Test-Path $src)) {
    Write-Error "Missing $src - run: cd apps/wordcraft-web; trunk build --release"
    exit 1
}

New-Item -ItemType Directory -Force -Path $Dest | Out-Null
Copy-Item -Path (Join-Path $src "*") -Destination $Dest -Recurse -Force
Write-Host "Copied WordCraft web build to $Dest"
