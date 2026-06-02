-- =============================================================================
-- Export application details + full approval log for a given form code.
-- Default: CAPF-0144 (change @form_code below).
--
-- Run (local defaults from application.properties):
--   mysql -h 127.0.0.1 -P 3308 -u root -p velocity_workbench < export_application_by_code.sql
--
-- Or in MySQL Workbench: set @form_code and run all sections.
-- =============================================================================

SET @form_code = 'CAPF-0144';

SELECT DATABASE() AS current_schema, @form_code AS form_code, NOW() AS run_at;

-- -----------------------------------------------------------------------------
-- 1) Application row (workflow state — excludes large PDF blobs)
-- -----------------------------------------------------------------------------
SELECT
    a.ser_application_id,
    a.txt_form_code,
    a.ser_form_id,
    f.txt_form_name,
    a.txt_status,
    a.int_current_approval_level,
    a.ser_submitted_by,
    submitter.txt_user_name AS submitter_name,
    submitter.txt_address AS submitter_email,
    a.ser_current_approver,
    approver.txt_user_name AS current_approver_name,
    approver.txt_address AS current_approver_email,
    approver_role.txt_role_name AS current_approver_role,
    a.txt_remarks AS application_txt_remarks,
    a.txt_asset_code,
    a.txt_pr_code,
    a.dte_created_date,
    a.dte_modified_date,
    a.ser_modified_user,
    modifier.txt_user_name AS last_modified_by_name,
    CASE WHEN a.blb_pdf_data IS NOT NULL THEN LENGTH(a.blb_pdf_data) ELSE 0 END AS pdf_bytes,
    a.txt_pdf_name,
    a.bl_is_deleted
FROM cfg_tbl_custom_form_application a
LEFT JOIN cfg_tbl_custom_form f ON f.ser_form_id = a.ser_form_id
LEFT JOIN cfg_tbl_user submitter ON submitter.ser_user_id = a.ser_submitted_by
LEFT JOIN cfg_tbl_user approver ON approver.ser_user_id = a.ser_current_approver
LEFT JOIN cfg_tbl_role approver_role ON approver_role.ser_role_id = approver.ser_role_id
LEFT JOIN cfg_tbl_user modifier ON modifier.ser_user_id = a.ser_modified_user
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL);

-- Stop if not found
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN CONCAT('ERROR: No application found for txt_form_code = ', @form_code)
        ELSE CONCAT('OK: Found ', COUNT(*), ' application(s) for ', @form_code)
    END AS lookup_status
FROM cfg_tbl_custom_form_application a
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL);

-- -----------------------------------------------------------------------------
-- 2) Approval pipeline from form template (for context)
-- -----------------------------------------------------------------------------
SELECT
    a.txt_form_code,
    f.txt_form_name,
    f.txt_approval_pipeline
FROM cfg_tbl_custom_form_application a
JOIN cfg_tbl_custom_form f ON f.ser_form_id = a.ser_form_id
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL);

-- -----------------------------------------------------------------------------
-- 3) Full approval history — one row per log entry (ordered)
-- -----------------------------------------------------------------------------
SELECT
    a.ser_application_id,
    a.txt_form_code,
    jt.row_idx AS history_index,
    jt.entry_action AS action,
    jt.entry_level AS level,
    jt.department_id,
    jt.department_name,
    jt.entry_role AS role,
    jt.approved_by,
    jt.approver_name,
    jt.designation,
    jt.approved_date,
    jt.approved_at,
    jt.approved_via,
    jt.approved_ip,
    jt.entry_remarks AS remarks
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_approval_history, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_action VARCHAR(50) PATH '$.action',
        entry_level INT PATH '$.level',
        department_id INT PATH '$.departmentId',
        department_name VARCHAR(255) PATH '$.departmentName',
        entry_role VARCHAR(255) PATH '$.role',
        approved_by INT PATH '$.approvedBy',
        approver_name VARCHAR(255) PATH '$.approverName',
        designation VARCHAR(255) PATH '$.designation',
        approved_date VARCHAR(64) PATH '$.approvedDate',
        approved_at VARCHAR(64) PATH '$.approvedAt',
        approved_via VARCHAR(32) PATH '$.approvedVia',
        approved_ip VARCHAR(64) PATH '$.approvedIp',
        entry_remarks TEXT PATH '$.remarks'
    )
) AS jt
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
ORDER BY jt.row_idx;

-- -----------------------------------------------------------------------------
-- 4) Prior approvals archive (if any)
-- -----------------------------------------------------------------------------
SELECT
    a.ser_application_id,
    a.txt_form_code,
    jt.row_idx AS prior_index,
    jt.entry_action AS action,
    jt.entry_level AS level,
    jt.department_name,
    jt.approved_by,
    jt.approver_name,
    jt.approved_date,
    jt.entry_remarks AS remarks
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_prior_approvals, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_action VARCHAR(50) PATH '$.action',
        entry_level INT PATH '$.level',
        department_name VARCHAR(255) PATH '$.departmentName',
        approved_by INT PATH '$.approvedBy',
        approver_name VARCHAR(255) PATH '$.approverName',
        approved_date VARCHAR(64) PATH '$.approvedDate',
        entry_remarks TEXT PATH '$.remarks'
    )
) AS jt
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
  AND a.txt_prior_approvals IS NOT NULL
  AND JSON_LENGTH(a.txt_prior_approvals) > 0
