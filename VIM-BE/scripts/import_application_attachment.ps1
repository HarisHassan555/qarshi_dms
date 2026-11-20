# Embed a local file into cfg_tbl_custom_form_application.txt_application_data
# (templateValues + root mirror), same shape the Angular app uses.
#
# Usage:
#   .\import_application_attachment.ps1 -ApplicationId 406 -FilePath "C:\path\4100060234 FOR PS United plastic (1).pdf"
#   .\import_application_attachment.ps1 -ApplicationId 406 -FilePath ".\file.pdf" -Database velocity -Port 3308
#   .\import_application_attachment.ps1 -ApplicationId 406 -FilePath ".\file.pdf" -FieldId "your-template-field-uuid" -Mode append
#
# Requires: mysql client on PATH. DB must allow large packets (see note at end).

param(
    [Parameter(Mandatory = $true)]
    [int]$ApplicationId,

    [Parameter(Mandatory = $true)]
    [string]$FilePath,

    [string]$FieldId = "",

    [ValidateSet("replace", "append")]
    [string]$Mode = "replace",

    [string]$Host = "127.0.0.1",
    [int]$Port = 3308,
    [string]$Database = "velocity",
    [string]$User = "root",
    [string]$Password = "dsg123",

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $FilePath)) {
    throw "File not found: $FilePath"
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    throw "mysql CLI not found on PATH. Install MySQL client or run the generated .sql in Workbench."
}

$resolvedPath = (Resolve-Path -LiteralPath $FilePath).Path
$fileName = [System.IO.Path]::GetFileName($resolvedPath)
$ext = [System.IO.Path]::GetExtension($fileName).ToLowerInvariant()
$mimeType = switch ($ext) {
    ".pdf" { "application/pdf" }
    ".png" { "image/png" }
    ".jpg" { "image/jpeg" }
    ".jpeg" { "image/jpeg" }
    ".webp" { "image/webp" }
    default { "application/octet-stream" }
}

$fileBytes = [System.IO.File]::ReadAllBytes($resolvedPath)
$base64 = [Convert]::ToBase64String($fileBytes)

Write-Host "ApplicationId: $ApplicationId"
Write-Host "File: $fileName ($($fileBytes.Length) bytes, mime=$mimeType)"
Write-Host "Base64 length: $($base64.Length) characters"
Write-Host "Database: ${User}@${Host}:${Port}/${Database}"
Write-Host "Mode: $Mode"

$mysqlArgs = @(
    "-h", $Host,
    "-P", $Port,
    "-u", $User,
    "-p$Password",
    "--default-character-set=utf8mb4",
    "-N", "-B",
    $Database
)

function Invoke-MySqlScalar([string]$Query) {
    $out = & $mysql.Source @mysqlArgs "-e" $Query 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL error: $out"
    }
    return ($out | Out-String).Trim()
}

if ([string]::IsNullOrWhiteSpace($FieldId)) {
    $discoverQuery = @"
SELECT fld.field_id
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(
        JSON_EXTRACT(
            CASE
                WHEN JSON_TYPE(a.txt_application_data) = 'STRING' THEN CAST(JSON_UNQUOTE(a.txt_application_data) AS JSON)
                ELSE a.txt_application_data
            END,
            '$.templatePayload.fields'
        ),
        JSON_ARRAY()
    ),
    '$[*]' COLUMNS (
        field_id VARCHAR(255) PATH '$.id',
        field_type VARCHAR(64) PATH '$.type'
    )
) fld
WHERE a.ser_application_id = $ApplicationId
  AND LOWER(TRIM(fld.field_type)) = 'attachment'
LIMIT 1;
"@
    $FieldId = Invoke-MySqlScalar $discoverQuery
    if ([string]::IsNullOrWhiteSpace($FieldId)) {
        throw "Could not find attachment field id in templatePayload for application $ApplicationId. Pass -FieldId explicitly."
    }
    Write-Host "Discovered attachment field id: $FieldId"
}

# Escape for SQL string literals (base64 is quote-safe; filenames may not be).
function Escape-SqlLiteral([string]$Value) {
    return ($Value -replace "'", "''")
}

$escFieldId = Escape-SqlLiteral $FieldId
$escFileName = Escape-SqlLiteral $fileName
$escMime = Escape-SqlLiteral $mimeType

