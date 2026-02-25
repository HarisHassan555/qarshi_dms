USE vim_3;

-- =====================================================
-- Script to create a new user: Talha
-- Email: talha@dsg.com
-- Role: Admin
-- =====================================================

-- Step 1: Find the Admin role ID
SET @admin_role_id = (
    SELECT ser_role_id
    FROM cfg_tbl_role
    WHERE (txt_role_name LIKE '%ADMIN%' OR txt_role_name LIKE '%admin%' OR txt_role_name LIKE '%Admin%')
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
      AND (bl_is_active = true OR bl_is_active IS NULL)
    ORDER BY ser_role_id
    LIMIT 1
);

SELECT 
    @admin_role_id AS 'Admin Role ID',
    CASE
        WHEN @admin_role_id IS NULL THEN 'ERROR - Admin role not found'
        ELSE 'SUCCESS - Admin role found'
    END AS 'Status';

-- Step 2: Check if user "Talha" already exists
SET @existing_user_id = (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE txt_user_name = 'Talha'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT 
    CASE
        WHEN @existing_user_id IS NOT NULL THEN CONCAT('User "Talha" already EXISTS with ID: ', @existing_user_id)
        ELSE 'User "Talha" does NOT exist - will be created'
    END AS 'User Status';

-- Step 3: Get the next available user ID (if needed)
SET @next_user_id = IFNULL((SELECT MAX(ser_user_id) + 1 FROM cfg_tbl_user), 1);

-- Step 4: Get taimoor's password to use for Talha
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
        ELSE 'WARNING - taimoor user not found, will use default password'
    END AS 'Password Source';

-- Use taimoor's password if found, otherwise use default
SET @default_password = IFNULL(@taimoor_password, 'cGFzc3dvcmQ='); -- Use taimoor's password or default "password"

-- Step 5: Insert new user (if doesn't exist)
-- Using only essential columns to avoid column name issues
INSERT INTO cfg_tbl_user (
    ser_user_id,
    txt_user_name,
    txt_address,  -- Email stored here
    txt_password,
    ser_role_id,
    txt_role,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    dte_created_date,
    ser_created_user
)
SELECT 
    @next_user_id,
    'Talha',
    'talha@dsg.com',
    @default_password,  -- Base64 encoded password (from taimoor or default)
    @admin_role_id,
    'ADMIN',
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    NOW(),
    1   -- Created by user ID 1 (CHANGE THIS TO YOUR ADMIN USER ID IF DIFFERENT)
WHERE @existing_user_id IS NULL 
  AND @admin_role_id IS NOT NULL;

SELECT 
    CASE
        WHEN @existing_user_id IS NULL AND @admin_role_id IS NOT NULL THEN 'User "Talha" inserted successfully!'
        WHEN @existing_user_id IS NOT NULL THEN 'User "Talha" already exists - skipping insert'
        ELSE 'ERROR: Admin role not found, cannot create user'
    END AS 'Insert Status';

-- Step 5b: Update existing user (if exists)
UPDATE cfg_tbl_user
SET
    txt_address = 'talha@dsg.com',
    txt_password = @default_password,  -- Update password to match taimoor's
    ser_role_id = @admin_role_id,
    txt_role = 'ADMIN',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    dte_modified_date = NOW(),
    ser_modified_user = 1  -- Modified by user ID 1 (CHANGE THIS IF DIFFERENT)
WHERE ser_user_id = @existing_user_id
  AND @existing_user_id IS NOT NULL
  AND @admin_role_id IS NOT NULL;

SELECT 
    CASE
        WHEN @existing_user_id IS NOT NULL AND @admin_role_id IS NOT NULL THEN 'User "Talha" updated successfully!'
        WHEN @existing_user_id IS NULL THEN 'No existing user to update'
        ELSE 'ERROR: Admin role not found, cannot update user'
    END AS 'Update Status';

-- Get the final user ID (either newly created or existing)
SET @talha_user_id = IFNULL(@existing_user_id, @next_user_id);

-- =====================================================
-- Step 6: Grant All Permissions to Talha
-- =====================================================
-- Grant full access to all submenus for Talha user

-- Insert/Update permissions for Talha for all active submenus
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
    bl_is_create,      -- For canAdd in Permission screen
    bl_is_NewView,     -- For canView in Permission screen
    bl_is_NewUpdate,   -- For canUpdate in Permission screen
    dte_created_date,
    ser_created_user
)
SELECT 
    sm.ser_sub_menu_id,
    @admin_role_id,
    @talha_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    1,  -- Delete permission
    1,  -- Update permission
    1,  -- Approve permission
    1,  -- Enabled
    1,  -- All permissions
    1,  -- bl_is_create
    1,  -- bl_is_NewView
    1,  -- bl_is_NewUpdate
    NOW(),
    1   -- Created by user ID 1
FROM cfg_tbl_sub_menu sm
WHERE sm.bl_is_deleted = 0
  AND sm.bln_status = 1
  AND @talha_user_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
      AND smr.ser_user_id = @talha_user_id
      AND smr.ser_role_id = @admin_role_id
  );

