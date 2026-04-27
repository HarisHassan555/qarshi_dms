USE vim_3;

-- ==========================================================
-- Script to update selected users email in cfg_tbl_user
-- New email: haris.hassan@datasphereglobal.co
-- Users: user1, user2, user3, user4, user5, user6
-- ==========================================================

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE cfg_tbl_user
SET txt_address = 'haris.hassan@datasphereglobal.co'
WHERE LOWER(TRIM(txt_user_name)) IN ('user1', 'user2', 'user3', 'user4', 'user5', 'user6')
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL);

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;

SET @updated_rows = ROW_COUNT();

SELECT
    CASE
        WHEN @updated_rows > 0 THEN CONCAT('SUCCESS - Updated ', @updated_rows, ' user(s)')
        ELSE 'WARNING - No matching active users found'
    END AS 'Update Status';

-- Verification
SELECT
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_address AS 'Email'
FROM cfg_tbl_user
WHERE LOWER(TRIM(txt_user_name)) IN ('user1', 'user2', 'user3', 'user4', 'user5', 'user6')
ORDER BY txt_user_name;
