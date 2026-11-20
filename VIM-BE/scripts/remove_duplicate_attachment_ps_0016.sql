-- =============================================================================
-- PS-0016: remove duplicate PDF attachment from txt_application_data.
--
-- Removes: 4100060234 FOR PS United plastic.pdf
-- Keeps:    4100060234 FOR PS United plastic (1).pdf
--
-- Run the whole script in one execution (session variables must stay set).
-- Target DB: velocity (not velocity_workbench).
-- Target row: ser_application_id = 406 (serial PS-0016).
-- =============================================================================

USE velocity;

-- Align session collation with table/column (avoids 1267 on LIKE / =).
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @form_code = 'PS-0016' COLLATE utf8mb4_unicode_ci;
SET @remove_file_name = '4100060234 FOR PS United plastic.pdf' COLLATE utf8mb4_unicode_ci;
SET @keep_file_name = '4100060234 FOR PS United plastic (1).pdf' COLLATE utf8mb4_unicode_ci;

-- Primary key from the UI (Application id). Serial PS-0016 may not match txt_form_code in DB.
SET @app_id_manual = 406;

-- ---------------------------------------------------------------------------
-- Identify application 406 vs form code PS-0016 (run and confirm one row).
-- ---------------------------------------------------------------------------
SELECT
    a.ser_application_id,
    a.txt_form_code,
    TRIM(a.txt_form_code) AS txt_form_code_trimmed,
    a.ser_form_id,
    f.txt_form_name,
    a.txt_status,
    a.bl_is_deleted,
    CHAR_LENGTH(CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4)) AS json_chars,
    (
        CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
        LIKE CONCAT('%', @remove_file_name, '%') COLLATE utf8mb4_unicode_ci
    ) AS has_remove_name,
    (
        CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
        LIKE CONCAT('%', @keep_file_name, '%') COLLATE utf8mb4_unicode_ci
    ) AS has_keep_name
FROM cfg_tbl_custom_form_application a
LEFT JOIN cfg_tbl_custom_form f ON f.ser_form_id = a.ser_form_id
WHERE a.ser_application_id = @app_id_manual;

SELECT
    a.ser_application_id,
    a.txt_form_code,
    a.txt_status,
    a.bl_is_deleted
FROM cfg_tbl_custom_form_application a
WHERE TRIM(a.txt_form_code) COLLATE utf8mb4_unicode_ci = @form_code COLLATE utf8mb4_unicode_ci
   OR a.txt_form_code COLLATE utf8mb4_unicode_ci LIKE 'PS-0016%' COLLATE utf8mb4_unicode_ci
ORDER BY a.ser_application_id DESC;

DROP TEMPORARY TABLE IF EXISTS tmp_ps0016_candidates;
CREATE TEMPORARY TABLE tmp_ps0016_candidates (
    ser_application_id INT PRIMARY KEY,
    txt_form_code VARCHAR(50),
    txt_status VARCHAR(50),
    json_chars BIGINT,
    has_remove_name TINYINT,
    has_keep_name TINYINT,
    has_united TINYINT,
    root_json_type VARCHAR(20)
);

INSERT INTO tmp_ps0016_candidates (
    ser_application_id,
    txt_form_code,
    txt_status,
    json_chars,
    has_remove_name,
    has_keep_name,
    has_united,
    root_json_type
)
SELECT
    a.ser_application_id,
    a.txt_form_code,
    a.txt_status,
    CHAR_LENGTH(CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4)) AS json_chars,
    (
        CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
        LIKE CONCAT('%', @remove_file_name, '%') COLLATE utf8mb4_unicode_ci
    ) AS has_remove_name,
    (
        CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
        LIKE CONCAT('%', @keep_file_name, '%') COLLATE utf8mb4_unicode_ci
    ) AS has_keep_name,
    (
        CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
        LIKE '%United plastic%' COLLATE utf8mb4_unicode_ci
    ) AS has_united,
    JSON_TYPE(a.txt_application_data) AS root_json_type
