-- =============================================================================
-- Delete ONLY the legacy CEO log entry (approverName = 'ceo_user')
-- for CAPF-0144 from txt_approval_history.
--
-- Safe behavior:
--   1) Builds a backup of current txt_approval_history into @history_before
--   2) Removes only rows matching:
--        role = 'CEO' AND approverName = 'ceo_user'
--   3) Rebuilds JSON array in original order
-- =============================================================================

USE velocity_workbench;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @form_code = 'CAPF-0144';

SET @app_id = (
    SELECT ser_application_id
    FROM cfg_tbl_custom_form_application
    WHERE txt_form_code COLLATE utf8mb4_unicode_ci = @form_code COLLATE utf8mb4_unicode_ci
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT
    @app_id AS application_id,
    CASE
        WHEN @app_id IS NULL THEN 'ERROR - Application not found'
        ELSE 'OK - Application found'
    END AS status;

-- Backup current history JSON in a session variable
SET @history_before = (
    SELECT txt_approval_history
    FROM cfg_tbl_custom_form_application
    WHERE ser_application_id = @app_id
);

-- Preview rows that will be removed
SELECT
    jt.row_idx AS history_index,
    jt.entry_role AS role,
    jt.approver_name,
    jt.entry_action AS action,
    jt.entry_level AS level,
    jt.approved_date
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_approval_history, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_role VARCHAR(100) PATH '$.role',
        approver_name VARCHAR(255) PATH '$.approverName',
        entry_action VARCHAR(100) PATH '$.action',
        entry_level INT PATH '$.level',
        approved_date VARCHAR(100) PATH '$.approvedDate'
    )
) jt
WHERE a.ser_application_id = @app_id
  AND UPPER(COALESCE(jt.entry_role, '')) = 'CEO'
  AND LOWER(COALESCE(jt.approver_name, '')) = 'ceo_user';

-- Remove only CEO entries for approverName = ceo_user
UPDATE cfg_tbl_custom_form_application a
SET
    a.txt_approval_history = (
        SELECT COALESCE(
            (
                SELECT JSON_ARRAYAGG(t.entry_json)
                FROM (
                    SELECT
                        jt.row_idx,
                        JSON_EXTRACT(@history_before, CONCAT('$[', jt.row_idx - 1, ']')) AS entry_json
                    FROM JSON_TABLE(
                        COALESCE(@history_before, '[]'),
                        '$[*]' COLUMNS (
                            row_idx FOR ORDINALITY,
                            entry_role VARCHAR(100) PATH '$.role',
                            approver_name VARCHAR(255) PATH '$.approverName'
                        )
                    ) jt
                    WHERE NOT (
                        UPPER(COALESCE(jt.entry_role, '')) = 'CEO'
                        AND LOWER(COALESCE(jt.approver_name, '')) = 'ceo_user'
                    )
                    ORDER BY jt.row_idx
                ) t
            ),
            JSON_ARRAY()
        )
    ),
    a.dte_modified_date = NOW(),
    a.ser_modified_user = 1
WHERE a.ser_application_id = @app_id
  AND @app_id IS NOT NULL;

SELECT
    CASE
        WHEN ROW_COUNT() > 0 THEN 'SUCCESS - Legacy ceo_user CEO log entry removed'
        ELSE 'WARNING - No row updated'
    END AS update_status;

-- Verification: show remaining CEO entries
SELECT
    jt.row_idx AS history_index,
    jt.entry_role AS role,
    jt.approver_name,
    jt.entry_action AS action,
    jt.entry_level AS level,
    jt.approved_date
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_approval_history, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_role VARCHAR(100) PATH '$.role',
        approver_name VARCHAR(255) PATH '$.approverName',
        entry_action VARCHAR(100) PATH '$.action',
        entry_level INT PATH '$.level',
        approved_date VARCHAR(100) PATH '$.approvedDate'
    )
) jt
WHERE a.ser_application_id = @app_id
  AND UPPER(COALESCE(jt.entry_role, '')) = 'CEO'
