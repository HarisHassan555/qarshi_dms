-- =============================================================================
-- Replace CEO user in CAPF-0144 with Amjad Aqeel (ser_user_id = 401)
--
-- New CEO details:
--   ser_user_id     : 401
--   txt_full_name   : Amjad Aqeel
--   txt_address     : amjad.aqeel@qarshi.com
--   txt_role        : CEO
--   txt_designation : CHIEF QRI +CQAU
--   txt_department  : CEO/COO QIL + DB
--   signature_path  : signatures/signature_401_a03055a9-9ae8-4192-9218-c2538a2fa60d.png
--
-- Run against: velocity_workbench
-- =============================================================================

USE velocity_workbench;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @form_code = 'CAPF-0144';

-- New CEO values
SET @new_ceo_id          = 401;
SET @new_ceo_name        = 'Amjad Aqeel';
SET @new_ceo_email       = 'amjad.aqeel@qarshi.com';
SET @new_ceo_signature   = 'signatures/signature_401_a03055a9-9ae8-4192-9218-c2538a2fa60d.png';
SET @new_ceo_designation = 'CHIEF QRI +CQAU';
SET @new_ceo_dept        = 'CEO/COO QIL + DB';

-- Resolve application ID
SET @app_id = (
    SELECT ser_application_id
    FROM cfg_tbl_custom_form_application
    WHERE txt_form_code COLLATE utf8mb4_unicode_ci = @form_code COLLATE utf8mb4_unicode_ci
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT
    @app_id AS 'Application ID',
    CASE
        WHEN @app_id IS NULL THEN 'ERROR - CAPF-0144 not found'
        ELSE 'OK - Application found'
    END AS 'Status';

-- =============================================================================
-- STEP 1: Snapshot current state (run first to verify before updating)
-- =============================================================================
SELECT
    ser_application_id,
    txt_form_code,
    txt_status,
    int_current_approval_level,
    ser_current_approver,
    (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = a.ser_current_approver) AS current_approver_name,
    txt_approval_history
FROM cfg_tbl_custom_form_application a
WHERE ser_application_id = @app_id;

-- =============================================================================
-- STEP 2: Update ser_current_approver → Amjad Aqeel
--         Only needed when the application is currently CEO_PENDING
--         (the link in the pending email will then point to user 401)
-- =============================================================================
UPDATE cfg_tbl_custom_form_application
SET
    ser_current_approver = @new_ceo_id,
    dte_modified_date    = NOW(),
    ser_modified_user    = 1
WHERE ser_application_id = @app_id
  AND @app_id IS NOT NULL;
  -- Remove the line below if you want to update regardless of status:
  -- AND txt_status COLLATE utf8mb4_unicode_ci = 'CEO_PENDING' COLLATE utf8mb4_unicode_ci;

SELECT
    CASE
        WHEN ROW_COUNT() > 0 THEN CONCAT('SUCCESS - ser_current_approver set to ', @new_ceo_id, ' (Amjad Aqeel)')
        ELSE 'WARNING - ser_current_approver not updated (check app_id / status)'
    END AS 'Step 2 Result';

-- =============================================================================
-- STEP 3: Update the CEO entry in txt_approval_history JSON
--         CEO entries are identified by: level = -99  OR  role = 'CEO'
--
--         MySQL cannot directly patch array elements by condition, so we use
--         a stored-procedure-style approach:
--           a) Extract the full JSON array into @history
--           b) Use JSON_SEARCH to find the index of the CEO entry
--           c) Use JSON_SET to patch only the relevant fields at that index
--
--  NOTE: If there are multiple CEO entries (e.g. send-back + re-approval),
--        this patches only the FIRST one found with role = 'CEO'.
--        Run STEP 1 snapshot first to confirm there is exactly one CEO entry.
-- =============================================================================

-- 3a) Capture current history
SET @history = (
    SELECT txt_approval_history
    FROM cfg_tbl_custom_form_application
    WHERE ser_application_id = @app_id
);

-- 3b) Find the array index of the first CEO entry (role == 'CEO')
--     JSON_SEARCH returns the path like '$[2].role'; we strip it to get the index.
SET @ceo_role_path = JSON_UNQUOTE(
    JSON_SEARCH(@history, 'one', 'CEO', NULL, '$[*].role')
);
-- @ceo_role_path example: '$[3].role'
-- Extract the numeric index between '[' and ']'
SET @ceo_idx = IF(
    @ceo_role_path IS NOT NULL,
    CAST(SUBSTRING(@ceo_role_path, 3, LOCATE(']', @ceo_role_path) - 3) AS UNSIGNED),
    NULL
);

SELECT
    @ceo_role_path AS 'Found CEO role path',
    @ceo_idx       AS 'CEO array index',
    CASE
        WHEN @ceo_idx IS NULL THEN 'WARNING - No CEO entry found in txt_approval_history (application may still be pending, history not yet written)'
        ELSE CONCAT('OK - CEO entry is at index ', @ceo_idx)
    END AS 'Step 3 Status';

