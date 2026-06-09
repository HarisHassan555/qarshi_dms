-- =============================================================================
-- Seed a CAPF application ready for CEO approval (CEO_PENDING)
--
-- Use this to continue testing: CEO -> Finance (asset code) -> PR -> PO -> Complete
--
-- What it does:
--   1) Finds the main CAPF form (CAPF-0001 / CAPF Form / CAPF Form Hattar)
--   2) Builds synthetic approval history for:
--        - Level 0  : Initiator department HOD
--        - Levels 1+: Each row in txt_approval_pipeline (department stages)
--      (No CEO / Finance / PR / PO entries yet)
--   3) Inserts a new application at CEO_PENDING
--
-- Run (adjust host/port/user/db):
--   mysql -h 127.0.0.1 -P 3308 -u root -p velocity_workbench < seed_capf_application_at_ceo_pending.sql
--
-- Optional overrides before running (uncomment and set):
--   SET @submitter_user_id = 145;
--   SET @ceo_user_id       = 401;
--   SET @test_form_code    = CONVERT('CAPF-TEST-CEO-001' USING utf8mb4) COLLATE utf8mb4_unicode_ci;
-- =============================================================================

USE velocity_workbench;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- ---------------------------------------------------------------------------
-- 1) Resolve CAPF form
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

SET @form_name = (SELECT txt_form_name FROM cfg_tbl_custom_form WHERE ser_form_id = @form_id LIMIT 1);
SET @pipeline_json = (SELECT txt_approval_pipeline FROM cfg_tbl_custom_form WHERE ser_form_id = @form_id LIMIT 1);
SET @pipeline_size = IFNULL(JSON_LENGTH(@pipeline_json), 0);

SELECT
    @form_id AS form_id,
    @form_name AS form_name,
    @pipeline_size AS pipeline_stage_count,
    CASE
        WHEN @form_id IS NULL THEN 'STOP - CAPF form not found'
        WHEN @pipeline_size = 0 THEN 'WARNING - pipeline empty; history will only include initiator HOD'
        ELSE 'OK'
    END AS form_status;

