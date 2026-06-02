-- Use the correct database for your environment before running:
-- USE velocity;

-- Clone "CAPF Form IT" into a new form called "CAPF form 123"
-- This script copies the form definition and all associated fields.

SET @source_form_name = 'CAPF Form IT';
SET @target_form_name = 'CAPF form 123';
SET @target_form_code = 'CAPF123';

-- Resolve source form id
SET @source_form_id = (
    SELECT ser_form_id
    FROM cfg_tbl_custom_form
    WHERE BINARY txt_form_name = BINARY @source_form_name
      AND COALESCE(bl_is_deleted, 0) = 0
    ORDER BY ser_form_id DESC
    LIMIT 1
);

-- Safety checks before cloning
SELECT
    @source_form_id AS source_form_id,
    @source_form_name AS source_form_name,
    @target_form_name AS target_form_name;

-- 1) Source form must exist
-- 2) Target form name must not already exist
-- If either condition fails, no rows are inserted.

START TRANSACTION;

INSERT INTO cfg_tbl_custom_form (
    txt_form_name,
    txt_form_code,
    txt_convention_prefix,
    txt_form_description,
    txt_user_ids,
    txt_approval_pipeline,
    bl_is_active,
    bl_is_deleted,
    bln_status,
    dte_created_date,
    dte_modified_date,
    ser_created_user,
    ser_modified_user
)
SELECT
    @target_form_name,
    @target_form_code,
    txt_convention_prefix,
    txt_form_description,
    txt_user_ids,
    txt_approval_pipeline,
    bl_is_active,
    0,
    bln_status,
    NOW(),
    NOW(),
    ser_created_user,
    ser_modified_user
FROM cfg_tbl_custom_form src
WHERE src.ser_form_id = @source_form_id
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_custom_form t
      WHERE BINARY t.txt_form_name = BINARY @target_form_name
        AND COALESCE(t.bl_is_deleted, 0) = 0
  );

SET @new_form_id = LAST_INSERT_ID();

INSERT INTO cfg_tbl_custom_form_field (
    ser_form_id,
    txt_field_label,
    txt_field_type,
    txt_placeholder,
    bl_is_required,
    int_field_order,
    txt_field_options,
    bl_is_active,
    bl_is_deleted,
    dte_created_date,
    dte_modified_date,
    ser_created_user,
    ser_modified_user
)
SELECT
    @new_form_id,
    f.txt_field_label,
    f.txt_field_type,
    f.txt_placeholder,
    f.bl_is_required,
    f.int_field_order,
    f.txt_field_options,
    f.bl_is_active,
    0,
    NOW(),
    NOW(),
    f.ser_created_user,
    f.ser_modified_user
FROM cfg_tbl_custom_form_field f
WHERE f.ser_form_id = @source_form_id
  AND @new_form_id IS NOT NULL
  AND @new_form_id > 0;

COMMIT;

-- Verification
SELECT
    ser_form_id,
    txt_form_name,
    txt_form_code,
    bl_is_active,
    bl_is_deleted
FROM cfg_tbl_custom_form
WHERE ser_form_id = @new_form_id;

SELECT
    ser_form_id,
    COUNT(*) AS field_count
FROM cfg_tbl_custom_form_field
WHERE ser_form_id IN (@source_form_id, @new_form_id)
GROUP BY ser_form_id
ORDER BY ser_form_id;
