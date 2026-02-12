USE vim_3;

-- =====================================================
-- Script to update dsg user password to match taimoor's password
-- =====================================================

-- Step 1: Get taimoor's password
SET @taimoor_password = (
    SELECT txt_password
    FROM cfg_tbl_user
    WHERE txt_user_name = 'taimoor'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT 
    CASE
        WHEN @taimoor_password IS NOT NULL THEN CONCAT('Found taimoor password: ', LEFT(@taimoor_password, 20), '...')
        ELSE 'ERROR - taimoor user not found'
    END AS 'Password Source';

-- Step 2: Find dsg user (try multiple variations)
SET @dsg_user_id = (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE (
        txt_user_name = 'dsg'
        OR txt_user_name = 'DSG'
        OR txt_user_name = 'Dsg'
        OR txt_user_name LIKE '%dsg%'
    )
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    ORDER BY 
        CASE 
            WHEN txt_user_name = 'dsg' THEN 1
            WHEN txt_user_name = 'DSG' THEN 2
            WHEN txt_user_name = 'Dsg' THEN 3
            ELSE 4
        END
    LIMIT 1
);

SET @dsg_username = (
    SELECT txt_user_name
    FROM cfg_tbl_user
    WHERE ser_user_id = @dsg_user_id
    LIMIT 1
);

SELECT 
    @dsg_user_id AS 'DSG User ID',
    @dsg_username AS 'DSG Username',
    CASE
        WHEN @dsg_user_id IS NULL THEN 'ERROR - dsg user not found'
        WHEN @taimoor_password IS NULL THEN 'ERROR - taimoor password not found'
        ELSE 'SUCCESS - Ready to update password'
    END AS 'Status';

-- Step 3: Update dsg password to match taimoor's
UPDATE cfg_tbl_user
SET
    txt_password = @taimoor_password,
    dte_modified_date = NOW(),
    ser_modified_user = 1  -- Modified by user ID 1
WHERE ser_user_id = @dsg_user_id
  AND @dsg_user_id IS NOT NULL
  AND @taimoor_password IS NOT NULL;

SELECT 
    CASE
        WHEN @dsg_user_id IS NOT NULL AND @taimoor_password IS NOT NULL AND ROW_COUNT() > 0 THEN 
            CONCAT('SUCCESS - dsg password updated! User: ', @dsg_username)
        WHEN @dsg_user_id IS NULL THEN 'ERROR - dsg user not found'
        WHEN @taimoor_password IS NULL THEN 'ERROR - taimoor password not found'
        ELSE 'WARNING - No rows updated (password may already match)'
    END AS 'Update Status';

-- =====================================================
-- Step 4: Verification
-- =====================================================
SELECT 'Verifying dsg user:' AS 'Verification';
SELECT 
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    LEFT(txt_password, 20) AS 'Password (First 20 chars)',
    CASE 
        WHEN txt_password = @taimoor_password THEN 'Password matches taimoor'
        ELSE 'Password does NOT match taimoor'
    END AS 'Password Match Status',
    txt_role AS 'Role',
    bl_is_active AS 'Is Active',
    bln_status AS 'Status',
    dte_modified_date AS 'Last Modified'
FROM cfg_tbl_user
WHERE ser_user_id = @dsg_user_id
  AND @dsg_user_id IS NOT NULL;

-- =====================================================
-- Step 5: List all potential dsg users (for reference)
-- =====================================================
SELECT 'All potential dsg users:' AS 'Reference';
SELECT 
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_role AS 'Role',
    bl_is_active AS 'Is Active',
    bln_status AS 'Status'
FROM cfg_tbl_user
WHERE (
    txt_user_name = 'dsg'
    OR txt_user_name = 'DSG'
    OR txt_user_name = 'Dsg'
    OR txt_user_name LIKE '%dsg%'
)
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
ORDER BY txt_user_name;

-- =====================================================
-- Step 6: Final Status
-- =====================================================
SELECT 
    'FINAL STATUS' AS 'Check',
    CASE 
        WHEN @dsg_user_id IS NOT NULL 
         AND @taimoor_password IS NOT NULL
         AND EXISTS (
             SELECT 1 FROM cfg_tbl_user
             WHERE ser_user_id = @dsg_user_id
               AND txt_password = @taimoor_password
         )
        THEN CONCAT('SUCCESS - dsg user (', IFNULL(@dsg_username, 'N/A'), ') password updated to match taimoor')
        WHEN @dsg_user_id IS NULL
        THEN 'ERROR - dsg user not found. Check the "All potential dsg users" query above.'
        WHEN @taimoor_password IS NULL
        THEN 'ERROR - taimoor user not found. Cannot copy password.'
        ELSE 'ERROR - Password update failed. Check verification query above.'
    END AS 'Status';




