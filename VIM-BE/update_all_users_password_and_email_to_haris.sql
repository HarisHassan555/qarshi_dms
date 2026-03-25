USE vim_3;

-- =====================================================
-- Script to update ALL users:
-- - Set password to match user "haris"
-- - Set email (txt_address) to harishassan551@gmail.com
-- =====================================================

-- Step 1: Set BCrypt hash for password '123'
-- BCrypt hash (cost=10) for '123'
SET @haris_password = '$2b$10$jx2bjar6ZLmcnrZyDlKa2OLBzZjMABAEXNWi.Zo52.QurLztCNj7K';

SELECT
    CASE
        WHEN @haris_password IS NOT NULL THEN CONCAT('Using BCrypt hash: ', LEFT(@haris_password, 20), '...')
        ELSE 'ERROR - BCrypt hash missing'
    END AS 'Haris Password Status';

-- Step 2: Update all users (excluding deleted)
UPDATE cfg_tbl_user
SET
    txt_password = @haris_password,
    txt_address = 'harishassan551@gmail.com',
    dte_modified_date = NOW(),
    ser_modified_user = 1  -- Modified by user ID 1 (CHANGE THIS IF DIFFERENT)
WHERE ser_user_id >= 0
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
  AND @haris_password IS NOT NULL;

SELECT
    CASE
        WHEN @haris_password IS NULL THEN 'ERROR - No updates applied (haris password missing)'
        WHEN ROW_COUNT() > 0 THEN CONCAT('SUCCESS - Updated ', ROW_COUNT(), ' user(s)')
        ELSE 'WARNING - No rows updated (no eligible users or already updated)'
    END AS 'Update Status';

-- Step 3: Verification (spot-check)
SELECT
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_address AS 'Email',
    LEFT(txt_password, 20) AS 'Password (First 20 chars)',
    dte_modified_date AS 'Last Modified'
FROM cfg_tbl_user
WHERE (bl_is_deleted = false OR bl_is_deleted IS NULL)
ORDER BY ser_user_id
LIMIT 50;