-- ---------------------------------------------------------------------------
-- 2) Resolve users (submitter + CEO)
-- ---------------------------------------------------------------------------
SET @submitter_user_id = IFNULL(@submitter_user_id, (
    SELECT u.ser_user_id
    FROM cfg_tbl_user u
    WHERE (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
      AND (u.bl_is_active = 1 OR u.bl_is_active IS NULL)
      AND u.txt_address IS NOT NULL
      AND TRIM(u.txt_address) <> ''
    ORDER BY u.ser_user_id
    LIMIT 1
));

SET @ceo_user_id = IFNULL(@ceo_user_id, (
    SELECT u.ser_user_id
    FROM cfg_tbl_user u
    JOIN cfg_tbl_role r ON r.ser_role_id = u.ser_role_id
    WHERE UPPER(TRIM(r.txt_role_name)) = 'CEO'
      AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
    ORDER BY u.ser_user_id
    LIMIT 1
));

SET @submitter_dept_id = (
    SELECT ser_department_id
    FROM cfg_tbl_user
    WHERE ser_user_id = @submitter_user_id
    LIMIT 1
);

SET @hod_user_id = (
    SELECT CAST(TRIM(SUBSTRING_INDEX(d.ser_department_head_id, ',', 1)) AS UNSIGNED)
    FROM hr_tbl_department d
    WHERE d.ser_department_id = @submitter_dept_id
      AND d.ser_department_head_id IS NOT NULL
      AND TRIM(d.ser_department_head_id) <> ''
    LIMIT 1
);

SELECT
    @submitter_user_id AS submitter_user_id,
    (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id) AS submitter_name,
    @hod_user_id AS initiator_hod_user_id,
    @ceo_user_id AS ceo_user_id,
    (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id) AS ceo_name,
    CASE
        WHEN @form_id IS NULL THEN 'STOP - no form'
        WHEN @submitter_user_id IS NULL THEN 'STOP - no submitter user'
        WHEN @ceo_user_id IS NULL THEN 'STOP - no CEO role user (CEO_PENDING requires CEO)'
        ELSE 'OK'
    END AS user_status;

-- ---------------------------------------------------------------------------
-- 3) Application code + minimal form data
-- ---------------------------------------------------------------------------
SET @test_form_code = (
    CONVERT(
        IFNULL(@test_form_code, CONCAT('CAPF-TEST-CEO-', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'))),
        CHAR(50) CHARACTER SET utf8mb4
    ) COLLATE utf8mb4_unicode_ci
);

SET @template_app_data = (
    SELECT a.txt_application_data
    FROM cfg_tbl_custom_form_application a
    WHERE a.ser_form_id = @form_id
      AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
      AND a.txt_application_data IS NOT NULL
    ORDER BY a.dte_created_date DESC
    LIMIT 1
);

SET @app_data = IFNULL(
    @template_app_data,
    JSON_OBJECT(
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
    )
);

-- Stamp ceo_user into app data when possible (helps CAPF preview/email)
SET @app_data = IF(
    @ceo_user_id IS NULL,
    @app_data,
    JSON_SET(
        @app_data,
        '$.ceo_user',
        JSON_OBJECT(
            'serUserId', @ceo_user_id,
            'txtUserName', (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
            'txtAddress', (SELECT txt_address FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
            'txtRole', 'CEO'
        )
    )
);

-- ---------------------------------------------------------------------------
-- 4) Build approval history (HOD + all pipeline stages approved)
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_capf_seed_history;
CREATE TEMPORARY TABLE tmp_capf_seed_history (
    sort_order INT NOT NULL,
    entry_json JSON NOT NULL
);

INSERT INTO tmp_capf_seed_history (sort_order, entry_json)
SELECT
    0 AS sort_order,
    JSON_OBJECT(
        'level', 0,
        'departmentId', @submitter_dept_id,
        'departmentName', COALESCE(
            (SELECT txt_department_name FROM hr_tbl_department WHERE ser_department_id = @submitter_dept_id LIMIT 1),
            'Initiator HOD'
        ),
        'remarks', 'Seeded test approval - initiator HOD',
        'approvedBy', COALESCE(@hod_user_id, @submitter_user_id),
        'approverName', COALESCE(
            (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @hod_user_id LIMIT 1),
            (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id LIMIT 1),
            'Initiator HOD'
        ),
        'approvedDate', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'),
        'action', 'APPROVED',
        'role', COALESCE(
            (SELECT txt_department_name FROM hr_tbl_department WHERE ser_department_id = @submitter_dept_id LIMIT 1),
            'Initiator HOD'
        ),
        'approvedVia', 'SYSTEM',
        'approvedIp', '127.0.0.1'
    )
WHERE @form_id IS NOT NULL;

INSERT INTO tmp_capf_seed_history (sort_order, entry_json)
SELECT
    COALESCE(jt.pipeline_order, jt.row_idx) AS sort_order,
    JSON_OBJECT(
        'level', COALESCE(jt.pipeline_order, jt.row_idx),
        'departmentId', jt.dept_id,
        'departmentName', COALESCE(jt.dept_name, CONCAT('Department ', jt.dept_id)),
        'remarks', CONCAT('Seeded test approval - stage ', COALESCE(jt.pipeline_order, jt.row_idx)),
        'approvedBy', COALESCE(
            CAST(TRIM(SUBSTRING_INDEX(d.ser_department_head_id, ',', 1)) AS UNSIGNED),
            jt.individual_user_id,
            @submitter_user_id
        ),
        'approverName', COALESCE(
            (SELECT u.txt_user_name
             FROM cfg_tbl_user u
             WHERE u.ser_user_id = CAST(TRIM(SUBSTRING_INDEX(d.ser_department_head_id, ',', 1)) AS UNSIGNED)
             LIMIT 1),
            jt.individual_user_name,
            'Pipeline Approver'
        ),
        'approvedDate', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL jt.row_idx MINUTE), '%Y-%m-%d %H:%i:%s'),
        'action', 'APPROVED',
        'role', COALESCE(jt.dept_name, jt.individual_user_name, 'Approver'),
        'approvedVia', 'SYSTEM',
        'approvedIp', '127.0.0.1'
    )
FROM cfg_tbl_custom_form f
JOIN JSON_TABLE(
    COALESCE(f.txt_approval_pipeline, JSON_ARRAY()),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        stage_type VARCHAR(32) PATH '$.type',
        dept_id INT PATH '$.serDepartmentId',
        dept_name VARCHAR(255) PATH '$.txtDepartmentName',
        individual_user_id INT PATH '$.serUserId',
        individual_user_name VARCHAR(255) PATH '$.txtUserName',
        pipeline_order INT PATH '$.intApprovalOrder'
    )
) AS jt ON 1 = 1
LEFT JOIN hr_tbl_department d ON d.ser_department_id = jt.dept_id
WHERE f.ser_form_id = @form_id;

SET @approval_history = (
    SELECT COALESCE(JSON_ARRAYAGG(entry_json), JSON_ARRAY())
    FROM (
        SELECT entry_json
        FROM tmp_capf_seed_history
        ORDER BY sort_order
    ) AS ordered_history
);

SET @ceo_level = @pipeline_size + 1;

