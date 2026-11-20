-- Repair application 406 attachments when the UI shows the file but
-- "Attachment content is unavailable" (missing base64 in txt_application_data).
-- Run diagnose_attachment_ps_0016_406.sql first. Database: velocity

USE velocity;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

SET @app_id = 406;
SET @keep_file_name = '4100060234 FOR PS United plastic (1).pdf' COLLATE utf8mb4_unicode_ci;

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

DROP TEMPORARY TABLE IF EXISTS tmp_ps0016_att_scan;
CREATE TEMPORARY TABLE tmp_ps0016_att_scan (
    field_key VARCHAR(255) NOT NULL,
    attachment_index INT NOT NULL,
    file_name VARCHAR(500),
    base64_len BIGINT,
    base64 LONGTEXT,
    PRIMARY KEY (field_key, attachment_index)
);

INSERT INTO tmp_ps0016_att_scan (field_key, attachment_index, file_name, base64_len, base64)
SELECT
    fk.field_key,
    att.row_idx - 1 AS attachment_index,
    COALESCE(att.file_name, att.att_name, att.original_name) AS file_name,
    CHAR_LENGTH(COALESCE(att.base64, '')) AS base64_len,
    att.base64
FROM (
    SELECT JSON_EXTRACT(@data_json, '$.templateValues') AS values_map
    UNION ALL
    SELECT @data_json AS values_map
) s
JOIN JSON_TABLE(
    JSON_KEYS(COALESCE(s.values_map, JSON_OBJECT())),
    '$[*]' COLUMNS (field_key VARCHAR(255) PATH '$')
) fk
JOIN JSON_TABLE(
    JSON_EXTRACT(s.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key))),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        file_name VARCHAR(500) PATH '$.fileName',
        att_name VARCHAR(500) PATH '$.name',
        original_name VARCHAR(500) PATH '$.originalName',
        base64 LONGTEXT PATH '$.base64'
    )
) att
WHERE JSON_TYPE(JSON_EXTRACT(s.values_map, CONCAT('$.', JSON_QUOTE(fk.field_key)))) = 'ARRAY';

SELECT * FROM tmp_ps0016_att_scan ORDER BY field_key, attachment_index;

SET @field_key = (
    SELECT field_key
    FROM tmp_ps0016_att_scan
    WHERE file_name COLLATE utf8mb4_unicode_ci = @keep_file_name COLLATE utf8mb4_unicode_ci
    ORDER BY field_key
    LIMIT 1
);

SET @keep_index = (
    SELECT attachment_index
    FROM tmp_ps0016_att_scan
    WHERE field_key = @field_key
      AND file_name COLLATE utf8mb4_unicode_ci = @keep_file_name COLLATE utf8mb4_unicode_ci
    LIMIT 1
);

SET @keep_base64_len = (
    SELECT base64_len
    FROM tmp_ps0016_att_scan
    WHERE field_key = @field_key
      AND attachment_index = @keep_index
);

SET @donor_base64 = (
    SELECT base64
    FROM tmp_ps0016_att_scan
    WHERE base64_len > 0
    ORDER BY base64_len DESC
    LIMIT 1
);

SELECT
    @field_key AS field_key,
    @keep_index AS keep_index,
    COALESCE(@keep_base64_len, 0) AS keep_base64_chars,
    CHAR_LENGTH(COALESCE(@donor_base64, '')) AS donor_base64_chars,
    CASE
        WHEN @field_key IS NULL THEN 'ERROR - keep file name not found in templateValues'
        WHEN COALESCE(@keep_base64_len, 0) > 0 THEN 'SKIP - keep file already has base64'
        WHEN @donor_base64 IS NULL OR CHAR_LENGTH(@donor_base64) = 0 THEN 'ERROR - no base64 anywhere in field; re-upload PDF in app'
        ELSE 'OK - will copy donor base64 onto keep file'
    END AS repair_status;

SET @root_field_path = CONCAT('$.', JSON_QUOTE(@field_key));
SET @tv_field_path = CONCAT('$.templateValues.', JSON_QUOTE(@field_key));
SET @synced_array = COALESCE(
    JSON_EXTRACT(@data_json, @tv_field_path),
    JSON_EXTRACT(@data_json, @root_field_path),
    JSON_ARRAY()
);

SET @patched_array = JSON_SET(
    @synced_array,
    CONCAT('$[', @keep_index, '].base64'),
    @donor_base64
);

SET @data_after = JSON_SET(
    JSON_SET(@data_json, @root_field_path, @patched_array),
    @tv_field_path,
    @patched_array
);

UPDATE cfg_tbl_custom_form_application a
SET a.txt_application_data = CAST(@data_after AS JSON)
WHERE a.ser_application_id = @app_id
  AND @field_key IS NOT NULL
  AND COALESCE(@keep_base64_len, 0) = 0
  AND @donor_base64 IS NOT NULL
  AND CHAR_LENGTH(@donor_base64) > 0
  AND @keep_index IS NOT NULL;

SELECT ROW_COUNT() AS rows_updated;

DROP TEMPORARY TABLE IF EXISTS tmp_ps0016_att_scan;

-- COMMIT;
ROLLBACK;
