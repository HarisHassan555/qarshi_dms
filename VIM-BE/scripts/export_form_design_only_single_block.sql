-- Run this on the SOURCE database.
-- It returns ONE text block containing SQL you can copy and run on the TARGET database.
-- This version uses a temporary table to avoid MySQL collation issues during UNION.

SET @form_id := 602;
SET SESSION group_concat_max_len = 1024 * 1024 * 50;

DROP TEMPORARY TABLE IF EXISTS tmp_form_export_lines;

CREATE TEMPORARY TABLE tmp_form_export_lines (
    sort_order INT NOT NULL,
    script_line LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
);

INSERT INTO tmp_form_export_lines (sort_order, script_line)
VALUES
    (10, 'START TRANSACTION;'),
    (20, CONCAT('-- Design-only import for source formId ', @form_id));

INSERT INTO tmp_form_export_lines (sort_order, script_line)
SELECT
    100,
    CONCAT(
        'INSERT INTO cfg_tbl_custom_form (',
        'txt_form_name, txt_form_code, txt_convention_prefix, txt_form_description, ',
        'txt_user_ids, bl_is_active, bl_is_deleted, bln_status, dte_created_date',
        ') VALUES (',
        IFNULL(QUOTE(CONVERT(f.txt_form_name USING utf8mb4)), 'NULL'), ', ',
        'NULL, ',
        IFNULL(QUOTE(CONVERT(f.txt_convention_prefix USING utf8mb4)), 'NULL'), ', ',
        IFNULL(QUOTE(CONVERT(f.txt_form_description USING utf8mb4)), 'NULL'), ', ',
        'NULL, 1, 0, 1, NOW());'
    )
FROM cfg_tbl_custom_form f
WHERE f.ser_form_id = @form_id;

INSERT INTO tmp_form_export_lines (sort_order, script_line)
VALUES (110, 'SET @new_form_id := LAST_INSERT_ID();');

INSERT INTO tmp_form_export_lines (sort_order, script_line)
SELECT
    200 + ROW_NUMBER() OVER (ORDER BY fld.int_field_order, fld.ser_field_id),
    CONCAT(
        'INSERT INTO cfg_tbl_custom_form_field (',
        'ser_form_id, txt_field_label, txt_field_type, txt_placeholder, ',
        'bl_is_required, int_field_order, txt_field_options, ',
        'bl_is_active, bl_is_deleted, dte_created_date',
        ') VALUES (',
        '@new_form_id, ',
        IFNULL(QUOTE(CONVERT(fld.txt_field_label USING utf8mb4)), 'NULL'), ', ',
        IFNULL(QUOTE(CONVERT(fld.txt_field_type USING utf8mb4)), 'NULL'), ', ',
        IFNULL(QUOTE(CONVERT(fld.txt_placeholder USING utf8mb4)), 'NULL'), ', ',
        IFNULL(CAST(fld.bl_is_required AS UNSIGNED), '0'), ', ',
        IFNULL(fld.int_field_order, '0'), ', ',
        IFNULL(QUOTE(CONVERT(CAST(fld.txt_field_options AS CHAR CHARACTER SET utf8mb4) USING utf8mb4)), 'NULL'), ', ',
        '1, 0, NOW());'
    )
FROM cfg_tbl_custom_form_field fld
WHERE fld.ser_form_id = @form_id;

INSERT INTO tmp_form_export_lines (sort_order, script_line)
SELECT
    600,
    CONCAT(
        'INSERT INTO tpl_template_definition (',
        'ser_form_id, txt_template_name, txt_code_convention, txt_template_payload, ',
        'bl_is_active, bl_is_deleted, bln_status, dte_created_date',
        ') VALUES (',
        '@new_form_id, ',
        IFNULL(QUOTE(CONVERT(t.txt_template_name USING utf8mb4)), 'NULL'), ', ',
        IFNULL(QUOTE(CONVERT(t.txt_code_convention USING utf8mb4)), 'NULL'), ', ',
        IFNULL(QUOTE(CONVERT(CAST(t.txt_template_payload AS CHAR CHARACTER SET utf8mb4) USING utf8mb4)), 'NULL'), ', ',
        '1, 0, 1, NOW());'
    )
FROM tpl_template_definition t
WHERE t.ser_form_id = @form_id;

INSERT INTO tmp_form_export_lines (sort_order, script_line)
VALUES
    (900, 'COMMIT;'),
    (910, 'SELECT @new_form_id AS new_form_id;');

SELECT GROUP_CONCAT(script_line ORDER BY sort_order SEPARATOR '\n') AS import_sql
FROM tmp_form_export_lines;

DROP TEMPORARY TABLE IF EXISTS tmp_form_export_lines;
