-- =============================================================================
-- Clone main CAPF form -> CAPF QU and CAPF QF (staging / production)
-- =============================================================================
-- Staging source (from your data):
--   ser_form_id = 572
--   txt_form_name = 'CAPF Form Hattar'
--   txt_form_code = 'CAPF-0001'
--   txt_convention_prefix = 'CAPF'
--
-- Local dev source is usually:
--   txt_form_name = 'CAPF Form'
--   txt_form_code = 'CAPF-0001'
--
-- Idempotent: safe to re-run. Existing source form is never modified.
-- Logo/title: handled in app code by target form name (CAPF QU / CAPF QF).
-- =============================================================================

-- USE vim_3;
USE velocity_workbench;

-- Optional hard override for staging (uncomment only if auto-detect fails):
-- SET @source_form_id = 572;

SET @source_form_id = IFNULL(@source_form_id, (
        SELECT ser_form_id
        FROM cfg_tbl_custom_form
        WHERE COALESCE(bl_is_deleted, 0) = 0
          AND UPPER(TRIM(txt_form_code)) = 'CAPF-0001'
        ORDER BY ser_form_id
        LIMIT 1
    ));

SET @source_form_id = IFNULL(@source_form_id, (
        SELECT ser_form_id
        FROM cfg_tbl_custom_form
        WHERE COALESCE(bl_is_deleted, 0) = 0
          AND txt_form_name IN ('CAPF Form Hattar', 'CAPF Form')
        ORDER BY ser_form_id
        LIMIT 1
    ));

SET @source_form_name = (
    SELECT txt_form_name
    FROM cfg_tbl_custom_form
    WHERE ser_form_id = @source_form_id
    LIMIT 1
);

SELECT
    @source_form_id AS source_form_id,
    @source_form_name AS source_form_name,
    (SELECT txt_form_code FROM cfg_tbl_custom_form WHERE ser_form_id = @source_form_id) AS source_form_code,
    (SELECT COUNT(*)
     FROM cfg_tbl_custom_form_field
     WHERE ser_form_id = @source_form_id
       AND COALESCE(bl_is_deleted, 0) = 0) AS source_field_count,
    CASE
        WHEN @source_form_id IS NULL THEN 'STOP - source CAPF form not found'
        ELSE 'OK - ready to clone'
    END AS status;

-- Abort early if source missing (MySQL has no IF/RETURN in scripts; inserts simply no-op)
-- Check the status row above before expecting new forms.

-- ---------------------------------------------------------------------------
-- Clone CAPF QU
-- ---------------------------------------------------------------------------
SET @target_form_name = 'CAPF QU';
SET @target_form_code = 'CAPF-QU';

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

SET @new_form_id_qu = LAST_INSERT_ID();

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
    @new_form_id_qu,
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
  AND COALESCE(f.bl_is_deleted, 0) = 0
  AND @new_form_id_qu IS NOT NULL
  AND @new_form_id_qu > 0;

COMMIT;

SELECT
    CASE
        WHEN @source_form_id IS NULL THEN 'SKIPPED - source CAPF form not found'
        WHEN @new_form_id_qu > 0 THEN CONCAT('CAPF QU created, ser_form_id=', @new_form_id_qu)
        ELSE 'CAPF QU already exists - no new row inserted'
    END AS capf_qu_result;

-- ---------------------------------------------------------------------------
-- Clone CAPF QF
-- ---------------------------------------------------------------------------
SET @target_form_name = 'CAPF QF';
SET @target_form_code = 'CAPF-QF';

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

SET @new_form_id_qf = LAST_INSERT_ID();

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
    @new_form_id_qf,
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
  AND COALESCE(f.bl_is_deleted, 0) = 0
  AND @new_form_id_qf IS NOT NULL
  AND @new_form_id_qf > 0;

COMMIT;

SELECT
    CASE
        WHEN @source_form_id IS NULL THEN 'SKIPPED - source CAPF form not found'
        WHEN @new_form_id_qf > 0 THEN CONCAT('CAPF QF created, ser_form_id=', @new_form_id_qf)
        ELSE 'CAPF QF already exists - no new row inserted'
    END AS capf_qf_result;

-- ---------------------------------------------------------------------------
-- Verification
-- ---------------------------------------------------------------------------
SELECT
    ser_form_id,
    txt_form_name,
    txt_form_code,
    txt_convention_prefix,
    bl_is_active
FROM cfg_tbl_custom_form
WHERE (
        txt_form_name IN ('CAPF Form Hattar', 'CAPF Form', 'CAPF QU', 'CAPF QF')
     OR txt_form_code IN ('CAPF-0001', 'CAPF-QU', 'CAPF-QF')
      )
  AND COALESCE(bl_is_deleted, 0) = 0
ORDER BY ser_form_id;

SELECT
    f.ser_form_id,
    cf.txt_form_name,
    COUNT(*) AS field_count
FROM cfg_tbl_custom_form_field f
JOIN cfg_tbl_custom_form cf ON cf.ser_form_id = f.ser_form_id
WHERE cf.txt_form_name IN ('CAPF Form Hattar', 'CAPF Form', 'CAPF QU', 'CAPF QF')
  AND COALESCE(f.bl_is_deleted, 0) = 0
  AND COALESCE(cf.bl_is_deleted, 0) = 0
GROUP BY f.ser_form_id, cf.txt_form_name
ORDER BY f.ser_form_id;