FROM cfg_tbl_custom_form_application a
WHERE a.ser_application_id = @app_id_manual
   OR (
        TRIM(a.txt_form_code) COLLATE utf8mb4_unicode_ci = @form_code COLLATE utf8mb4_unicode_ci
        AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
   );

SELECT * FROM tmp_ps0016_candidates ORDER BY ser_application_id DESC;

SET @app_id = @app_id_manual;

SELECT
    @app_id AS application_id_chosen,
    (
        SELECT txt_form_code
        FROM cfg_tbl_custom_form_application
        WHERE ser_application_id = @app_id
    ) AS txt_form_code_on_row,
    CASE
        WHEN @app_id IS NULL THEN 'ERROR - Set @app_id_manual'
        WHEN NOT EXISTS (
            SELECT 1 FROM cfg_tbl_custom_form_application WHERE ser_application_id = @app_id
        ) THEN CONCAT('ERROR - No application with ser_application_id=', @app_id)
        ELSE 'OK'
    END AS app_lookup;

-- Normalize JSON (handles double-encoded JSON string roots)
SET @data_before = NULL;
SET @data_before = (
    SELECT
        CASE
            WHEN a.txt_application_data IS NULL THEN NULL
            WHEN JSON_TYPE(a.txt_application_data) = 'STRING' THEN JSON_UNQUOTE(a.txt_application_data)
            ELSE CAST(a.txt_application_data AS CHAR CHARACTER SET utf8mb4)
        END
    FROM cfg_tbl_custom_form_application a
    WHERE a.ser_application_id = @app_id
);

SET @data_json = CAST(COALESCE(@data_before, '{}') AS JSON);

SELECT
    @app_id AS application_id,
    CASE WHEN @data_before IS NULL THEN 'no json' ELSE 'has json' END AS application_data_status,
    CHAR_LENGTH(@data_before) AS json_char_length,
    (
        @data_before COLLATE utf8mb4_unicode_ci
        LIKE CONCAT('%', @remove_file_name, '%') COLLATE utf8mb4_unicode_ci
    ) AS has_remove_name_substring,
    (
        @data_before COLLATE utf8mb4_unicode_ci
        LIKE CONCAT('%', @keep_file_name, '%') COLLATE utf8mb4_unicode_ci
    ) AS has_keep_name_substring,
    JSON_TYPE(@data_json) AS parsed_root_type,
    JSON_TYPE(JSON_EXTRACT(@data_json, '$.templateValues')) AS template_values_type,
    JSON_LENGTH(COALESCE(JSON_EXTRACT(@data_json, '$.templateValues'), JSON_OBJECT())) AS template_values_key_count;

DROP TEMPORARY TABLE IF EXISTS tmp_ps0016_attachment_hits;
CREATE TEMPORARY TABLE tmp_ps0016_attachment_hits (
    hit_id INT AUTO_INCREMENT PRIMARY KEY,
    value_scope VARCHAR(20) NOT NULL,
    field_key VARCHAR(255) NOT NULL,
    array_index INT NOT NULL,
    resolved_file_name VARCHAR(500) NULL,
    json_array_path VARCHAR(512) NOT NULL
);

-- Scan $.templateValues.<field> arrays
INSERT INTO tmp_ps0016_attachment_hits (value_scope, field_key, array_index, resolved_file_name, json_array_path)
SELECT
    'templateValues' AS value_scope,
    fk.field_key,
    att.row_idx AS array_index,
    COALESCE(att.file_name, att.att_name) AS resolved_file_name,
    CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key)) AS json_array_path
FROM (
    SELECT JSON_EXTRACT(@data_json, '$.templateValues') AS tv
) s
JOIN JSON_TABLE(
    JSON_KEYS(COALESCE(s.tv, JSON_OBJECT())),
    '$[*]' COLUMNS (field_key VARCHAR(255) PATH '$')
) fk
JOIN JSON_TABLE(
    JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key))),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        file_name VARCHAR(500) PATH '$.fileName',
        att_name VARCHAR(500) PATH '$.name',
        original_name VARCHAR(500) PATH '$.originalName'
    )
) att
WHERE JSON_TYPE(JSON_EXTRACT(@data_json, CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key)))) = 'ARRAY';

