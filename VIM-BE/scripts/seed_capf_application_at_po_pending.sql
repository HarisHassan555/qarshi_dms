-- =============================================================================
-- Seed everything needed to test the PO code step directly (PO_PENDING).
--
-- Workflow position after this script:
--   Initiator HOD + pipeline + CEO + Finance (asset) + PR already done
--   -> waiting for PO_Approver to assign PO code -> APPROVED
--
-- Also ensures:
--   - PO_Approver role exists
--   - A login user with PO_Approver role (creates po_approver / 123 if missing)
--   - PO_Approver menu permissions (so login + Pending Approvals work)
--
-- Prerequisite (run once if column missing):
--   add_po_code_to_application.sql
--
-- Run:
--   mysql -h 127.0.0.1 -P 3308 -u root -p velocity_workbench < seed_capf_application_at_po_pending.sql
--
-- Optional overrides before running:
--   SET @submitter_user_id   = 145;
--   SET @ceo_user_id         = 401;
--   SET @finance_user_id     = 210;
--   SET @po_approver_user_id = 350;
--   SET @test_form_code      = CONVERT('CAPF-TEST-PO-001' USING utf8mb4) COLLATE utf8mb4_unicode_ci;
--   SET @test_asset_code     = 'AST-TEST-PO-001';
--   SET @test_pr_code        = 'PR-TEST-PO-001';
-- =============================================================================

USE velocity_workbench;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @password_hash = '$2b$10$jx2bjar6ZLmcnrZyDlKa2OLBzZjMABAEXNWi.Zo52.QurLztCNj7K';

-- ---------------------------------------------------------------------------
-- A) Ensure PO_Approver role + test user + permissions
-- ---------------------------------------------------------------------------
SET @po_role_id = (
    SELECT ser_role_id
    FROM cfg_tbl_role
    WHERE UPPER(TRIM(txt_role_name)) = 'PO_APPROVER'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY ser_role_id
    LIMIT 1
);

SET @next_role_id = IFNULL((SELECT MAX(ser_role_id) + 1 FROM cfg_tbl_role), 1);

INSERT INTO cfg_tbl_role (
    ser_role_id,
    txt_role_name,
    txt_role_code,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    dte_created_date,
    ser_created_user
)
SELECT
    @next_role_id,
    'PO_Approver',
    'PO_Approver',
    1,
    1,
    0,
    NOW(),
    1
WHERE @po_role_id IS NULL;

SET @po_role_id = IFNULL(@po_role_id, @next_role_id);

SET @password_policy_id = (
    SELECT ser_password_policy_id
    FROM cfg_tbl_password_policy
    WHERE (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY ser_password_policy_id
    LIMIT 1
);

SET @po_approver_user_id = IFNULL(@po_approver_user_id, (
    SELECT u.ser_user_id
    FROM cfg_tbl_user u
    JOIN cfg_tbl_role r ON r.ser_role_id = u.ser_role_id
    WHERE UPPER(TRIM(r.txt_role_name)) = 'PO_APPROVER'
      AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
    ORDER BY u.ser_user_id
    LIMIT 1
));

SET @next_user_id = IFNULL((SELECT MAX(ser_user_id) + 1 FROM cfg_tbl_user), 1);

INSERT INTO cfg_tbl_user (
    ser_user_id,
    txt_user_name,
    txt_address,
    txt_password,
    ser_role_id,
    txt_role,
    ser_password_policy_id,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    bl_is_password_chang,
    num_attempt,
    dte_created_date,
    dte_expiry_date,
    ser_created_user,
    txt_department_name,
    txt_designation
)
SELECT
    @next_user_id,
    'po_approver',
    'po.approver@test.local',
    @password_hash,
    @po_role_id,
    'PO_Approver',
    @password_policy_id,
    1,
    1,
    0,
    0,
    0,
    NOW(),
    DATE_ADD(NOW(), INTERVAL 365 DAY),
    1,
    'Procurement',
    'PO Approver'
WHERE @po_role_id IS NOT NULL
  AND @po_approver_user_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM cfg_tbl_user u
      WHERE LOWER(TRIM(u.txt_user_name)) = 'po_approver'
  );

