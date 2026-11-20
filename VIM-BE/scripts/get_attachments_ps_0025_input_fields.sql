-- =============================================================================
-- Get attachments for application PS-0025 from INPUT FIELDS only.
-- Source: txt_application_data.templateValues.<attachmentField>[]
-- Ignores: blb_pdf_data (generated form PDF)
-- Run entire script in MySQL Workbench against: velocity
-- =============================================================================

USE velocity;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @form_code = 'PS-0025' COLLATE utf8mb4_unicode_ci;

-- 1) Resolve application row(s) for PS-0025
SELECT
    a.ser_application_id,
    a.txt_form_code,
    a.ser_form_id,
    a.txt_status,
    a.bl_is_deleted,
    CHAR_LENGTH(CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4)) AS json_chars
FROM cfg_tbl_custom_form_application a
WHERE TRIM(a.txt_form_code) COLLATE utf8mb4_unicode_ci = @form_code
   OR a.txt_form_code COLLATE utf8mb4_unicode_ci LIKE 'PS-0025%' COLLATE utf8mb4_unicode_ci
ORDER BY a.ser_application_id DESC;

-- Prefer exact match; fall back to LIKE if needed
SET @app_id = (
    SELECT a.ser_application_id
    FROM cfg_tbl_custom_form_application a
    WHERE TRIM(a.txt_form_code) COLLATE utf8mb4_unicode_ci = @form_code
      AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
    ORDER BY a.ser_application_id DESC
    LIMIT 1
);

SET @app_id = COALESCE(
    @app_id,
    (
        SELECT a.ser_application_id
        FROM cfg_tbl_custom_form_application a
        WHERE a.txt_form_code COLLATE utf8mb4_unicode_ci LIKE 'PS-0025%' COLLATE utf8mb4_unicode_ci
          AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
        ORDER BY a.ser_application_id DESC
        LIMIT 1
    )
);

SELECT
    @app_id AS application_id,
    (
        SELECT a.txt_form_code
        FROM cfg_tbl_custom_form_application a
        WHERE a.ser_application_id = @app_id
    ) AS txt_form_code,
    CASE
        WHEN @app_id IS NULL THEN 'ERROR - no application found for PS-0025'
        ELSE 'OK'
    END AS resolve_status;

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

-- 2) Attachment INPUT fields (from templatePayload) + file summary
SELECT
    @app_id AS application_id,
    fld.field_id,
    fld.field_label,
    fld.field_type,
    COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))), 0) AS file_count,
    (
        SELECT GROUP_CONCAT(COALESCE(n.file_name, n.att_name) ORDER BY n.idx SEPARATOR ' | ')
        FROM JSON_TABLE(
            COALESCE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))), JSON_ARRAY()),
            '$[*]' COLUMNS (
                idx FOR ORDINALITY,
                file_name VARCHAR(500) PATH '$.fileName',
                att_name VARCHAR(500) PATH '$.name'
            )
        ) n
    ) AS file_names,
    (
        SELECT MAX(CHAR_LENGTH(COALESCE(x.base64, x.data_url, '')))
        FROM JSON_TABLE(
            COALESCE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))), JSON_ARRAY()),
            '$[*]' COLUMNS (
                base64 LONGTEXT PATH '$.base64',
                data_url LONGTEXT PATH '$.dataUrl'
            )
        ) x
    ) AS max_content_chars,
    CASE
        WHEN (
            SELECT MAX(CHAR_LENGTH(COALESCE(x.base64, x.data_url, '')))
            FROM JSON_TABLE(
                COALESCE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))), JSON_ARRAY()),
                '$[*]' COLUMNS (base64 LONGTEXT PATH '$.base64', data_url LONGTEXT PATH '$.dataUrl')
            ) x
        ) > 0 THEN 'OPENABLE'
        WHEN COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))), 0) > 0
            THEN 'MISSING_CONTENT'
        ELSE 'EMPTY'
    END AS status
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

-- 3) Each file on attachment INPUT fields (metadata; no huge base64 dump)
SELECT
    @app_id AS application_id,
    fld.field_id,
    fld.field_label,
    att.row_idx - 1 AS attachment_index,
    COALESCE(att.file_name, att.att_name, att.original_name) AS file_name,
    att.mime_type,
    CHAR_LENGTH(COALESCE(att.base64, '')) AS base64_chars,
    CHAR_LENGTH(COALESCE(att.data_url, '')) AS data_url_chars,
    CASE
        WHEN CHAR_LENGTH(COALESCE(att.base64, att.data_url, '')) > 0 THEN 'OPENABLE'
        ELSE 'MISSING_CONTENT'
    END AS open_status
FROM JSON_TABLE(
    COALESCE(JSON_EXTRACT(@data_json, '$.templatePayload.fields'), JSON_ARRAY()),
    '$[*]' COLUMNS (
        field_id VARCHAR(255) PATH '$.id',
        field_label VARCHAR(255) PATH '$.label',
        field_type VARCHAR(64) PATH '$.type'
    )
) fld
JOIN JSON_TABLE(
    COALESCE(
        JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))),
        JSON_ARRAY()
    ),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        file_name VARCHAR(500) PATH '$.fileName',
        att_name VARCHAR(500) PATH '$.name',
        original_name VARCHAR(500) PATH '$.originalName',
        mime_type VARCHAR(255) PATH '$.mimeType',
        base64 LONGTEXT PATH '$.base64',
        data_url LONGTEXT PATH '$.dataUrl'
    )
) att
WHERE LOWER(TRIM(fld.field_type)) = 'attachment'
ORDER BY fld.field_label, attachment_index;

-- 4) Full attachment payloads from INPUT fields only (includes base64 / dataUrl)
--    Warning: result can be very large in Workbench.
SELECT
    @app_id AS application_id,
    fld.field_id,
    fld.field_label,
    att.row_idx - 1 AS attachment_index,
    COALESCE(att.file_name, att.att_name, att.original_name) AS file_name,
    att.mime_type,
    att.base64,
    att.data_url
FROM JSON_TABLE(
    COALESCE(JSON_EXTRACT(@data_json, '$.templatePayload.fields'), JSON_ARRAY()),
    '$[*]' COLUMNS (
        field_id VARCHAR(255) PATH '$.id',
        field_label VARCHAR(255) PATH '$.label',
        field_type VARCHAR(64) PATH '$.type'
    )
) fld
JOIN JSON_TABLE(
    COALESCE(
        JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))),
        JSON_ARRAY()
    ),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        file_name VARCHAR(500) PATH '$.fileName',
        att_name VARCHAR(500) PATH '$.name',
        original_name VARCHAR(500) PATH '$.originalName',
        mime_type VARCHAR(255) PATH '$.mimeType',
        base64 LONGTEXT PATH '$.base64',
        data_url LONGTEXT PATH '$.dataUrl'
    )
) att
WHERE LOWER(TRIM(fld.field_type)) = 'attachment'
ORDER BY fld.field_label, attachment_index;