-- Scan root-level <field> arrays (non-metadata keys)
INSERT INTO tmp_ps0016_attachment_hits (value_scope, field_key, array_index, resolved_file_name, json_array_path)
SELECT
    'root' AS value_scope,
    fk.field_key,
    att.row_idx AS array_index,
    COALESCE(att.file_name, att.att_name, att.original_name) AS resolved_file_name,
    CONCAT('$.', JSON_QUOTE(fk.field_key)) AS json_array_path
FROM JSON_TABLE(
    JSON_KEYS(@data_json),
    '$[*]' COLUMNS (field_key VARCHAR(255) PATH '$')
) fk
JOIN JSON_TABLE(
    JSON_EXTRACT(@data_json, CONCAT('$.', JSON_QUOTE(fk.field_key))),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        file_name VARCHAR(500) PATH '$.fileName',
        att_name VARCHAR(500) PATH '$.name',
        original_name VARCHAR(500) PATH '$.originalName'
    )
) att
WHERE fk.field_key NOT IN ('templateValues', 'footerFields', 'templatePayload', 'templateOpinionRequest')
  AND JSON_TYPE(JSON_EXTRACT(@data_json, CONCAT('$.', JSON_QUOTE(fk.field_key)))) = 'ARRAY';

-- Single-object attachment fields (not arrays)
INSERT INTO tmp_ps0016_attachment_hits (value_scope, field_key, array_index, resolved_file_name, json_array_path)
SELECT
    scopes.value_scope,
    scopes.field_key,
    1 AS array_index,
    COALESCE(
        JSON_UNQUOTE(JSON_EXTRACT(@data_json, CONCAT(scopes.json_array_path, '.fileName'))),
        JSON_UNQUOTE(JSON_EXTRACT(@data_json, CONCAT(scopes.json_array_path, '.name'))),
        JSON_UNQUOTE(JSON_EXTRACT(@data_json, CONCAT(scopes.json_array_path, '.originalName')))
    ) AS resolved_file_name,
    scopes.json_array_path
FROM (
    SELECT 'templateValues' AS value_scope, fk.field_key,
           CONCAT('$.templateValues.', JSON_QUOTE(fk.field_key)) AS json_array_path
    FROM (
        SELECT JSON_EXTRACT(@data_json, '$.templateValues') AS tv
    ) s
    JOIN JSON_TABLE(
        JSON_KEYS(COALESCE(s.tv, JSON_OBJECT())),
        '$[*]' COLUMNS (field_key VARCHAR(255) PATH '$')
    ) fk
    UNION ALL
    SELECT 'root' AS value_scope, fk.field_key,
           CONCAT('$.', JSON_QUOTE(fk.field_key)) AS json_array_path
    FROM JSON_TABLE(
        JSON_KEYS(@data_json),
        '$[*]' COLUMNS (field_key VARCHAR(255) PATH '$')
    ) fk
    WHERE fk.field_key NOT IN ('templateValues', 'footerFields', 'templatePayload', 'templateOpinionRequest')
) scopes
WHERE JSON_TYPE(JSON_EXTRACT(@data_json, scopes.json_array_path)) = 'OBJECT';

SELECT * FROM tmp_ps0016_attachment_hits
WHERE resolved_file_name IS NOT NULL
  AND (
        resolved_file_name COLLATE utf8mb4_unicode_ci LIKE '%United plastic%' COLLATE utf8mb4_unicode_ci
        OR resolved_file_name COLLATE utf8mb4_unicode_ci = @remove_file_name COLLATE utf8mb4_unicode_ci
        OR resolved_file_name COLLATE utf8mb4_unicode_ci = @keep_file_name COLLATE utf8mb4_unicode_ci
      )
ORDER BY value_scope, field_key, array_index;

SET @hit_scope = NULL;
SET @hit_field_key = NULL;
SET @array_path = NULL;

SELECT
    h.value_scope,
    h.field_key,
    h.json_array_path
