-- Run this on the SOURCE database.
-- It returns a copy/paste-ready SQL script you can run on the TARGET database
-- to recreate formId 602 with its related records.

SET @form_id := 602;

SELECT script_line
FROM (
    SELECT 10 AS sort_order, '-- Copy/paste the lines below into the target database' AS script_line

    UNION ALL
    SELECT 20, 'START TRANSACTION;'

    UNION ALL
    SELECT 30, 'SET FOREIGN_KEY_CHECKS = 0;'

    UNION ALL
    SELECT 40, CONCAT('-- Export generated for formId ', @form_id)

    UNION ALL
    SELECT 50, CONCAT(
        'DELETE FROM tpl_template_definition WHERE ser_form_id = ', @form_id, ';'
    )

    UNION ALL
    SELECT 60, CONCAT(
        'DELETE FROM cfg_tbl_custom_form_approval_pipeline WHERE ser_form_id = ', @form_id, ';'
    )

    UNION ALL
    SELECT 70, CONCAT(
        'DELETE FROM cfg_tbl_custom_form_field WHERE ser_form_id = ', @form_id, ';'
    )

    UNION ALL
    SELECT 80, CONCAT(
        'DELETE FROM cfg_tbl_custom_form WHERE ser_form_id = ', @form_id, ';'
    )

    UNION ALL
    SELECT
        100,
        CONCAT(
            'INSERT INTO cfg_tbl_custom_form (',
            'ser_form_id, txt_form_name, txt_form_code, txt_convention_prefix, txt_form_description, ',
            'txt_user_ids, txt_approval_pipeline, bl_is_active, bl_is_deleted, bln_status, ',
            'dte_created_date, dte_modified_date, ser_created_user, ser_modified_user',
            ') VALUES (',
            f.ser_form_id, ', ',
            IFNULL(QUOTE(f.txt_form_name), 'NULL'), ', ',
            IFNULL(QUOTE(f.txt_form_code), 'NULL'), ', ',
            IFNULL(QUOTE(f.txt_convention_prefix), 'NULL'), ', ',
            IFNULL(QUOTE(f.txt_form_description), 'NULL'), ', ',
            IFNULL(QUOTE(CAST(f.txt_user_ids AS CHAR)), 'NULL'), ', ',
            IFNULL(QUOTE(CAST(f.txt_approval_pipeline AS CHAR)), 'NULL'), ', ',
            IFNULL(CAST(f.bl_is_active AS UNSIGNED), 'NULL'), ', ',
            IFNULL(CAST(f.bl_is_deleted AS UNSIGNED), 'NULL'), ', ',
            IFNULL(CAST(f.bln_status AS UNSIGNED), 'NULL'), ', ',
            IF(f.dte_created_date IS NULL, 'NULL', QUOTE(DATE_FORMAT(f.dte_created_date, '%Y-%m-%d %H:%i:%s'))), ', ',
            IF(f.dte_modified_date IS NULL, 'NULL', QUOTE(DATE_FORMAT(f.dte_modified_date, '%Y-%m-%d %H:%i:%s'))), ', ',
            IFNULL(f.ser_created_user, 'NULL'), ', ',
            IFNULL(f.ser_modified_user, 'NULL'),
            ');'
        ) AS script_line
    FROM cfg_tbl_custom_form f
    WHERE f.ser_form_id = @form_id

    UNION ALL
    SELECT
        200 + ROW_NUMBER() OVER (ORDER BY fld.int_field_order, fld.ser_field_id),
        CONCAT(
            'INSERT INTO cfg_tbl_custom_form_field (',
            'ser_field_id, ser_form_id, txt_field_label, txt_field_type, txt_placeholder, ',
            'bl_is_required, int_field_order, txt_field_options, bl_is_active, bl_is_deleted, ',
            'dte_created_date, dte_modified_date, ser_created_user, ser_modified_user',
            ') VALUES (',
            fld.ser_field_id, ', ',
            fld.ser_form_id, ', ',
            IFNULL(QUOTE(fld.txt_field_label), 'NULL'), ', ',
            IFNULL(QUOTE(fld.txt_field_type), 'NULL'), ', ',
            IFNULL(QUOTE(fld.txt_placeholder), 'NULL'), ', ',
            IFNULL(CAST(fld.bl_is_required AS UNSIGNED), 'NULL'), ', ',
            IFNULL(fld.int_field_order, 'NULL'), ', ',
            IFNULL(QUOTE(CAST(fld.txt_field_options AS CHAR)), 'NULL'), ', ',
            IFNULL(CAST(fld.bl_is_active AS UNSIGNED), 'NULL'), ', ',
            IFNULL(CAST(fld.bl_is_deleted AS UNSIGNED), 'NULL'), ', ',
            IF(fld.dte_created_date IS NULL, 'NULL', QUOTE(DATE_FORMAT(fld.dte_created_date, '%Y-%m-%d %H:%i:%s'))), ', ',
            IF(fld.dte_modified_date IS NULL, 'NULL', QUOTE(DATE_FORMAT(fld.dte_modified_date, '%Y-%m-%d %H:%i:%s'))), ', ',
            IFNULL(fld.ser_created_user, 'NULL'), ', ',
            IFNULL(fld.ser_modified_user, 'NULL'),
            ');'
        ) AS script_line
    FROM cfg_tbl_custom_form_field fld
    WHERE fld.ser_form_id = @form_id

    UNION ALL
    SELECT
        400 + ROW_NUMBER() OVER (ORDER BY ap.int_approval_order, ap.ser_approval_pipeline_id),
        CONCAT(
            'INSERT INTO cfg_tbl_custom_form_approval_pipeline (',
            'ser_approval_pipeline_id, ser_form_id, ser_department_id, int_approval_order, ',
            'bl_is_active, bl_is_deleted, dte_created_date, dte_modified_date, ',
            'ser_created_user, ser_modified_user',
            ') VALUES (',
            ap.ser_approval_pipeline_id, ', ',
            ap.ser_form_id, ', ',
            ap.ser_department_id, ', ',
            IFNULL(ap.int_approval_order, 'NULL'), ', ',
            IFNULL(CAST(ap.bl_is_active AS UNSIGNED), 'NULL'), ', ',
            IFNULL(CAST(ap.bl_is_deleted AS UNSIGNED), 'NULL'), ', ',
            IF(ap.dte_created_date IS NULL, 'NULL', QUOTE(DATE_FORMAT(ap.dte_created_date, '%Y-%m-%d %H:%i:%s'))), ', ',
            IF(ap.dte_modified_date IS NULL, 'NULL', QUOTE(DATE_FORMAT(ap.dte_modified_date, '%Y-%m-%d %H:%i:%s'))), ', ',
            IFNULL(ap.ser_created_user, 'NULL'), ', ',
            IFNULL(ap.ser_modified_user, 'NULL'),
            ');'
        ) AS script_line
    FROM cfg_tbl_custom_form_approval_pipeline ap
    WHERE ap.ser_form_id = @form_id

    UNION ALL
    SELECT
        600,
        CONCAT(
            'INSERT INTO tpl_template_definition (',
            'ser_template_id, ser_form_id, txt_template_name, txt_code_convention, txt_template_payload, ',
            'bl_is_active, bl_is_deleted, bln_status, dte_created_date, dte_modified_date, ',
            'ser_created_user, ser_modified_user',
            ') VALUES (',
            t.ser_template_id, ', ',
            t.ser_form_id, ', ',
            IFNULL(QUOTE(t.txt_template_name), 'NULL'), ', ',
            IFNULL(QUOTE(t.txt_code_convention), 'NULL'), ', ',
            IFNULL(QUOTE(CAST(t.txt_template_payload AS CHAR)), 'NULL'), ', ',
            IFNULL(CAST(t.bl_is_active AS UNSIGNED), 'NULL'), ', ',
            IFNULL(CAST(t.bl_is_deleted AS UNSIGNED), 'NULL'), ', ',
            IFNULL(CAST(t.bln_status AS UNSIGNED), 'NULL'), ', ',
            IF(t.dte_created_date IS NULL, 'NULL', QUOTE(DATE_FORMAT(t.dte_created_date, '%Y-%m-%d %H:%i:%s'))), ', ',
            IF(t.dte_modified_date IS NULL, 'NULL', QUOTE(DATE_FORMAT(t.dte_modified_date, '%Y-%m-%d %H:%i:%s'))), ', ',
            IFNULL(t.ser_created_user, 'NULL'), ', ',
            IFNULL(t.ser_modified_user, 'NULL'),
            ');'
        ) AS script_line
    FROM tpl_template_definition t
    WHERE t.ser_form_id = @form_id

    UNION ALL
    SELECT 900, 'SET FOREIGN_KEY_CHECKS = 1;'

    UNION ALL
    SELECT 910, 'COMMIT;'

    UNION ALL
    SELECT 920, CONCAT('-- Done for formId ', @form_id)
) exported_sql
ORDER BY sort_order;