ORDER BY jt.row_idx;

-- =============================================================================
-- STEP B: The UI "Prior Approvals" section is typically driven by txt_prior_approvals,
--         not txt_approval_history. If you still see ceo_user, remove it here too.
-- =============================================================================

-- Preview prior approvals rows that will be removed
SELECT
    jt.row_idx AS prior_index,
    jt.entry_role AS role,
    jt.approver_name,
    jt.approved_by,
    jt.entry_action AS action,
    jt.entry_level AS level,
    jt.approved_date
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_prior_approvals, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_role VARCHAR(100) PATH '$.role',
        approver_name VARCHAR(255) PATH '$.approverName',
        approved_by INT PATH '$.approvedBy',
        entry_action VARCHAR(100) PATH '$.action',
        entry_level INT PATH '$.level',
        approved_date VARCHAR(100) PATH '$.approvedDate'
    )
) jt
WHERE a.ser_application_id = @app_id
  AND UPPER(COALESCE(jt.entry_role, '')) = 'CEO'
  AND LOWER(COALESCE(jt.approver_name, '')) = 'ceo_user'
ORDER BY jt.row_idx;

-- Backup prior approvals JSON
SET @prior_before = (
    SELECT txt_prior_approvals
    FROM cfg_tbl_custom_form_application
    WHERE ser_application_id = @app_id
);

-- Remove only CEO entries for approverName = ceo_user from txt_prior_approvals
UPDATE cfg_tbl_custom_form_application a
SET
    a.txt_prior_approvals = COALESCE(
        (
            SELECT JSON_ARRAYAGG(t.entry_json)
            FROM (
                SELECT
                    jt.row_idx,
                    JSON_EXTRACT(@prior_before, CONCAT('$[', jt.row_idx - 1, ']')) AS entry_json
                FROM JSON_TABLE(
                    COALESCE(@prior_before, '[]'),
                    '$[*]' COLUMNS (
                        row_idx FOR ORDINALITY,
                        entry_role VARCHAR(100) PATH '$.role',
                        approver_name VARCHAR(255) PATH '$.approverName'
                    )
                ) jt
                WHERE NOT (
                    UPPER(COALESCE(jt.entry_role, '')) = 'CEO'
                    AND LOWER(COALESCE(jt.approver_name, '')) = 'ceo_user'
                )
                ORDER BY jt.row_idx
            ) t
        ),
        JSON_ARRAY()
    ),
    a.dte_modified_date = NOW(),
    a.ser_modified_user = 1
WHERE a.ser_application_id = @app_id
  AND @app_id IS NOT NULL;

SELECT
    CASE
        WHEN ROW_COUNT() > 0 THEN 'SUCCESS - Legacy ceo_user CEO entry removed from txt_prior_approvals (if present)'
        ELSE 'WARNING - No row updated'
    END AS prior_update_status;

-- Verification: show remaining CEO entries in prior approvals
SELECT
    jt.row_idx AS prior_index,
    jt.entry_role AS role,
    jt.approver_name,
    jt.approved_by,
    jt.entry_action AS action,
    jt.entry_level AS level,
    jt.approved_date
FROM cfg_tbl_custom_form_application a
JOIN JSON_TABLE(
    COALESCE(a.txt_prior_approvals, '[]'),
    '$[*]' COLUMNS (
        row_idx FOR ORDINALITY,
        entry_role VARCHAR(100) PATH '$.role',
        approver_name VARCHAR(255) PATH '$.approverName',
        approved_by INT PATH '$.approvedBy',
        entry_action VARCHAR(100) PATH '$.action',
        entry_level INT PATH '$.level',
        approved_date VARCHAR(100) PATH '$.approvedDate'
    )
) jt
WHERE a.ser_application_id = @app_id
  AND UPPER(COALESCE(jt.entry_role, '')) = 'CEO'
ORDER BY jt.row_idx;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
