-- =============================================================================
-- QUICK: Seed one CAPF application at PO_PENDING (copy/paste into MySQL Workbench)
--
-- Database: velocity_workbench
-- Creates: NEW application with asset + PR codes set, PO code empty
-- History: HOD + pipeline + CEO + Finance + PR (all done)
--
-- IMPORTANT: Run the ENTIRE script in one go (not just the last SELECT).
-- If you ran the CEO seed script earlier in the same Workbench tab, stale
-- @test_form_code can point at the wrong application — this script clears that.
--
-- Prerequisite: txt_po_code column exists (run add_po_code_to_application.sql once)
--
-- Optional — uncomment ONE line before running for a fixed application code:
--   SET @test_form_code = CONVERT('CAPF-TEST-PO-MY001' USING utf8mb4) COLLATE utf8mb4_unicode_ci;
-- =============================================================================

USE velocity_workbench;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- Clear stale session variables (e.g. from seed_capf_application_at_ceo_pending.sql)
SET @test_form_code = NULL;
SET @test_asset_code = NULL;
SET @test_pr_code = NULL;

-- ---------------------------------------------------------------------------
-- 1) CAPF form
-- ---------------------------------------------------------------------------
SET @form_id = (
    SELECT ser_form_id
    FROM cfg_tbl_custom_form
    WHERE COALESCE(bl_is_deleted, 0) = 0
      AND (
          UPPER(TRIM(txt_form_code)) = 'CAPF-0001'
          OR txt_form_name IN ('CAPF Form', 'CAPF Form Hattar')
      )
    ORDER BY ser_form_id
    LIMIT 1
);

SET @pipeline_json = (SELECT txt_approval_pipeline FROM cfg_tbl_custom_form WHERE ser_form_id = @form_id LIMIT 1);
SET @pipeline_size = IFNULL(JSON_LENGTH(@pipeline_json), 0);
SET @post_pipeline_level = @pipeline_size + 1;

