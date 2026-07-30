-- Run this on the SOURCE database.
-- It outputs a copy/paste SQL script for the TARGET database that recreates:
-- 1) the form definition
-- 2) the form fields
-- 3) the template/styling payload
--
-- It intentionally does NOT copy:
-- - ser_form_id
-- - field ids
-- - approval pipeline
-- - created/modified dates
-- - created/modified users
-- - deleted/status metadata
--
-- The target database will generate its own new IDs automatically.

SET @form_id := 602;

SELECT script_line
FROM (
    SELECT 10 AS sort_order, CAST('-- Copy/paste the lines below into the target database' AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS script_line

    UNION ALL
    SELECT 20, CAST('START TRANSACTION;' AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci

    UNION ALL
    SELECT 30, CAST(CONCAT('-- Design-only export generated for source formId ', @form_id) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci

    UNION ALL
    SELECT
        100,
        CAST(CONCAT(
            'INSERT INTO cfg_tbl_custom_form (',
            'txt_form_name, txt_form_code, txt_convention_prefix, txt_form_description, ',
            'txt_user_ids, bl_is_active, bl_is_deleted, bln_status, dte_created_date',
            ') VALUES (',
            IFNULL(QUOTE(f.txt_form_name), 'NULL'), ', ',
            'NULL, ',
            IFNULL(QUOTE(f.txt_convention_prefix), 'NULL'), ', ',
            IFNULL(QUOTE(f.txt_form_description), 'NULL'), ', ',
            'NULL, ',
            '1, 0, 1, NOW());'
        ) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS script_line
    FROM cfg_tbl_custom_form f
    WHERE f.ser_form_id = @form_id

    UNION ALL
    SELECT 110, CAST('SET @new_form_id := LAST_INSERT_ID();' AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci

    UNION ALL
    SELECT
        200 + ROW_NUMBER() OVER (ORDER BY fld.int_field_order, fld.ser_field_id),
        CAST(CONCAT(
            'INSERT INTO cfg_tbl_custom_form_field (',
            'ser_form_id, txt_field_label, txt_field_type, txt_placeholder, ',
            'bl_is_required, int_field_order, txt_field_options, ',
            'bl_is_active, bl_is_deleted, dte_created_date',
            ') VALUES (',
            '@new_form_id, ',
            IFNULL(QUOTE(fld.txt_field_label), 'NULL'), ', ',
            IFNULL(QUOTE(fld.txt_field_type), 'NULL'), ', ',
            IFNULL(QUOTE(fld.txt_placeholder), 'NULL'), ', ',
            IFNULL(CAST(fld.bl_is_required AS UNSIGNED), '0'), ', ',
            IFNULL(fld.int_field_order, '0'), ', ',
            IFNULL(QUOTE(CAST(fld.txt_field_options AS CHAR)), 'NULL'), ', ',
            '1, 0, NOW());'
        ) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS script_line
    FROM cfg_tbl_custom_form_field fld
    WHERE fld.ser_form_id = @form_id

    UNION ALL
    SELECT
        600,
        CAST(CONCAT(
            'INSERT INTO tpl_template_definition (',
            'ser_form_id, txt_template_name, txt_code_convention, txt_template_payload, ',
            'bl_is_active, bl_is_deleted, bln_status, dte_created_date',
            ') VALUES (',
            '@new_form_id, ',
            IFNULL(QUOTE(t.txt_template_name), 'NULL'), ', ',
            IFNULL(QUOTE(t.txt_code_convention), 'NULL'), ', ',
            IFNULL(QUOTE(CAST(t.txt_template_payload AS CHAR)), 'NULL'), ', ',
            '1, 0, 1, NOW());'
        ) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS script_line
    FROM tpl_template_definition t
    WHERE t.ser_form_id = @form_id

    UNION ALL
    SELECT 900, CAST('COMMIT;' AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci

    UNION ALL
    SELECT 910, CAST('SELECT @new_form_id AS new_form_id;' AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
) exported_sql
ORDER BY sort_order;