ORDER BY jt.row_idx;

-- -----------------------------------------------------------------------------
-- 5) LAST history entry (any action)
-- -----------------------------------------------------------------------------
SELECT
    'LAST_HISTORY_ENTRY' AS section,
    a.ser_application_id,
    a.txt_form_code,
    jt.row_idx AS history_index,
    jt.entry_action AS action,
    jt.entry_level AS level,
    jt.department_name,
    jt.entry_role AS role,
    jt.approved_by,
    jt.approver_name,
    jt.approved_date,
    jt.entry_remarks AS remarks,
    a.txt_remarks AS application_txt_remarks
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_approval_history, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_action VARCHAR(50) PATH '$.action',
        entry_level INT PATH '$.level',
        department_name VARCHAR(255) PATH '$.departmentName',
        entry_role VARCHAR(255) PATH '$.role',
        approved_by INT PATH '$.approvedBy',
        approver_name VARCHAR(255) PATH '$.approverName',
        approved_date VARCHAR(64) PATH '$.approvedDate',
        entry_remarks TEXT PATH '$.remarks'
    )
) AS jt
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
ORDER BY jt.row_idx DESC
LIMIT 1;

-- -----------------------------------------------------------------------------
-- 6) Last APPROVED entry (typical "last approver before CEO")
-- -----------------------------------------------------------------------------
SELECT
    'LAST_APPROVED_ENTRY' AS section,
    a.ser_application_id,
    a.txt_form_code,
    jt.row_idx AS history_index,
    jt.entry_level AS level,
    jt.department_name,
    jt.entry_role AS role,
    jt.approved_by,
    jt.approver_name,
    jt.approved_date,
    jt.approved_at,
    jt.approved_via,
    jt.entry_remarks AS remarks,
    a.txt_remarks AS application_txt_remarks
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_approval_history, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_action VARCHAR(50) PATH '$.action',
        entry_level INT PATH '$.level',
        department_name VARCHAR(255) PATH '$.departmentName',
        entry_role VARCHAR(255) PATH '$.role',
        approved_by INT PATH '$.approvedBy',
        approver_name VARCHAR(255) PATH '$.approverName',
        approved_date VARCHAR(64) PATH '$.approvedDate',
        approved_at VARCHAR(64) PATH '$.approvedAt',
        approved_via VARCHAR(32) PATH '$.approvedVia',
        entry_remarks TEXT PATH '$.remarks'
    )
) AS jt
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
  AND UPPER(COALESCE(jt.entry_action, '')) = 'APPROVED'
ORDER BY jt.row_idx DESC
LIMIT 1;

-- -----------------------------------------------------------------------------
-- 7) Last "Core team" approval (department/role name contains "core")
-- -----------------------------------------------------------------------------
SELECT
    'LAST_CORE_TEAM_APPROVED' AS section,
    a.ser_application_id,
    a.txt_form_code,
    jt.row_idx AS history_index,
    jt.entry_level AS level,
    jt.department_name,
    jt.entry_role AS role,
    jt.approved_by,
    jt.approver_name,
    jt.approved_date,
    jt.approved_at,
    jt.approved_via,
    jt.entry_remarks AS core_team_remarks,
    a.txt_remarks AS application_txt_remarks
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_approval_history, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_action VARCHAR(50) PATH '$.action',
        entry_level INT PATH '$.level',
        department_name VARCHAR(255) PATH '$.departmentName',
        entry_role VARCHAR(255) PATH '$.role',
        approved_by INT PATH '$.approvedBy',
        approver_name VARCHAR(255) PATH '$.approverName',
        approved_date VARCHAR(64) PATH '$.approvedDate',
        approved_at VARCHAR(64) PATH '$.approvedAt',
        approved_via VARCHAR(32) PATH '$.approvedVia',
        entry_remarks TEXT PATH '$.remarks'
    )
) AS jt
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
  AND UPPER(COALESCE(jt.entry_action, '')) = 'APPROVED'
  AND (
        UPPER(COALESCE(jt.department_name, '')) LIKE '%CORE%'
     OR UPPER(COALESCE(jt.entry_role, '')) LIKE '%CORE%'
  )
ORDER BY jt.row_idx DESC
LIMIT 1;

-- -----------------------------------------------------------------------------
-- 8) Raw JSON blobs (for backup before rollback) — copy these for restore
-- -----------------------------------------------------------------------------
SELECT
    a.ser_application_id,
    a.txt_form_code,
    a.txt_status,
    a.int_current_approval_level,
    a.ser_current_approver,
    a.txt_remarks,
    a.txt_approval_history,
    a.txt_prior_approvals
FROM cfg_tbl_custom_form_application a
WHERE a.txt_form_code = @form_code
  AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL);