-- ---------------------------------------------------------------------------
-- 2) Users (auto-resolve from DB)
-- ---------------------------------------------------------------------------
SET @submitter_user_id = (
    SELECT u.ser_user_id FROM cfg_tbl_user u
    WHERE (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
      AND u.txt_address IS NOT NULL AND TRIM(u.txt_address) <> ''
    ORDER BY u.ser_user_id LIMIT 1
);

SET @ceo_user_id = (
    SELECT u.ser_user_id FROM cfg_tbl_user u
    JOIN cfg_tbl_role r ON r.ser_role_id = u.ser_role_id
    WHERE UPPER(TRIM(r.txt_role_name)) = 'CEO'
      AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
    ORDER BY u.ser_user_id LIMIT 1
);

SET @finance_user_id = (
    SELECT u.ser_user_id FROM cfg_tbl_user u
    JOIN cfg_tbl_role r ON r.ser_role_id = u.ser_role_id
    WHERE UPPER(TRIM(r.txt_role_name)) IN ('FINANCE_HEAD', 'FINANCE')
      AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
    ORDER BY CASE WHEN UPPER(TRIM(r.txt_role_name)) = 'FINANCE_HEAD' THEN 0 ELSE 1 END, u.ser_user_id
    LIMIT 1
);

SET @submitter_dept_id = (SELECT ser_department_id FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id LIMIT 1);

SET @hod_user_id = (
    SELECT CAST(TRIM(SUBSTRING_INDEX(d.ser_department_head_id, ',', 1)) AS UNSIGNED)
    FROM hr_tbl_department d
    WHERE d.ser_department_id = @submitter_dept_id
      AND d.ser_department_head_id IS NOT NULL AND TRIM(d.ser_department_head_id) <> ''
    LIMIT 1
);

-- Always generate a fresh unique code unless you set @test_form_code above this script block
SET @test_form_code = IFNULL(
    @test_form_code,
    CONVERT(
        CONCAT('CAPF-TEST-PO-', DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%f')),
        CHAR(64) CHARACTER SET utf8mb4
    ) COLLATE utf8mb4_unicode_ci
);

SET @test_asset_code = IFNULL(@test_asset_code, 'AST-TEST-PO-001');
SET @test_pr_code = IFNULL(@test_pr_code, 'PR-TEST-PO-001');

-- ---------------------------------------------------------------------------
-- Pre-flight check (read this if insert fails)
-- ---------------------------------------------------------------------------
SELECT
    @form_id AS form_id,
    @submitter_user_id AS submitter_user_id,
    @ceo_user_id AS ceo_user_id,
    @finance_user_id AS finance_user_id,
    @test_form_code AS new_form_code_will_be,
    CASE
        WHEN @form_id IS NULL THEN 'STOP - CAPF form not found'
        WHEN @submitter_user_id IS NULL THEN 'STOP - no submitter user with email'
        WHEN @ceo_user_id IS NULL THEN 'STOP - no CEO user'
        WHEN @finance_user_id IS NULL THEN 'STOP - no Finance user'
        WHEN EXISTS (
            SELECT 1 FROM cfg_tbl_custom_form_application a
            WHERE a.txt_form_code COLLATE utf8mb4_unicode_ci = @test_form_code COLLATE utf8mb4_unicode_ci
              AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
        ) THEN 'STOP - form code already exists (re-run script for new timestamp)'
        ELSE 'OK - ready to insert'
    END AS preflight_status;

-- ---------------------------------------------------------------------------
-- 3) Application data
-- ---------------------------------------------------------------------------
SET @template_app_data = (
    SELECT a.txt_application_data FROM cfg_tbl_custom_form_application a
    WHERE a.ser_form_id = @form_id AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
      AND a.txt_application_data IS NOT NULL
    ORDER BY a.dte_created_date DESC LIMIT 1
);

SET @app_data = IFNULL(@template_app_data, JSON_OBJECT(
    'vendor_name', 'Test Vendor Ltd',
    'vendor_address', '123 Test Street, Lahore',
    'approved_price', '100000',
    'delivery_period', '30 days / 2026-12-31',
    'terms_conditions', 'Standard payment terms',
    'NAME', 'Test Vendor Ltd',
    'ADDRESS', '123 Test Street, Lahore',
    'APPROVED PRICE', '100000',
    'DELIVERY PERIOD & DATE', '30 days / 2026-12-31',
    'TERMS & CONDITIONS', 'Standard payment terms'
));

SET @app_data = IF(@ceo_user_id IS NULL, @app_data, JSON_SET(@app_data, '$.ceo_user', JSON_OBJECT(
    'serUserId', @ceo_user_id,
    'txtUserName', (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
    'txtAddress', (SELECT txt_address FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
    'txtRole', 'CEO'
)));

-- ---------------------------------------------------------------------------
-- 4) Approval history
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_po_hist;
CREATE TEMPORARY TABLE tmp_po_hist (sort_order INT NOT NULL, entry_json JSON NOT NULL);

INSERT INTO tmp_po_hist SELECT 0, JSON_OBJECT(
    'level', 0, 'departmentId', @submitter_dept_id,
    'departmentName', COALESCE((SELECT txt_department_name FROM hr_tbl_department WHERE ser_department_id = @submitter_dept_id LIMIT 1), 'Initiator HOD'),
    'approvedBy', COALESCE(@hod_user_id, @submitter_user_id),
    'approverName', COALESCE((SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @hod_user_id LIMIT 1), 'Initiator HOD'),
    'approvedDate', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'action', 'APPROVED',
    'role', COALESCE((SELECT txt_department_name FROM hr_tbl_department WHERE ser_department_id = @submitter_dept_id LIMIT 1), 'Initiator HOD'),
    'approvedVia', 'SYSTEM', 'approvedIp', '127.0.0.1'
) WHERE @form_id IS NOT NULL;

INSERT INTO tmp_po_hist
SELECT COALESCE(jt.pipeline_order, jt.row_idx), JSON_OBJECT(
    'level', COALESCE(jt.pipeline_order, jt.row_idx), 'departmentId', jt.dept_id,
    'departmentName', COALESCE(jt.dept_name, CONCAT('Department ', jt.dept_id)),
    'approvedBy', COALESCE(CAST(TRIM(SUBSTRING_INDEX(d.ser_department_head_id, ',', 1)) AS UNSIGNED), jt.individual_user_id, @submitter_user_id),
    'approverName', COALESCE((SELECT u.txt_user_name FROM cfg_tbl_user u WHERE u.ser_user_id = CAST(TRIM(SUBSTRING_INDEX(d.ser_department_head_id, ',', 1)) AS UNSIGNED) LIMIT 1), jt.individual_user_name, 'Pipeline Approver'),
    'approvedDate', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL jt.row_idx MINUTE), '%Y-%m-%d %H:%i:%s'),
    'action', 'APPROVED', 'role', COALESCE(jt.dept_name, jt.individual_user_name, 'Approver'),
    'approvedVia', 'SYSTEM', 'approvedIp', '127.0.0.1'
)
FROM cfg_tbl_custom_form f
JOIN JSON_TABLE(COALESCE(f.txt_approval_pipeline, JSON_ARRAY()), '$[*]' COLUMNS (
    row_idx FOR ORDINALITY, dept_id INT PATH '$.serDepartmentId', dept_name VARCHAR(255) PATH '$.txtDepartmentName',
    individual_user_id INT PATH '$.serUserId', individual_user_name VARCHAR(255) PATH '$.txtUserName',
    pipeline_order INT PATH '$.intApprovalOrder'
)) jt ON 1=1
LEFT JOIN hr_tbl_department d ON d.ser_department_id = jt.dept_id
WHERE f.ser_form_id = @form_id;