SET @po_approver_user_id = IFNULL(@po_approver_user_id, (
    SELECT ser_user_id FROM cfg_tbl_user
    WHERE LOWER(TRIM(txt_user_name)) = 'po_approver'
    LIMIT 1
));

UPDATE cfg_tbl_user
SET
    ser_role_id = @po_role_id,
    txt_role = 'PO_Approver',
    txt_password = @password_hash,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    num_attempt = 0,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE ser_user_id = @po_approver_user_id
  AND @po_role_id IS NOT NULL;

INSERT INTO cfg_tbl_user_role (
    ser_user_role_id,
    bl_is_active,
    bln_status,
    dte_created_date,
    ser_created_user,
    ser_role_id,
    ser_user_id
)
SELECT
    IFNULL((SELECT MAX(ser_user_role_id) FROM cfg_tbl_user_role), 0) + 1,
    1,
    1,
    NOW(),
    1,
    @po_role_id,
    @po_approver_user_id
WHERE @po_role_id IS NOT NULL
  AND @po_approver_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM cfg_tbl_user_role ur
      WHERE ur.ser_user_id = @po_approver_user_id
        AND ur.ser_role_id = @po_role_id
  );

INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id,
    ser_role_id,
    ser_user_id,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    bl_is_view,
    bl_is_add,
    bl_is_delete,
    bl_is_update,
    bl_is_approve,
    bl_is_enabled,
    bl_is_all,
    bl_is_create,
    bl_is_NewView,
    bl_is_NewUpdate,
    dte_created_date,
    ser_created_user
)
SELECT
    sm.ser_sub_menu_id,
    @po_role_id,
    NULL,
    1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, NOW(), 1
FROM cfg_tbl_sub_menu sm
WHERE @po_role_id IS NOT NULL
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
  AND (sm.bln_status = 1 OR sm.bln_status IS NULL)
  AND NOT EXISTS (
      SELECT 1 FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
        AND smr.ser_role_id = @po_role_id
        AND smr.ser_user_id IS NULL
  );

UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 1,
    bl_is_enabled = 1,
    bl_is_create = 1,
    bl_is_NewView = 1,
    bl_is_NewUpdate = 1,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE smr.ser_role_id = @po_role_id
  AND smr.ser_user_id IS NULL
  AND @po_role_id IS NOT NULL;

INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id,
    ser_role_id,
    ser_user_id,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    bl_is_view,
    bl_is_add,
    bl_is_delete,
    bl_is_update,
    bl_is_approve,
    bl_is_enabled,
    bl_is_all,
    bl_is_create,
    bl_is_NewView,
    bl_is_NewUpdate,
    dte_created_date,
    ser_created_user
)
SELECT
    sm.ser_sub_menu_id,
    @po_role_id,
    @po_approver_user_id,
    1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, NOW(), 1
FROM cfg_tbl_sub_menu sm
WHERE @po_role_id IS NOT NULL
  AND @po_approver_user_id IS NOT NULL
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
  AND (sm.bln_status = 1 OR sm.bln_status IS NULL)
  AND NOT EXISTS (
      SELECT 1 FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
        AND smr.ser_role_id = @po_role_id
        AND smr.ser_user_id = @po_approver_user_id
  );

SELECT
    @po_role_id AS po_role_id,
    @po_approver_user_id AS po_approver_user_id,
    (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @po_approver_user_id) AS po_approver_username,
    CASE
        WHEN @po_role_id IS NULL THEN 'STOP - PO_Approver role missing'
        WHEN @po_approver_user_id IS NULL THEN 'STOP - no PO_Approver user'
        ELSE 'OK - PO_Approver ready (login: po_approver / 123 if newly created)'
    END AS po_setup_status;

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
SET @post_pipeline_level = @pipeline_size + 1;

SELECT
    @form_id AS form_id,
    @form_name AS form_name,
    @pipeline_size AS pipeline_stage_count,
    @post_pipeline_level AS post_pipeline_level,
    CASE
        WHEN @form_id IS NULL THEN 'STOP - CAPF form not found'
        WHEN @pipeline_size = 0 THEN 'WARNING - pipeline empty; history will only include initiator HOD'
        ELSE 'OK'
    END AS form_status;