$tempSql = Join-Path $env:TEMP ("import_attachment_" + [guid]::NewGuid().ToString("N") + ".sql")

$modeSql = if ($Mode -eq "append") {
@"
SET @new_item = JSON_OBJECT(
    'fileName', @file_name,
    'mimeType', @mime_type,
    'base64', @b64
);
SET @cur_tv = JSON_EXTRACT(@data_json, @tv_path);
SET @merged = CASE
    WHEN @cur_tv IS NULL OR JSON_TYPE(@cur_tv) <> 'ARRAY' THEN JSON_ARRAY(@new_item)
    ELSE JSON_ARRAY_APPEND(@cur_tv, '$', CAST(@new_item AS JSON))
END;
SET @data_after = JSON_SET(
    JSON_SET(@data_json, @root_path, @merged),
    @tv_path,
    @merged
);
"@
} else {
@"
SET @payload_text = CONCAT(
    '[{"fileName":"',
    REPLACE(REPLACE(@file_name, '\\', '\\\\'), '"', '\\"'),
    '","mimeType":"',
    REPLACE(REPLACE(@mime_type, '\\', '\\\\'), '"', '\\"'),
    '","base64":"',
    @b64,
    '"}]'
);
SET @payload = CAST(@payload_text AS JSON);
SET @patch_text = CONCAT(
    '{',
    '"templateValues":{',
    JSON_QUOTE(@field_id),
    ':',
    @payload_text,
    '},',
    JSON_QUOTE(@field_id),
    ':',
    @payload_text,
    '}'
);
SET @patch = CAST(@patch_text AS JSON);
SET @data_after = JSON_MERGE_PATCH(@data_json, @patch);
"@
}

$sql = @"
-- database selected via mysql CLI argument
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
START TRANSACTION;

SET @app_id = $ApplicationId;
SET @field_id = '$escFieldId';
SET @file_name = '$escFileName';
SET @mime_type = '$escMime';
SET @b64 = '$base64';

SET @root_path = CONCAT('$.', JSON_QUOTE(@field_id));
SET @tv_path = CONCAT('$.templateValues.', JSON_QUOTE(@field_id));

SET @data_json = (
    SELECT
        CASE
            WHEN a.txt_application_data IS NULL THEN CAST('{}' AS JSON)
            WHEN JSON_TYPE(a.txt_application_data) = 'STRING' THEN CAST(JSON_UNQUOTE(a.txt_application_data) AS JSON)
            ELSE a.txt_application_data
        END
    FROM cfg_tbl_custom_form_application a
    WHERE a.ser_application_id = @app_id
);

$modeSql

UPDATE cfg_tbl_custom_form_application a
SET
    a.txt_application_data = CAST(@data_after AS JSON),
    a.dte_modified_date = NOW()
WHERE a.ser_application_id = @app_id;

SELECT
    @app_id AS application_id,
    @field_id AS field_id,
    @file_name AS file_name,
    JSON_LENGTH(JSON_EXTRACT(@data_after, @tv_path)) AS attachment_count,
    (
        SELECT MAX(CHAR_LENGTH(COALESCE(x.b64, '')))
        FROM JSON_TABLE(
            JSON_EXTRACT(@data_after, @tv_path),
            '$[*]' COLUMNS (b64 LONGTEXT PATH '$.base64')
        ) x
    ) AS max_base64_chars,
    ROW_COUNT() AS rows_updated;

COMMIT;
"@

Set-Content -Path $tempSql -Value $sql -Encoding UTF8 -NoNewline

if ($DryRun) {
    Write-Host "Dry run: SQL written to $tempSql"
    Write-Host "Run: mysql ... < `"$tempSql`""
    exit 0
}

Write-Host "Applying update (large SQL file, may take a minute)..."
$runArgs = @(
    "-h", $Host,
    "-P", $Port,
    "-u", $User,
    "-p$Password",
    "--default-character-set=utf8mb4",
    "--max_allowed_packet=512M",
    $Database
)

& $mysql.Source @runArgs 2>&1 | ForEach-Object { $_ }
if ($LASTEXITCODE -ne 0) {
    throw "Import failed. SQL saved at: $tempSql"
}

Remove-Item $tempSql -Force -ErrorAction SilentlyContinue
Write-Host "Done. Refresh the application in the browser and open the attachment."
Write-Host "If import failed with packet error, run in Workbench after: SET GLOBAL max_allowed_packet=1073741824;"
