-- =============================================================================
-- Diagnose 500 errors on pending approvals (getApplicationsPendingApproval /
-- getAllApplicationsPendingApproval). Run in MySQL against the SAME database
-- the staging app uses (edit USE line below).
--
-- How to run:
--   mysql -h HOST -u USER -p YOUR_DB < diagnose_pending_approvals.sql
--   or paste sections into MySQL Workbench.
-- =============================================================================

-- >>> Set your staging schema <<<
-- USE your_staging_db;

SELECT DATABASE() AS current_schema, NOW() AS run_at;

-- -----------------------------------------------------------------------------
-- 1) Core tables exist?
-- -----------------------------------------------------------------------------
SELECT
    t.TABLE_NAME,
    t.TABLE_ROWS AS approx_rows,
    t.ENGINE
FROM information_schema.TABLES t
WHERE t.TABLE_SCHEMA = DATABASE()
  AND t.TABLE_NAME IN (
    'cfg_tbl_custom_form_application',
    'cfg_tbl_custom_form',
    'cfg_tbl_custom_form_field',
    'cfg_tbl_user'
  )
ORDER BY t.TABLE_NAME;

-- If any table is missing from this result, Hibernate/JPA will fail hard.

-- -----------------------------------------------------------------------------
-- 2) cfg_tbl_custom_form_application — columns expected by Java entity
--    (CfgTblCustomFormApplication). Any row with status <<< MISSING >>> must be
--    added (or run app with spring.jpa.hibernate.ddl-auto=update once).
-- -----------------------------------------------------------------------------
SELECT
    e.expected_col,
    CASE WHEN c.COLUMN_NAME IS NULL THEN '<<< MISSING >>>' ELSE 'OK' END AS status,
    c.DATA_TYPE,
    c.IS_NULLABLE
FROM (
    SELECT 'ser_application_id' AS expected_col UNION ALL SELECT 'ser_form_id'
    UNION ALL SELECT 'txt_form_code' UNION ALL SELECT 'txt_application_data'
    UNION ALL SELECT 'txt_status' UNION ALL SELECT 'int_current_approval_level'
    UNION ALL SELECT 'ser_submitted_by' UNION ALL SELECT 'ser_current_approver'
    UNION ALL SELECT 'txt_remarks' UNION ALL SELECT 'txt_approval_history'
    UNION ALL SELECT 'txt_prior_approvals' UNION ALL SELECT 'txt_asset_code'
    UNION ALL SELECT 'txt_pr_code' UNION ALL SELECT 'blb_pdf_data'
    UNION ALL SELECT 'blb_pdf_stage_0' UNION ALL SELECT 'blb_pdf_stage_1'
    UNION ALL SELECT 'blb_pdf_stage_2' UNION ALL SELECT 'blb_pdf_stage_3'
    UNION ALL SELECT 'blb_pdf_stage_4' UNION ALL SELECT 'blb_pdf_stage_5'
    UNION ALL SELECT 'blb_pdf_stage_6' UNION ALL SELECT 'blb_pdf_stage_7'
    UNION ALL SELECT 'blb_pdf_stage_8' UNION ALL SELECT 'blb_pdf_stage_9'
    UNION ALL SELECT 'txt_pdf_name' UNION ALL SELECT 'txt_pdf_mime'
    UNION ALL SELECT 'bl_is_active' UNION ALL SELECT 'bl_is_deleted'
    UNION ALL SELECT 'bln_status' UNION ALL SELECT 'dte_created_date'
    UNION ALL SELECT 'dte_modified_date' UNION ALL SELECT 'ser_created_user'
    UNION ALL SELECT 'ser_modified_user'
) e
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = DATABASE()
 AND c.TABLE_NAME = 'cfg_tbl_custom_form_application'
 AND c.COLUMN_NAME = e.expected_col
ORDER BY e.expected_col;

-- -----------------------------------------------------------------------------
-- 3) cfg_tbl_custom_form — columns expected by CfgTblCustomForm (JOIN FETCH)
-- -----------------------------------------------------------------------------
SELECT
    e.expected_col,
    CASE WHEN c.COLUMN_NAME IS NULL THEN '<<< MISSING >>>' ELSE 'OK' END AS status,
    c.DATA_TYPE
