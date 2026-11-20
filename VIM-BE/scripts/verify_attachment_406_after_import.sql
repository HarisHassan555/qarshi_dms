-- Quick check after import for application 406 (attachment field only).
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

SELECT
    fld.field_id,
    fld.field_label,
    COALESCE(JSON_LENGTH(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id)))), 0) AS file_count,
    (
        SELECT GROUP_CONCAT(n.file_name ORDER BY n.idx SEPARATOR ' | ')
        FROM JSON_TABLE(
            COALESCE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))), JSON_ARRAY()),
            '$[*]' COLUMNS (
                idx FOR ORDINALITY,
                file_name VARCHAR(500) PATH '$.fileName'
            )
        ) n
    ) AS file_names,
    (
        SELECT MAX(CHAR_LENGTH(COALESCE(x.b64, '')))
        FROM JSON_TABLE(
            COALESCE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))), JSON_ARRAY()),
            '$[*]' COLUMNS (b64 LONGTEXT PATH '$.base64')
        ) x
    ) AS max_base64_chars,
    CASE
        WHEN (
            SELECT MAX(CHAR_LENGTH(COALESCE(x.b64, '')))
            FROM JSON_TABLE(
                COALESCE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fld.field_id))), JSON_ARRAY()),
                '$[*]' COLUMNS (b64 LONGTEXT PATH '$.base64')
            ) x
        ) > 1000 THEN 'OPENABLE'
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
