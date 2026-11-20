-- Application 406 ONLY: attachment field in txt_application_data (not blb_pdf_data).
-- Run the entire script. Use result grids 1–3 (ignore blb_pdf / Purchase-summary PDF).
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

SET @ser_form_id = (
    SELECT ser_form_id FROM cfg_tbl_custom_form_application WHERE ser_application_id = @app_id
);

-- ========== Result 1: fields from templatePayload in JSON ==========
SELECT 'from_templatePayload' AS field_source,
       fld.field_id,
       fld.field_label,
       JSON_TYPE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))) AS tv_type,
       COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))), 0) AS file_count,
       (
           SELECT GROUP_CONCAT(n.file_name ORDER BY n.idx SEPARATOR ' | ')
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
       ) AS content_chars,
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
WHERE LOWER(TRIM(fld.field_type)) = 'attachment';

-- ========== Result 2: fields from cfg_tbl_custom_form_field ==========
SELECT 'from_cfg_tbl_custom_form_field' AS field_source,
       fld.field_id,
       fld.field_label,
       JSON_TYPE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))) AS tv_type,
       COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))), 0) AS file_count,
       (
           SELECT GROUP_CONCAT(n.file_name ORDER BY n.idx SEPARATOR ' | ')
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
       ) AS content_chars,
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
FROM (
    SELECT CAST(f.ser_field_id AS CHAR) AS field_id, f.txt_field_label AS field_label
    FROM cfg_tbl_custom_form_field f
    WHERE f.ser_form_id = @ser_form_id
      AND LOWER(TRIM(f.txt_field_type)) = 'attachment'
      AND (f.bl_is_deleted = 0 OR f.bl_is_deleted IS NULL)
) fld;

-- ========== Result 3: any attachment-like array in templateValues ==========
SELECT 'fallback_scan' AS field_source,
       fk.field_key AS field_id,
       '(scan)' AS field_label,
       JSON_TYPE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key)))) AS tv_type,
       COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key)))), 0) AS file_count,
       (
           SELECT GROUP_CONCAT(n.file_name ORDER BY n.idx SEPARATOR ' | ')
           FROM JSON_TABLE(
               JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key))),
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
               JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key))),
               '$[*]' COLUMNS (
                   base64 LONGTEXT PATH '$.base64',
                   data_url LONGTEXT PATH '$.dataUrl'
               )
           ) x
       ) AS content_chars,
       CASE
           WHEN (
               SELECT MAX(CHAR_LENGTH(COALESCE(x.base64, x.data_url, '')))
               FROM JSON_TABLE(
                   JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key))),
                   '$[*]' COLUMNS (base64 LONGTEXT PATH '$.base64', data_url LONGTEXT PATH '$.dataUrl')
               ) x
           ) > 0 THEN 'OPENABLE'
           WHEN COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key)))), 0) > 0
               THEN 'MISSING_CONTENT'
           ELSE 'EMPTY'
       END AS status
FROM JSON_TABLE(
    JSON_KEYS(COALESCE(JSON_EXTRACT(@data_json, '$.templateValues'), JSON_OBJECT())),
    '$[*]' COLUMNS (field_key VARCHAR(255) PATH '$')
) fk
WHERE JSON_TYPE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key)))) = 'ARRAY'
  AND JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key)))) > 0
  AND (
        JSON_EXTRACT(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key))), '$[0].fileName') IS NOT NULL
        OR JSON_EXTRACT(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key))), '$[0].name') IS NOT NULL
      );
