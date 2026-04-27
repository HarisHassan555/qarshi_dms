USE vim_3;

-- ==========================================================
-- Script to update selected users password in cfg_tbl_user
-- Password: 123 (BCrypt hash)
-- Users: user1, user2, user3, user4, user5, user6, ceo_user
-- ==========================================================

-- BCrypt hash (cost=10) for plain password '123'
SET @new_password_hash = '$2b$10$jx2bjar6ZLmcnrZyDlKa2OLBzZjMABAEXNWi.Zo52.QurLztCNj7K';

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE cfg_tbl_user
SET
    txt_password = @new_password_hash,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE LOWER(TRIM(txt_user_name)) IN ('user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'ceo_user')
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
  AND @new_password_hash IS NOT NULL;

SET @updated_rows = ROW_COUNT();
SET SQL_SAFE_UPDATES = @old_sql_safe_updates;

SELECT
    CASE
        WHEN @new_password_hash IS NULL THEN 'ERROR - Password hash missing'
        WHEN @updated_rows > 0 THEN CONCAT('SUCCESS - Updated password for ', @updated_rows, ' user(s)')
        ELSE 'WARNING - No matching active users found'
    END AS 'Update Status';

-- Verification
SELECT
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    LEFT(txt_password, 20) AS 'Password Hash (First 20 chars)',
    dte_modified_date AS 'Last Modified'
FROM cfg_tbl_user
WHERE LOWER(TRIM(txt_user_name)) IN ('user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'ceo_user')
ORDER BY txt_user_name;
