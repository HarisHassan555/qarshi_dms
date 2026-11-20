# Copies GridCraft Trunk build (dist/web) into Angular assets for /excelpro.
# Rebuild first (from gridcraft repo, with Rust + trunk installed):
#   cd apps/gridcraft-web
#   trunk build --release
# Embed UI (title bar, File tab, extra ribbon tabs) is trimmed when host=parent.
param(
    [string]$GridcraftRoot = "$env:USERPROFILE\Desktop\DSG\gridcraft",
    [string]$Dest = "$PSScriptRoot\..\src\assets\gridcraft"
)

$src = Join-Path $GridcraftRoot "dist\web"
if (-not (Test-Path $src)) {
    Write-Error "Missing $src - run: cd apps/gridcraft-web; trunk build --release"
    exit 1
}

New-Item -ItemType Directory -Force -Path $Dest | Out-Null
Copy-Item -Path (Join-Path $src "*") -Destination $Dest -Recurse -Force
Write-Host "Copied GridCraft web build to $Dest"