FROM (
    SELECT 'ser_form_id' AS expected_col UNION ALL SELECT 'txt_form_name'
    UNION ALL SELECT 'txt_form_code' UNION ALL SELECT 'txt_convention_prefix'
    UNION ALL SELECT 'txt_form_description' UNION ALL SELECT 'txt_approval_pipeline'
    UNION ALL SELECT 'bl_is_active' UNION ALL SELECT 'bl_is_deleted'
    UNION ALL SELECT 'bln_status' UNION ALL SELECT 'dte_created_date'
    UNION ALL SELECT 'dte_modified_date' UNION ALL SELECT 'ser_created_user'
    UNION ALL SELECT 'ser_modified_user'
) e
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = DATABASE()
 AND c.TABLE_NAME = 'cfg_tbl_custom_form'
 AND c.COLUMN_NAME = e.expected_col
ORDER BY e.expected_col;

-- -----------------------------------------------------------------------------
-- 4) cfg_tbl_custom_form_field — columns expected by CfgTblCustomFormField
-- -----------------------------------------------------------------------------
SELECT
    e.expected_col,
    CASE WHEN c.COLUMN_NAME IS NULL THEN '<<< MISSING >>>' ELSE 'OK' END AS status,
    c.DATA_TYPE
FROM (
    SELECT 'ser_field_id' AS expected_col UNION ALL SELECT 'ser_form_id'
    UNION ALL SELECT 'txt_field_label' UNION ALL SELECT 'txt_field_type'
    UNION ALL SELECT 'txt_placeholder' UNION ALL SELECT 'bl_is_required'
    UNION ALL SELECT 'int_field_order' UNION ALL SELECT 'txt_field_options'
    UNION ALL SELECT 'bl_is_active' UNION ALL SELECT 'bl_is_deleted'
    UNION ALL SELECT 'dte_created_date' UNION ALL SELECT 'dte_modified_date'
    UNION ALL SELECT 'ser_created_user' UNION ALL SELECT 'ser_modified_user'
) e
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = DATABASE()
 AND c.TABLE_NAME = 'cfg_tbl_custom_form_field'
 AND c.COLUMN_NAME = e.expected_col
ORDER BY e.expected_col;

-- -----------------------------------------------------------------------------
-- 5) cfg_tbl_user — column used by pending-filter preload (ser_department_id)
-- -----------------------------------------------------------------------------
SELECT
    CASE WHEN COUNT(*) > 0 THEN 'OK' ELSE '<<< MISSING ser_department_id >>>' END AS ser_department_id_check
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'cfg_tbl_user'
  AND COLUMN_NAME = 'ser_department_id';

-- -----------------------------------------------------------------------------
-- 6) Raw SQL close to the JPA pending query (no Hibernate). If this errors,
--    the stack trace / errno points to the real SQL problem.
-- -----------------------------------------------------------------------------
SELECT
    a.ser_application_id,
    a.txt_status,
    a.int_current_approval_level,
    a.ser_submitted_by,
    a.ser_current_approver,
    f.ser_form_id,
    f.txt_form_name
FROM cfg_tbl_custom_form_application a
LEFT JOIN cfg_tbl_custom_form f ON f.ser_form_id = a.ser_form_id
WHERE a.txt_status IN (
    'PENDING', 'IN_PROGRESS', 'CEO_PENDING', 'ASSET_PENDING', 'PR_PENDING'
)
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
ORDER BY a.dte_created_date DESC
LIMIT 50;

-- -----------------------------------------------------------------------------
-- 7) Orphan / bad FK: applications pointing at missing form rows
-- -----------------------------------------------------------------------------
SELECT
    a.ser_application_id,
    a.ser_form_id,
    a.txt_status
FROM cfg_tbl_custom_form_application a
LEFT JOIN cfg_tbl_custom_form f ON f.ser_form_id = a.ser_form_id
WHERE (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
  AND a.ser_form_id IS NOT NULL
  AND f.ser_form_id IS NULL
LIMIT 100;

-- -----------------------------------------------------------------------------
-- 8) Pending counts by status (sanity)
-- -----------------------------------------------------------------------------
SELECT
    txt_status,
    COUNT(*) AS cnt
FROM cfg_tbl_custom_form_application
WHERE (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
GROUP BY txt_status
ORDER BY cnt DESC;

-- =============================================================================
-- Next steps if SQL is all OK but HTTP is still 500:
-- * Check application log at ERROR for the exact exception (often Jackson
--   serialization or LazyInitializationException — less common here due to FETCH).
-- * Call API with curl and capture response body / Tomcat log line.
-- * Confirm staging uses the same code + DB schema version as dev.
-- =============================================================================