INTO @hit_scope, @hit_field_key, @array_path
FROM tmp_ps0016_attachment_hits h
WHERE h.resolved_file_name COLLATE utf8mb4_unicode_ci = @remove_file_name COLLATE utf8mb4_unicode_ci
ORDER BY
    CASE h.value_scope WHEN 'templateValues' THEN 0 ELSE 1 END,
    h.array_index
LIMIT 1;

SELECT
    @hit_scope AS hit_scope,
    @hit_field_key AS hit_field_key,
    @array_path AS attachment_array_path,
    CASE
        WHEN @app_id IS NULL THEN 'SKIP - No application'
        WHEN @data_before IS NULL THEN 'SKIP - Empty application data'
        WHEN @array_path IS NULL THEN 'ERROR - Remove-file not found in parsed attachment fields (see hits above / substring flags)'
        ELSE 'OK - Ready to update'
    END AS update_readiness;

-- Remove by index (preserves full attachment JSON including large base64; avoids JSON_ARRAYAGG).
SET @remove_array_index = NULL;
SET @source_value_type = JSON_TYPE(JSON_EXTRACT(@data_json, @array_path));

SELECT (jt.row_idx - 1)
INTO @remove_array_index
FROM JSON_TABLE(
    COALESCE(JSON_EXTRACT(@data_json, @array_path), JSON_ARRAY()),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        file_name VARCHAR(500) PATH '$.fileName',
        att_name VARCHAR(500) PATH '$.name',
        original_name VARCHAR(500) PATH '$.originalName'
    )
) jt
WHERE @source_value_type = 'ARRAY'
  AND COALESCE(jt.file_name, jt.att_name, jt.original_name) COLLATE utf8mb4_unicode_ci
      = @remove_file_name COLLATE utf8mb4_unicode_ci
ORDER BY jt.row_idx
LIMIT 1;

SET @root_field_path = CONCAT('$.', JSON_QUOTE(@hit_field_key));
SET @tv_field_path = CONCAT('$.templateValues.', JSON_QUOTE(@hit_field_key));

SET @data_after = @data_json;

SET @data_after = CASE
    WHEN @source_value_type = 'OBJECT'
         AND COALESCE(
             JSON_UNQUOTE(JSON_EXTRACT(@data_json, CONCAT(@array_path, '.fileName'))),
             JSON_UNQUOTE(JSON_EXTRACT(@data_json, CONCAT(@array_path, '.name'))),
             JSON_UNQUOTE(JSON_EXTRACT(@data_json, CONCAT(@array_path, '.originalName')))
         ) COLLATE utf8mb4_unicode_ci = @remove_file_name COLLATE utf8mb4_unicode_ci
        THEN JSON_SET(@data_json, @array_path, JSON_ARRAY())
    WHEN @remove_array_index IS NOT NULL
        THEN JSON_REMOVE(@data_json, CONCAT(@array_path, '[', @remove_array_index, ']'))
    ELSE @data_json
END;

-- Keep root and templateValues in sync (UI reads templateValues first).
SET @synced_array = COALESCE(
    JSON_EXTRACT(@data_after, @tv_field_path),
    JSON_EXTRACT(@data_after, @root_field_path),
    JSON_ARRAY()
);

SET @data_after = JSON_SET(
    JSON_SET(@data_after, @root_field_path, @synced_array),
    @tv_field_path,
    @synced_array
);

-- If kept file has no bytes, copy base64 from the sibling that had the most content (same field).
SET @keep_base64_len = (
    SELECT MAX(CHAR_LENGTH(COALESCE(
        JSON_UNQUOTE(JSON_EXTRACT(elem.item_json, '$.base64')),
        ''
    )))
    FROM JSON_TABLE(
        COALESCE(@synced_array, JSON_ARRAY()),
        '$[*]' COLUMNS (
            item_json JSON PATH '$'
        )
    ) elem
    WHERE COALESCE(
        JSON_UNQUOTE(JSON_EXTRACT(elem.item_json, '$.fileName')),
        JSON_UNQUOTE(JSON_EXTRACT(elem.item_json, '$.name')),
        JSON_UNQUOTE(JSON_EXTRACT(elem.item_json, '$.originalName'))
    ) COLLATE utf8mb4_unicode_ci = @keep_file_name COLLATE utf8mb4_unicode_ci
);