INSERT INTO tmp_po_hist SELECT @pipeline_size + 10, JSON_OBJECT(
    'level', -99, 'departmentName', 'CEO', 'approvedBy', @ceo_user_id,
    'approverName', (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
    'approvedDate', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL (@pipeline_size+1) MINUTE), '%Y-%m-%d %H:%i:%s'),
    'action', 'APPROVED', 'role', 'CEO', 'approvedVia', 'SYSTEM', 'approvedIp', '127.0.0.1'
) WHERE @form_id IS NOT NULL AND @ceo_user_id IS NOT NULL;

INSERT INTO tmp_po_hist SELECT @pipeline_size + 20, JSON_OBJECT(
    'level', 999, 'departmentName', 'Finance', 'approvedBy', @finance_user_id,
    'approverName', (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @finance_user_id LIMIT 1),
    'approvedDate', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL (@pipeline_size+2) MINUTE), '%Y-%m-%d %H:%i:%s'),
    'action', 'ASSET_CODE_ASSIGNED', 'role', 'FINANCE', 'approvedVia', 'SYSTEM', 'approvedIp', '127.0.0.1'
) WHERE @form_id IS NOT NULL AND @finance_user_id IS NOT NULL;

INSERT INTO tmp_po_hist SELECT @pipeline_size + 30, JSON_OBJECT(
    'level', @post_pipeline_level, 'departmentName', 'Initiator', 'approvedBy', @submitter_user_id,
    'approverName', (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id LIMIT 1),
    'approvedDate', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL (@pipeline_size+3) MINUTE), '%Y-%m-%d %H:%i:%s'),
    'action', 'PR_CODE_ASSIGNED', 'role', 'INITIATOR', 'approvedVia', 'SYSTEM', 'approvedIp', '127.0.0.1'
) WHERE @form_id IS NOT NULL AND @submitter_user_id IS NOT NULL;

SET @approval_history = (
    SELECT COALESCE(JSON_ARRAYAGG(entry_json), JSON_ARRAY())
    FROM (SELECT entry_json FROM tmp_po_hist ORDER BY sort_order) t
);

-- ---------------------------------------------------------------------------
-- 5) Insert at PO_PENDING
-- ---------------------------------------------------------------------------
INSERT INTO cfg_tbl_custom_form_application (
    ser_form_id, txt_form_code, txt_application_data, txt_status,
    int_current_approval_level, ser_submitted_by, ser_current_approver,
    txt_remarks, txt_approval_history, txt_asset_code, txt_pr_code, txt_po_code,
    bl_is_active, bl_is_deleted, bln_status,
    dte_created_date, dte_modified_date, ser_created_user, ser_modified_user
)
SELECT @form_id, @test_form_code, @app_data, 'PO_PENDING', @post_pipeline_level,
    @submitter_user_id, NULL, 'Seeded for PO testing', @approval_history,
    @test_asset_code, @test_pr_code, NULL,
    1, 0, 1, NOW(), NOW(), @submitter_user_id, @submitter_user_id
WHERE @form_id IS NOT NULL AND @submitter_user_id IS NOT NULL
  AND @ceo_user_id IS NOT NULL AND @finance_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM cfg_tbl_custom_form_application a
      WHERE a.txt_form_code COLLATE utf8mb4_unicode_ci = @test_form_code COLLATE utf8mb4_unicode_ci
        AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
  );

SET @rows_inserted = ROW_COUNT();

SET @new_app_id = (
    SELECT ser_application_id
    FROM cfg_tbl_custom_form_application
    WHERE txt_form_code COLLATE utf8mb4_unicode_ci = @test_form_code COLLATE utf8mb4_unicode_ci
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY ser_application_id DESC
    LIMIT 1
);

-- ---------------------------------------------------------------------------
-- 6) Result (only the row created by THIS run)
-- ---------------------------------------------------------------------------
SELECT
    @rows_inserted AS rows_inserted,
    @new_app_id AS ser_application_id,
    @test_form_code AS txt_form_code,
    a.txt_status,
    a.txt_asset_code,
    a.txt_pr_code,
    a.txt_po_code,
    JSON_LENGTH(COALESCE(a.txt_approval_history, '[]')) AS history_entries,
    CASE
        WHEN @rows_inserted = 0 AND @new_app_id IS NULL THEN 'FAILED — insert skipped (see preflight_status above)'
        WHEN @rows_inserted = 0 AND a.txt_status <> 'PO_PENDING' THEN CONCAT('FAILED — code ', @test_form_code, ' already exists as ', a.txt_status)
        WHEN a.txt_status = 'PO_PENDING' THEN 'SUCCESS — log in as PO_Approver, Pending Approvals, assign PO code'
        ELSE CONCAT('FAILED — unexpected status: ', IFNULL(a.txt_status, 'NULL'))
    END AS result
FROM cfg_tbl_custom_form_application a
WHERE a.ser_application_id = @new_app_id;

SELECT CASE
    WHEN @new_app_id IS NULL THEN 'No new application created.'
    ELSE CONCAT('Created app id=', @new_app_id, ' code=', @test_form_code, ' — NOT the old CAPF-TEST-CEO-* apps')
END AS note;

DROP TEMPORARY TABLE IF EXISTS tmp_po_hist;
SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