-- ---------------------------------------------------------------------------
-- 2) Resolve workflow users
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

SET @finance_user_id = IFNULL(@finance_user_id, (
    SELECT u.ser_user_id
    FROM cfg_tbl_user u
    JOIN cfg_tbl_role r ON r.ser_role_id = u.ser_role_id
    WHERE UPPER(TRIM(r.txt_role_name)) IN ('FINANCE_HEAD', 'FINANCE')
      AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
    ORDER BY CASE WHEN UPPER(TRIM(r.txt_role_name)) = 'FINANCE_HEAD' THEN 0 ELSE 1 END, u.ser_user_id
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

SET @test_asset_code = IFNULL(@test_asset_code, 'AST-TEST-PO-001');
SET @test_pr_code = IFNULL(@test_pr_code, 'PR-TEST-PO-001');

SELECT
    @submitter_user_id AS submitter_user_id,
    (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id) AS submitter_name,
    @hod_user_id AS initiator_hod_user_id,
    @ceo_user_id AS ceo_user_id,
    (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id) AS ceo_name,
    @finance_user_id AS finance_user_id,
    (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @finance_user_id) AS finance_name,
    CASE
        WHEN @form_id IS NULL THEN 'STOP - no form'
        WHEN @submitter_user_id IS NULL THEN 'STOP - no submitter user'
        WHEN @ceo_user_id IS NULL THEN 'STOP - no CEO user (needed for history)'
        WHEN @finance_user_id IS NULL THEN 'STOP - no Finance user (needed for history)'
        WHEN @po_approver_user_id IS NULL THEN 'STOP - no PO_Approver user'
        ELSE 'OK'
    END AS user_status;

-- ---------------------------------------------------------------------------
-- 3) Application code + form data
-- ---------------------------------------------------------------------------
SET @test_form_code = (
    CONVERT(
        IFNULL(@test_form_code, CONCAT('CAPF-TEST-PO-', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'))),
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
-- 4) Build approval history (HOD + pipeline + CEO + Finance + PR)
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_capf_po_seed_history;
CREATE TEMPORARY TABLE tmp_capf_po_seed_history (
    sort_order INT NOT NULL,
    entry_json JSON NOT NULL
);

INSERT INTO tmp_capf_po_seed_history (sort_order, entry_json)
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

INSERT INTO tmp_capf_po_seed_history (sort_order, entry_json)
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

-- CEO approved
INSERT INTO tmp_capf_po_seed_history (sort_order, entry_json)
SELECT
    @pipeline_size + 10,
    JSON_OBJECT(
        'level', -99,
        'departmentName', 'CEO',
        'remarks', 'Seeded test approval - CEO',
        'approvedBy', @ceo_user_id,
        'approverName', (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
        'designation', (SELECT txt_designation FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
        'txtDesignation', (SELECT txt_designation FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
        'txtDepartmentName', (SELECT txt_department_name FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
        'signaturePath', (SELECT txt_signature_path FROM cfg_tbl_user WHERE ser_user_id = @ceo_user_id LIMIT 1),
        'approvedDate', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL (@pipeline_size + 1) MINUTE), '%Y-%m-%d %H:%i:%s'),
        'action', 'APPROVED',
        'role', 'CEO',
        'approvedVia', 'SYSTEM',
        'approvedIp', '127.0.0.1'
    )
WHERE @form_id IS NOT NULL
  AND @ceo_user_id IS NOT NULL;

-- Finance asset code assigned
INSERT INTO tmp_capf_po_seed_history (sort_order, entry_json)
SELECT
    @pipeline_size + 20,
    JSON_OBJECT(
        'level', 999,
        'departmentName', 'Finance',
        'remarks', CONCAT('Seeded asset code: ', @test_asset_code),
        'approvedBy', @finance_user_id,
        'approverName', (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @finance_user_id LIMIT 1),
        'designation', (SELECT txt_designation FROM cfg_tbl_user WHERE ser_user_id = @finance_user_id LIMIT 1),
        'txtDesignation', (SELECT txt_designation FROM cfg_tbl_user WHERE ser_user_id = @finance_user_id LIMIT 1),
        'txtDepartmentName', (SELECT txt_department_name FROM cfg_tbl_user WHERE ser_user_id = @finance_user_id LIMIT 1),
        'signaturePath', (SELECT txt_signature_path FROM cfg_tbl_user WHERE ser_user_id = @finance_user_id LIMIT 1),
        'approvedDate', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL (@pipeline_size + 2) MINUTE), '%Y-%m-%d %H:%i:%s'),
        'action', 'ASSET_CODE_ASSIGNED',
        'role', 'FINANCE',
        'approvedVia', 'SYSTEM',
        'approvedIp', '127.0.0.1'
    )
WHERE @form_id IS NOT NULL
  AND @finance_user_id IS NOT NULL;

-- Initiator PR code assigned
INSERT INTO tmp_capf_po_seed_history (sort_order, entry_json)
SELECT
    @pipeline_size + 30,
    JSON_OBJECT(
        'level', @post_pipeline_level,
        'departmentName', 'Initiator',
        'remarks', CONCAT('Seeded PR code: ', @test_pr_code),
        'approvedBy', @submitter_user_id,
        'approverName', (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id LIMIT 1),
        'designation', (SELECT txt_designation FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id LIMIT 1),
        'txtDesignation', (SELECT txt_designation FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id LIMIT 1),
        'txtDepartmentName', (SELECT txt_department_name FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id LIMIT 1),
        'signaturePath', (SELECT txt_signature_path FROM cfg_tbl_user WHERE ser_user_id = @submitter_user_id LIMIT 1),
        'approvedDate', DATE_FORMAT(DATE_ADD(NOW(), INTERVAL (@pipeline_size + 3) MINUTE), '%Y-%m-%d %H:%i:%s'),
        'action', 'PR_CODE_ASSIGNED',
        'role', 'INITIATOR',
        'approvedVia', 'SYSTEM',
        'approvedIp', '127.0.0.1'
    )
WHERE @form_id IS NOT NULL
  AND @submitter_user_id IS NOT NULL;

SET @approval_history = (
    SELECT COALESCE(JSON_ARRAYAGG(entry_json), JSON_ARRAY())
    FROM (
        SELECT entry_json
        FROM tmp_capf_po_seed_history
        ORDER BY sort_order
    ) AS ordered_history
);

-- ---------------------------------------------------------------------------
-- 5) Insert application at PO_PENDING
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
    'PO_PENDING',
    @post_pipeline_level,
    @submitter_user_id,
    NULL,
    'Seeded for PO code testing',
    @approval_history,
    @test_asset_code,
    @test_pr_code,
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
  AND @finance_user_id IS NOT NULL
  AND @po_approver_user_id IS NOT NULL
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
    @test_asset_code AS txt_asset_code,
    @test_pr_code AS txt_pr_code,
    CASE
        WHEN @form_id IS NULL THEN 'FAILED - CAPF form not found'
        WHEN @po_approver_user_id IS NULL THEN 'FAILED - no PO_Approver user'
        WHEN @finance_user_id IS NULL THEN 'FAILED - no Finance user'
        WHEN @new_app_id IS NULL THEN 'FAILED - insert skipped (duplicate code or missing prerequisites)'
        ELSE 'SUCCESS - application seeded at PO_PENDING'
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
    a.txt_asset_code,
    a.txt_pr_code,
    a.txt_po_code,
    JSON_LENGTH(COALESCE(a.txt_approval_history, JSON_ARRAY())) AS history_entry_count,
    a.dte_created_date
FROM cfg_tbl_custom_form_application a
JOIN cfg_tbl_custom_form f ON f.ser_form_id = a.ser_form_id
LEFT JOIN cfg_tbl_user submitter ON submitter.ser_user_id = a.ser_submitted_by
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
    'Next steps: 1) Log in as PO_Approver (',
    COALESCE((SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = @po_approver_user_id), 'po_approver'),
    ' / 123). 2) Open Pending Approvals. 3) Assign PO code on application ',
    @test_form_code,
    ' (id=', IFNULL(@new_app_id, 'N/A'), '). 4) Status should become APPROVED.'
) AS next_steps;

DROP TEMPORARY TABLE IF EXISTS tmp_capf_po_seed_history;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