-- ---------------------------------------------------------------------------
-- 5) Insert application at CEO_PENDING
-- ---------------------------------------------------------------------------
INSERT INTO cfg_tbl_custom_form_application (
    ser_form_id,
    txt_form_code,
    txt_application_data,
    txt_status,
    int_current_approval_level,
    ser_submitted_by,
    ser_current_approver,
    txt_remarks,
    txt_approval_history,
    txt_asset_code,
    txt_pr_code,
    txt_po_code,
    bl_is_active,
    bl_is_deleted,
    bln_status,
    dte_created_date,
    dte_modified_date,
    ser_created_user,
    ser_modified_user
)
SELECT
    @form_id,
    CONVERT(@test_form_code USING utf8mb4) COLLATE utf8mb4_unicode_ci,
    @app_data,
    'CEO_PENDING',
    @ceo_level,
    @submitter_user_id,
    @ceo_user_id,
    'Seeded for CEO approval testing',
    @approval_history,
    NULL,
    NULL,
    NULL,
    1,
    0,
    1,
    NOW(),
    NOW(),
    @submitter_user_id,
    @submitter_user_id
WHERE @form_id IS NOT NULL
  AND @submitter_user_id IS NOT NULL
  AND @ceo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_custom_form_application a
      WHERE a.txt_form_code COLLATE utf8mb4_unicode_ci = @test_form_code COLLATE utf8mb4_unicode_ci
        AND (a.bl_is_deleted = 0 OR a.bl_is_deleted IS NULL)
  );

SET @new_app_id = (
    SELECT ser_application_id
    FROM cfg_tbl_custom_form_application
    WHERE txt_form_code COLLATE utf8mb4_unicode_ci = @test_form_code COLLATE utf8mb4_unicode_ci
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY ser_application_id DESC
    LIMIT 1
);

-- ---------------------------------------------------------------------------
-- 6) Verification
-- ---------------------------------------------------------------------------
SELECT
    @new_app_id AS ser_application_id,
    @test_form_code AS txt_form_code,
    CASE
        WHEN @form_id IS NULL THEN 'FAILED - CAPF form not found'
        WHEN @ceo_user_id IS NULL THEN 'FAILED - no CEO user'
        WHEN @new_app_id IS NULL THEN 'FAILED - insert skipped (duplicate code or missing prerequisites)'
        ELSE 'SUCCESS - application seeded at CEO_PENDING'
    END AS seed_status;

SELECT
    a.ser_application_id,
    a.txt_form_code,
    f.txt_form_name,
    a.txt_status,
    a.int_current_approval_level,
    a.ser_submitted_by,
    submitter.txt_user_name AS submitter_name,
    a.ser_current_approver,
    ceo.txt_user_name AS current_approver_name,
    ceo_role.txt_role_name AS current_approver_role,
    a.txt_asset_code,
    a.txt_pr_code,
    a.txt_po_code,
    JSON_LENGTH(COALESCE(a.txt_approval_history, JSON_ARRAY())) AS history_entry_count,
    a.dte_created_date
FROM cfg_tbl_custom_form_application a
JOIN cfg_tbl_custom_form f ON f.ser_form_id = a.ser_form_id
LEFT JOIN cfg_tbl_user submitter ON submitter.ser_user_id = a.ser_submitted_by
LEFT JOIN cfg_tbl_user ceo ON ceo.ser_user_id = a.ser_current_approver
LEFT JOIN cfg_tbl_role ceo_role ON ceo_role.ser_role_id = ceo.ser_role_id
WHERE a.ser_application_id = @new_app_id;

SELECT
    jt.row_idx AS history_index,
    jt.entry_level AS level,
    jt.entry_role AS role,
    jt.dept_name AS department,
    jt.approver_name,
    jt.entry_action AS action,
    jt.approved_date
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_approval_history, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_level INT PATH '$.level',
        entry_role VARCHAR(100) PATH '$.role',
        dept_name VARCHAR(255) PATH '$.departmentName',
        approver_name VARCHAR(255) PATH '$.approverName',
        entry_action VARCHAR(100) PATH '$.action',
        approved_date VARCHAR(100) PATH '$.approvedDate'
    )
) AS jt
WHERE a.ser_application_id = @new_app_id
ORDER BY jt.row_idx;

SELECT CONCAT(
    'Next steps: log in as CEO (user ', @ceo_user_id, '), approve application ',
    @test_form_code, ' (id=', IFNULL(@new_app_id, 'N/A'), '), then test Finance -> PR -> PO flow.'
) AS next_steps;

DROP TEMPORARY TABLE IF EXISTS tmp_capf_seed_history;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
