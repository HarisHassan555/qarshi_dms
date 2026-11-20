-- =============================================================================
-- Delete ONE input-field attachment from PS-0025 (application 418).
-- File: 09.02.2026 Naat Approval - Signed Naat Books.pdf
-- Field: field-1789019525113-fc29d9bcc9e69
-- Does NOT touch: other fields, blb_pdf_data, status, form metadata.
-- Run whole script on: velocity
-- Review Result grids 1–3, then uncomment COMMIT (or keep ROLLBACK).
-- =============================================================================

USE velocity;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @app_id = 418;
SET @field_id = 'field-1789019525113-fc29d9bcc9e69' COLLATE utf8mb4_unicode_ci;
SET @remove_file_name = '09.02.2026 Naat Approval - Signed Naat Books.pdf' COLLATE utf8mb4_unicode_ci;

SET @tv_path = CONCAT('$.templateValues.', JSON_QUOTE(@field_id));
SET @root_path = CONCAT('$.', JSON_QUOTE(@field_id));

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

-- 1) BEFORE: show only this field's files
SELECT
    @app_id AS application_id,
    @field_id AS field_id,
    att.row_idx - 1 AS attachment_index,
    COALESCE(att.file_name, att.att_name, att.original_name) AS file_name,
    att.mime_type,
    CHAR_LENGTH(COALESCE(att.base64, '')) AS base64_chars,
    'BEFORE' AS stage
FROM JSON_TABLE(
    COALESCE(
        JSON_EXTRACT(@data_json, @tv_path),
        JSON_EXTRACT(@data_json, @root_path),
        JSON_ARRAY()
    ),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        file_name VARCHAR(500) PATH '$.fileName',
        att_name VARCHAR(500) PATH '$.name',
        original_name VARCHAR(500) PATH '$.originalName',
        mime_type VARCHAR(255) PATH '$.mimeType',
        base64 LONGTEXT PATH '$.base64'
    )
) att
ORDER BY attachment_index;

SET @source_array = COALESCE(
    JSON_EXTRACT(@data_json, @tv_path),
    JSON_EXTRACT(@data_json, @root_path),
    JSON_ARRAY()
);

SET @remove_index = (
    SELECT att.row_idx - 1
    FROM JSON_TABLE(
        COALESCE(@source_array, JSON_ARRAY()),
        '$[*]' COLUMNS (
            row_idx FOR ORDINALITY,
            file_name VARCHAR(500) PATH '$.fileName',
            att_name VARCHAR(500) PATH '$.name',
            original_name VARCHAR(500) PATH '$.originalName'
        )
    ) att
    WHERE COALESCE(att.file_name, att.att_name, att.original_name) COLLATE utf8mb4_unicode_ci
          = @remove_file_name COLLATE utf8mb4_unicode_ci
    ORDER BY att.row_idx
    LIMIT 1
);

SELECT
    @app_id AS application_id,
    @field_id AS field_id,
    @remove_file_name AS remove_file_name,
    @remove_index AS remove_index,
    JSON_LENGTH(COALESCE(@source_array, JSON_ARRAY())) AS files_before,
    CASE
        WHEN @data_json IS NULL THEN 'ERROR - no application data'
        WHEN @remove_index IS NULL THEN 'ERROR - file not found in this field'
        ELSE 'OK - will remove only this index from this field'
    END AS readiness;

-- Remove only that array element, then keep root + templateValues mirrors in sync.
SET @array_after = CASE
    WHEN @remove_index IS NOT NULL
        THEN JSON_REMOVE(@source_array, CONCAT('$[', @remove_index, ']'))
    ELSE @source_array
END;

SET @data_after = JSON_SET(
    JSON_SET(@data_json, @root_path, COALESCE(@array_after, JSON_ARRAY())),
    @tv_path,
    COALESCE(@array_after, JSON_ARRAY())
);

UPDATE cfg_tbl_custom_form_application a
SET
    a.txt_application_data = CAST(@data_after AS JSON),
    a.dte_modified_date = NOW(),
    a.ser_modified_user = 1
WHERE a.ser_application_id = @app_id
  AND @remove_index IS NOT NULL
  AND @data_after IS NOT NULL;

SELECT ROW_COUNT() AS rows_updated;

-- 2) AFTER: confirm this field is empty / no longer has that file
SET @data_verify = (
    SELECT
        CASE
            WHEN a.txt_application_data IS NULL THEN CAST('{}' AS JSON)
            WHEN JSON_TYPE(a.txt_application_data) = 'STRING' THEN CAST(JSON_UNQUOTE(a.txt_application_data) AS JSON)
            ELSE a.txt_application_data
        END
    FROM cfg_tbl_custom_form_application a
    WHERE a.ser_application_id = @app_id
);

SELECT
    @app_id AS application_id,
    @field_id AS field_id,
    COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_verify, @tv_path)), 0) AS tv_file_count,
    COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_verify, @root_path)), 0) AS root_file_count,
    JSON_SEARCH(@data_verify, 'one', @remove_file_name) AS still_has_file_name_path,
    CASE
        WHEN JSON_SEARCH(@data_verify, 'one', @remove_file_name) IS NULL THEN 'OK - file removed'
        ELSE 'ERROR - file name still present somewhere in JSON'
    END AS delete_result;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;

-- Default safe: roll back until you confirm Result grids look right.
ROLLBACK;
-- COMMIT;
