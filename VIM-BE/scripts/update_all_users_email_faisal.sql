-- Email for application users: cfg_tbl_user.txt_address (not hr_tbl_employee.txt_email).
-- Sets all non-deleted users to faisal.s@qarshi.com
-- Change USE ... if your schema is not velocity (e.g. velocity_workbench, vim_3).

USE velocity;

SET @new_email = 'faisal.s@qarshi.com';

START TRANSACTION;

SELECT
    COUNT(*) AS user_count_before,
    COUNT(DISTINCT txt_address) AS distinct_emails_before
FROM cfg_tbl_user
WHERE bl_is_deleted = 0 OR bl_is_deleted IS NULL;

-- Workbench safe-update (1175): WHERE must use a key column — ser_user_id is the PK.
UPDATE cfg_tbl_user
SET
    txt_address = @new_email,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE ser_user_id >= 0
  AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL);

SELECT ROW_COUNT() AS users_updated;

SELECT
    COUNT(*) AS user_count_after,
    COUNT(DISTINCT txt_address) AS distinct_emails_after,
    MIN(txt_address) AS sample_email
FROM cfg_tbl_user
WHERE bl_is_deleted = 0 OR bl_is_deleted IS NULL;

SELECT
    ser_user_id,
    txt_user_name,
    txt_login_name,
    txt_address AS email,
    dte_modified_date
FROM cfg_tbl_user
WHERE bl_is_deleted = 0 OR bl_is_deleted IS NULL
ORDER BY ser_user_id;

COMMIT;
