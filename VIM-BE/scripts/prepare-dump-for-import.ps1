# Patches a mysqldump so wide tables import on MySQL 8.4 (fixes ERROR 1118 row size too large).
# Usage:
#   .\prepare-dump-for-import.ps1 -DumpPath "C:\Users\haris\OneDrive\Desktop\Dump20260512 (1).sql"

param(
    [Parameter(Mandatory = $true)]
    [string]$DumpPath
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $DumpPath)) {
    Write-Error "File not found: $DumpPath"
}

$dir = [System.IO.Path]::GetDirectoryName($DumpPath)
$base = [System.IO.Path]::GetFileNameWithoutExtension($DumpPath)
$outPath = Join-Path $dir ($base + "_patched.sql")

Write-Host "Reading dump (may take 1-2 minutes for large files)..."
$content = [System.IO.File]::ReadAllText($DumpPath)

$header = @"
-- Patched for MySQL 8.4 import
SET NAMES utf8mb4;
SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';
SET SESSION innodb_strict_mode = 0;

"@

# cfg_tbl_customer has 34x varchar(255) -> exceeds InnoDB 8126 byte row limit
$pattern = '(?s)(CREATE TABLE `cfg_tbl_customer` \().*?(\) ENGINE=InnoDB[^;]+;)'
if ($content -match $pattern) {
    $block = $Matches[0]
    $fixed = $block -replace '`txt_([^`]+)` varchar\(255\)', '`txt_$1` text'
    $fixed = $fixed -replace '\) ENGINE=InnoDB', ') ENGINE=InnoDB ROW_FORMAT=DYNAMIC'
    $content = $content.Replace($block, $fixed)
    Write-Host "Patched cfg_tbl_customer (varchar -> text for txt_* columns)."
} else {
    Write-Warning "cfg_tbl_customer block not found; applying generic InnoDB patches only."
}

$content = $content -replace '\) ENGINE=InnoDB DEFAULT CHARSET=latin1;', ') ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=latin1;'
$content = $content -replace '\) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;', ') ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;'
$content = $content -replace '\) ENGINE=InnoDB DEFAULT CHARSET=utf8;', ') ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8;'
$content = $content -replace '\) ENGINE=InnoDB;', ') ENGINE=InnoDB ROW_FORMAT=DYNAMIC;'

[System.IO.File]::WriteAllText($outPath, $header + $content)
Write-Host "Wrote: $outPath"
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. In Workbench, run: VIM-BE\scripts\pre-import-mysql.sql"
Write-Host "  2. Server -> Data Import -> Import from Self-Contained File -> choose the _patched.sql file"
Write-Host "  3. Default Schema: velocity"