-- Update existing permissions to ensure full access
UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 1,
    bl_is_update = 1,
    bl_is_approve = 1,
    bl_is_enabled = 1,
    bl_is_all = 1,
    bl_is_create = 1,      -- For canAdd
    bl_is_NewView = 1,     -- For canView
    bl_is_NewUpdate = 1,   -- For canUpdate
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE smr.ser_user_id = @talha_user_id
  AND smr.ser_role_id = @admin_role_id
  AND @talha_user_id IS NOT NULL
  AND @admin_role_id IS NOT NULL;

SELECT 
    CASE
        WHEN @talha_user_id IS NOT NULL AND @admin_role_id IS NOT NULL THEN 
            CONCAT('SUCCESS - Full permissions granted to Talha (User ID: ', @talha_user_id, ')')
        ELSE 'ERROR - Could not grant permissions (user or role not found)'
    END AS 'Permissions Status';

-- =====================================================
-- Step 7: Verification
-- =====================================================
SELECT 'Verifying user "Talha":' AS 'Verification';
SELECT 
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_address AS 'Email',
    txt_role AS 'Role',
    ser_role_id AS 'Role ID',
    bl_is_active AS 'Is Active',
    bln_status AS 'Status',
    bl_is_deleted AS 'Is Deleted',
    dte_created_date AS 'Created Date'
FROM cfg_tbl_user
WHERE txt_user_name = 'Talha'
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL);

-- Count permissions granted to Talha
SELECT 
    'Permissions Count for Talha:' AS 'Verification',
    COUNT(*) AS 'Total Permissions',
    SUM(CASE WHEN bl_is_view = 1 THEN 1 ELSE 0 END) AS 'View Permissions',
    SUM(CASE WHEN bl_is_add = 1 THEN 1 ELSE 0 END) AS 'Add Permissions',
    SUM(CASE WHEN bl_is_delete = 1 THEN 1 ELSE 0 END) AS 'Delete Permissions',
    SUM(CASE WHEN bl_is_update = 1 THEN 1 ELSE 0 END) AS 'Update Permissions',
    SUM(CASE WHEN bl_is_approve = 1 THEN 1 ELSE 0 END) AS 'Approve Permissions'
FROM cfg_tbl_sub_menu_role
WHERE ser_user_id = @talha_user_id
  AND bl_is_deleted = 0
  AND @talha_user_id IS NOT NULL;

-- =====================================================
-- Step 8: Login Credentials
-- =====================================================
SELECT 
    'Login Credentials for Talha:' AS 'Info',
    'Username: Talha' AS 'Username',
    CASE 
        WHEN @taimoor_password IS NOT NULL THEN 'Password: Same as taimoor user'
        ELSE 'Password: password (default)'
    END AS 'Password (Plain Text)',
    'Email: talha@dsg.com' AS 'Email',
    'Role: ADMIN' AS 'Role',
    CASE 
        WHEN @taimoor_password IS NOT NULL THEN 'SUCCESS: Password copied from taimoor user'
        ELSE 'WARNING: taimoor user not found, using default password'
    END AS 'Note',
    CASE 
        WHEN @talha_user_id IS NOT NULL THEN CONCAT('User ID: ', @talha_user_id)
        ELSE 'User ID: Not found'
    END AS 'User ID';






