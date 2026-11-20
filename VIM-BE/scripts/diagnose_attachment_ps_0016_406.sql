-- Diagnose why attachments won't open for application 406 (PS-0016).
-- UI opens files from base64 / dataUrl in txt_application_data (templateValues first).
-- Run on database: velocity

USE velocity;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @app_id = 406;

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

SELECT @app_id AS application_id, JSON_TYPE(@data_json) AS root_type;

-- ========== ATTACHMENT FIELDS (what the UI uses) ==========
-- Values live in templateValues.<fieldId>[] with fileName + base64 (not blb_pdf_data).
SELECT
    fld.field_id,
    fld.field_label,
    JSON_TYPE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))) AS template_values_type,
    JSON_TYPE(JSON_EXTRACT(@data_json, CONCAT('$.', JSON_QUOTE(fld.field_id)))) AS root_type,
    COALESCE(
        JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))),
        0
    ) AS template_values_array_length,
    (
        SELECT GROUP_CONCAT(names.file_name ORDER BY names.idx SEPARATOR ' | ')
        FROM JSON_TABLE(
            COALESCE(
                JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))),
                JSON_ARRAY()
            ),
            '$[*]' COLUMNS (
                idx FOR ORDINALITY,
                file_name VARCHAR(500) PATH '$.fileName',
                att_name VARCHAR(500) PATH '$.name'
            )
        ) names
    ) AS file_names_in_template_values,
    (
        SELECT MAX(CHAR_LENGTH(COALESCE(b64.base64, '')))
        FROM JSON_TABLE(
            COALESCE(
                JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))),
                JSON_ARRAY()
            ),
            '$[*]' COLUMNS (
                base64 LONGTEXT PATH '$.base64',
                data_url LONGTEXT PATH '$.dataUrl'
            )
        ) b64
    ) AS max_base64_or_dataurl_chars_in_field,
    CASE
        WHEN (
            SELECT MAX(CHAR_LENGTH(COALESCE(b64.base64, b64.data_url, '')))
            FROM JSON_TABLE(
                COALESCE(
                    JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))),
                    JSON_ARRAY()
                ),
                '$[*]' COLUMNS (
                    base64 LONGTEXT PATH '$.base64',
                    data_url LONGTEXT PATH '$.dataUrl'
                )
            ) b64
        ) > 0 THEN 'OPENABLE'
        WHEN COALESCE(
            JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))),
            0
        ) > 0 THEN 'MISSING_CONTENT'
        ELSE 'EMPTY_FIELD'
    END AS attachment_field_status
FROM JSON_TABLE(
    COALESCE(JSON_EXTRACT(@data_json, '$.templatePayload.fields'), JSON_ARRAY()),
    '$[*]' COLUMNS (
        field_id VARCHAR(255) PATH '$.id',
        field_label VARCHAR(255) PATH '$.label',
        field_type VARCHAR(64) PATH '$.type'
    )
) fld
WHERE LOWER(TRIM(fld.field_type)) = 'attachment'
ORDER BY fld.field_label;

-- Attachment-like arrays under templateValues and root (metadata only; no base64 printed).
SELECT
    scopes.value_scope,
    fk.field_key,
    att.row_idx AS attachment_index,
    COALESCE(att.file_name, att.att_name, att.original_name) AS file_name,
    CHAR_LENGTH(COALESCE(att.base64, '')) AS base64_chars,
    CHAR_LENGTH(COALESCE(att.data_url, '')) AS data_url_chars,
    CASE
        WHEN CHAR_LENGTH(COALESCE(att.base64, att.data_url, '')) > 0 THEN 'OPENABLE'
        ELSE 'MISSING_CONTENT'
    END AS open_status
FROM (
    SELECT 'templateValues' AS value_scope, JSON_EXTRACT(@data_json, '$.templateValues') AS values_map
    UNION ALL
    SELECT 'root', @data_json
) scopes
JOIN JSON_TABLE(
    JSON_KEYS(COALESCE(scopes.values_map, JSON_OBJECT())),
    '$[*]' COLUMNS (field_key VARCHAR(255) PATH '$')
) fk
JOIN JSON_TABLE(
    JSON_EXTRACT(
        scopes.values_map,
        CONCAT('$.', JSON_QUOTE(fk.field_key))
    ),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        file_name VARCHAR(500) PATH '$.fileName',
        att_name VARCHAR(500) PATH '$.name',
        original_name VARCHAR(500) PATH '$.originalName',
        base64 LONGTEXT PATH '$.base64',
        data_url LONGTEXT PATH '$.dataUrl'
    )
) att
WHERE fk.field_key NOT IN ('templateValues', 'footerFields', 'templatePayload', 'templateOpinionRequest')
  AND JSON_TYPE(
      JSON_EXTRACT(scopes.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key)))
  ) = 'ARRAY'
ORDER BY value_scope, field_key, attachment_index;

-- Single-object attachment fields (not arrays).
SELECT
    scopes.value_scope,
    fk.field_key,
    0 AS attachment_index,
    COALESCE(
        JSON_UNQUOTE(JSON_EXTRACT(scopes.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key), '.fileName'))),
        JSON_UNQUOTE(JSON_EXTRACT(scopes.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key), '.name')))
    ) AS file_name,
    CHAR_LENGTH(COALESCE(
        JSON_UNQUOTE(JSON_EXTRACT(scopes.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key), '.base64'))),
        ''
    )) AS base64_chars,
    CASE
        WHEN CHAR_LENGTH(COALESCE(
            JSON_UNQUOTE(JSON_EXTRACT(scopes.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key), '.base64'))),
            JSON_UNQUOTE(JSON_EXTRACT(scopes.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key), '.dataUrl'))),
            ''
        )) > 0 THEN 'OPENABLE'
        ELSE 'MISSING_CONTENT'
    END AS open_status
FROM (
    SELECT 'templateValues' AS value_scope, JSON_EXTRACT(@data_json, '$.templateValues') AS values_map
    UNION ALL
    SELECT 'root', @data_json
) scopes
JOIN JSON_TABLE(
    JSON_KEYS(COALESCE(scopes.values_map, JSON_OBJECT())),
    '$[*]' COLUMNS (field_key VARCHAR(255) PATH '$')
) fk
WHERE fk.field_key NOT IN ('templateValues', 'footerFields', 'templatePayload', 'templateOpinionRequest')
  AND JSON_TYPE(JSON_EXTRACT(scopes.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key)))) = 'OBJECT'
ORDER BY value_scope, field_key;

-- Quick JSON substring checks (vendor PDF vs generated form PDF).
SELECT
    @app_id AS application_id,
    (
        CAST(@data_json AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
        LIKE '%United plastic%' COLLATE utf8mb4_unicode_ci
    ) AS json_has_united_plastic_name,
    (
        CAST(@data_json AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
        LIKE '%"base64"%' COLLATE utf8mb4_unicode_ci
    ) AS json_has_base64_key,
    CHAR_LENGTH(CAST(@data_json AS CHAR CHARACTER SET utf8mb4)) AS json_char_length;

-- Ignore below for attachment-field issues (generated form output only).
SELECT
    ser_application_id,
    txt_form_code,
    txt_pdf_name,
    CASE WHEN blb_pdf_data IS NULL THEN 0 ELSE OCTET_LENGTH(blb_pdf_data) END AS pdf_bytes,
    'IGNORE_FOR_ATTACHMENTS' AS note
FROM cfg_tbl_custom_form_application
WHERE ser_application_id = @app_id;