SET @donor_base64 = (
    SELECT donor.b64
    FROM (
        SELECT
            CHAR_LENGTH(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(elem.item_json, '$.base64')), '')) AS b64_len,
            JSON_UNQUOTE(JSON_EXTRACT(elem.item_json, '$.base64')) AS b64
        FROM JSON_TABLE(
            COALESCE(JSON_EXTRACT(@data_json, @array_path), JSON_ARRAY()),
            '$[*]' COLUMNS (item_json JSON PATH '$')
        ) elem
    ) donor
    WHERE donor.b64_len > 0
    ORDER BY donor.b64_len DESC
    LIMIT 1
);

SET @keep_array_index = (
    SELECT (jt.row_idx - 1)
    FROM JSON_TABLE(
        COALESCE(@synced_array, JSON_ARRAY()),
        '$[*]' COLUMNS (
            row_idx FOR ORDINALITY,
            file_name VARCHAR(500) PATH '$.fileName',
            att_name VARCHAR(500) PATH '$.name',
            original_name VARCHAR(500) PATH '$.originalName'
        )
    ) jt
    WHERE COALESCE(jt.file_name, jt.att_name, jt.original_name) COLLATE utf8mb4_unicode_ci
          = @keep_file_name COLLATE utf8mb4_unicode_ci
    ORDER BY jt.row_idx
    LIMIT 1
);

SET @data_after = CASE
    WHEN COALESCE(@keep_base64_len, 0) = 0
         AND @donor_base64 IS NOT NULL
         AND CHAR_LENGTH(@donor_base64) > 0
         AND @keep_array_index IS NOT NULL
        THEN JSON_SET(
            JSON_SET(@data_after, @root_field_path, JSON_SET(
                @synced_array,
                CONCAT('$[', @keep_array_index, '].base64'),
                @donor_base64
            )),
            @tv_field_path,
            JSON_SET(
                @synced_array,
                CONCAT('$[', @keep_array_index, '].base64'),
                @donor_base64
            )
        )
    ELSE @data_after
END;

SELECT
    @source_value_type AS source_value_type,
    @remove_array_index AS removed_array_index,
    JSON_LENGTH(COALESCE(@synced_array, JSON_ARRAY())) AS attachments_after_count,
    @root_field_path AS root_field_path,
    @tv_field_path AS template_values_field_path,
    COALESCE(@keep_base64_len, 0) AS keep_file_base64_chars_before_patch,
    CASE
        WHEN COALESCE(@keep_base64_len, 0) > 0 THEN 'OK - kept file already has base64'
        WHEN @donor_base64 IS NOT NULL AND CHAR_LENGTH(@donor_base64) > 0 THEN 'PATCHED - copied base64 from removed duplicate'
        ELSE 'WARNING - no base64 found; re-upload attachment in app or restore DB backup'
    END AS base64_status;

UPDATE cfg_tbl_custom_form_application a
SET a.txt_application_data = CAST(@data_after AS JSON)
WHERE a.ser_application_id = @app_id
  AND @array_path IS NOT NULL
  AND @data_after IS NOT NULL
  AND (
        @remove_array_index IS NOT NULL
        OR @source_value_type = 'OBJECT'
      );

SELECT ROW_COUNT() AS rows_updated;

SELECT
    @app_id AS application_id,
    JSON_SEARCH(a.txt_application_data, 'all', @remove_file_name) AS remove_name_paths,
    JSON_SEARCH(a.txt_application_data, 'all', @keep_file_name) AS keep_name_paths
FROM cfg_tbl_custom_form_application a
WHERE a.ser_application_id = @app_id;

DROP TEMPORARY TABLE IF EXISTS tmp_ps0016_attachment_hits;
DROP TEMPORARY TABLE IF EXISTS tmp_ps0016_candidates;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;

COMMIT;

-- Dry run only: comment COMMIT above and use ROLLBACK;
-- ROLLBACK;
