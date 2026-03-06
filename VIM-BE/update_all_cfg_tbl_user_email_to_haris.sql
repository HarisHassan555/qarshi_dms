USE vim_3;

-- =====================================================
-- Script to update all user emails in cfg_tbl_user
-- New email: harishassan551@gmail.com
-- =====================================================

UPDATE cfg_tbl_user
SET txt_address = 'harishassan551@gmail.com'
WHERE ser_user_id >= 0
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL);

SET @updated_rows = ROW_COUNT();

SELECT
    CASE
        WHEN @updated_rows > 0 THEN CONCAT('SUCCESS - Updated ', @updated_rows, ' user(s)')
        ELSE 'WARNING - No user rows updated'
    END AS 'Update Status';

-- Verification
SELECT
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_address AS 'Email'
FROM cfg_tbl_user
WHERE (bl_is_deleted = false OR bl_is_deleted IS NULL)
ORDER BY ser_user_id
LIMIT 200;