-- 3c) Patch the CEO entry fields (only runs if a CEO history entry exists)
UPDATE cfg_tbl_custom_form_application
SET txt_approval_history = JSON_SET(
    txt_approval_history,
    CONCAT('$[', @ceo_idx, '].approvedBy'),        @new_ceo_id,
    CONCAT('$[', @ceo_idx, '].approverName'),       @new_ceo_name,
    CONCAT('$[', @ceo_idx, '].signaturePath'),      @new_ceo_signature,
    CONCAT('$[', @ceo_idx, '].designation'),        @new_ceo_designation,
    CONCAT('$[', @ceo_idx, '].txtDepartmentName'),  @new_ceo_dept,
    CONCAT('$[', @ceo_idx, '].userDepartmentName'), @new_ceo_dept,
    CONCAT('$[', @ceo_idx, '].departmentName'),     'CEO'
),
dte_modified_date = NOW(),
ser_modified_user = 1
WHERE ser_application_id = @app_id
  AND @app_id IS NOT NULL
  AND @ceo_idx IS NOT NULL;

SELECT
    CASE
        WHEN @ceo_idx IS NULL THEN 'SKIPPED - No CEO history entry to patch (this is fine if status is still CEO_PENDING)'
        WHEN ROW_COUNT() > 0  THEN CONCAT('SUCCESS - CEO history entry at index ', @ceo_idx, ' updated to Amjad Aqeel')
        ELSE 'WARNING - History update ran but affected 0 rows'
    END AS 'Step 3 Result';

-- =============================================================================
-- STEP 4: Update ceo_user object inside txt_application_data JSON
--         The form stores the selected CEO as a nested object at $.ceo_user
--         We replace the whole sub-object with Amjad Aqeel's data.
-- =============================================================================
UPDATE cfg_tbl_custom_form_application
SET txt_application_data = JSON_SET(
    txt_application_data,
    '$.ceo_user',
    JSON_OBJECT(
        'serUserId',          @new_ceo_id,
        'txtUserName',        @new_ceo_name,
        'txtAddress',         @new_ceo_email,
        'txtRole',            'CEO',
        'txtDesignation',     @new_ceo_designation,
        'txtDepartmentName',  @new_ceo_dept,
        'txtSignaturePath',   @new_ceo_signature
    )
),
dte_modified_date = NOW(),
ser_modified_user = 1
WHERE ser_application_id = @app_id
  AND @app_id IS NOT NULL
  AND JSON_CONTAINS_PATH(txt_application_data, 'one', '$.ceo_user');

SELECT
    CASE
        WHEN ROW_COUNT() > 0 THEN 'SUCCESS - ceo_user in txt_application_data replaced with Amjad Aqeel'
        ELSE 'WARNING - $.ceo_user path not found in txt_application_data (check field name in your form schema)'
    END AS 'Step 4 Result';

-- =============================================================================
-- STEP 5: Verification — confirm all changes
-- =============================================================================
SELECT
    ser_application_id,
    txt_form_code,
    txt_status,
    int_current_approval_level,
    ser_current_approver,
    (SELECT txt_user_name FROM cfg_tbl_user WHERE ser_user_id = a.ser_current_approver) AS current_approver_name,
    JSON_EXTRACT(txt_application_data, '$.ceo_user.serUserId')       AS ceo_user_id_in_appdata,
    JSON_EXTRACT(txt_application_data, '$.ceo_user.txtUserName')     AS ceo_user_name_in_appdata,
    JSON_EXTRACT(txt_application_data, '$.ceo_user.txtDesignation')  AS ceo_designation_in_appdata
FROM cfg_tbl_custom_form_application a
WHERE ser_application_id = @app_id;

-- Show patched CEO history entry (if it existed)
SELECT
    jt.row_idx       AS history_index,
    jt.entry_level   AS level,
    jt.entry_role    AS role,
    jt.approved_by,
    jt.approver_name,
    jt.signature_path,
    jt.designation,
    jt.dept_name
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_approval_history, '[]'),
    '$[*]' COLUMNS (
        row_idx       FOR ORDINALITY,
        entry_level   INT          PATH '$.level',
        entry_role    VARCHAR(50)  PATH '$.role',
        approved_by   INT          PATH '$.approvedBy',
        approver_name VARCHAR(255) PATH '$.approverName',
        signature_path VARCHAR(512) PATH '$.signaturePath',
        designation   VARCHAR(255) PATH '$.designation',
        dept_name     VARCHAR(255) PATH '$.departmentName'
    )
) AS jt
WHERE a.ser_application_id = @app_id
  AND (jt.entry_level = -99 OR UPPER(COALESCE(jt.entry_role, '')) = 'CEO')
ORDER BY jt.row_idx;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
