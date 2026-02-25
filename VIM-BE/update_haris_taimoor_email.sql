USE vim_3;

-- =====================================================
-- Script to update email addresses for users haris and taimoor
-- New email: harishassan551@gmail.com
-- =====================================================

-- Step 1: Find haris user ID
SET @haris_user_id = (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE txt_user_name = 'haris'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT 
    CASE
        WHEN @haris_user_id IS NOT NULL THEN CONCAT('Found haris user ID: ', @haris_user_id)
        ELSE 'ERROR - haris user not found'
    END AS 'Haris User Status';

-- Step 2: Find taimoor user ID
SET @taimoor_user_id = (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE txt_user_name = 'taimoor'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT 
    CASE
        WHEN @taimoor_user_id IS NOT NULL THEN CONCAT('Found taimoor user ID: ', @taimoor_user_id)
        ELSE 'ERROR - taimoor user not found'
    END AS 'Taimoor User Status';

-- Step 3: Update haris email
UPDATE cfg_tbl_user
SET
    txt_address = 'harishassan551@gmail.com',
    dte_modified_date = NOW(),
    ser_modified_user = 1  -- Modified by user ID 1 (CHANGE THIS IF DIFFERENT)
WHERE ser_user_id = @haris_user_id
  AND @haris_user_id IS NOT NULL;

SELECT 
    CASE
        WHEN @haris_user_id IS NOT NULL AND ROW_COUNT() > 0 THEN 'User "haris" email updated successfully to harishassan551@gmail.com'
        WHEN @haris_user_id IS NOT NULL AND ROW_COUNT() = 0 THEN 'User "haris" found but email update failed'
        ELSE 'ERROR - haris user not found, cannot update email'
    END AS 'Haris Update Status';

-- Step 4: Update taimoor email
UPDATE cfg_tbl_user
SET
    txt_address = 'harishassan551@gmail.com',
    dte_modified_date = NOW(),
    ser_modified_user = 1  -- Modified by user ID 1 (CHANGE THIS IF DIFFERENT)
WHERE ser_user_id = @taimoor_user_id
  AND @taimoor_user_id IS NOT NULL;

SELECT 
    CASE
        WHEN @taimoor_user_id IS NOT NULL AND ROW_COUNT() > 0 THEN 'User "taimoor" email updated successfully to harishassan551@gmail.com'
        WHEN @taimoor_user_id IS NOT NULL AND ROW_COUNT() = 0 THEN 'User "taimoor" found but email update failed'
        ELSE 'ERROR - taimoor user not found, cannot update email'
    END AS 'Taimoor Update Status';

-- =====================================================
-- Step 5: Verification
-- =====================================================
SELECT 
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_address AS 'Email Address',
    dte_modified_date AS 'Last Modified'
FROM cfg_tbl_user
WHERE txt_user_name IN ('haris', 'taimoor')
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
ORDER BY txt_user_name;

-- =====================================================
-- Summary
-- =====================================================
SELECT 
    CONCAT(
        'Email update completed. ',
        CASE 
            WHEN @haris_user_id IS NOT NULL AND @taimoor_user_id IS NOT NULL 
            THEN 'Both users (haris and taimoor) email addresses updated to harishassan551@gmail.com'
            WHEN @haris_user_id IS NOT NULL 
            THEN 'haris email updated, but taimoor user not found'
            WHEN @taimoor_user_id IS NOT NULL 
            THEN 'taimoor email updated, but haris user not found'
            ELSE 'ERROR - Neither user found'
        END
    ) AS 'Summary';






