USE vim_3;

-- =====================================================
-- Script to create a new user: Haris
-- Email: haris@dsg.com
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

-- Step 2: Check if user "Haris" already exists
SET @existing_user_id = (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE txt_user_name = 'Haris'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT 
    CASE
        WHEN @existing_user_id IS NOT NULL THEN CONCAT('User "Haris" already EXISTS with ID: ', @existing_user_id)
        ELSE 'User "Haris" does NOT exist - will be created'
    END AS 'User Status';

-- Step 3: Get the next available user ID (if needed)
SET @next_user_id = IFNULL((SELECT MAX(ser_user_id) + 1 FROM cfg_tbl_user), 1);

-- Step 4: Get taimoor's password to use for Haris
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
    'Haris',
    'haris@dsg.com',
    @default_password,  -- Base64 encoded "password"
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
        WHEN @existing_user_id IS NULL AND @admin_role_id IS NOT NULL THEN 'User "Haris" inserted successfully!'
        WHEN @existing_user_id IS NOT NULL THEN 'User "Haris" already exists - skipping insert'
        ELSE 'ERROR: Admin role not found, cannot create user'
    END AS 'Insert Status';

-- Step 5b: Update existing user (if exists)
UPDATE cfg_tbl_user
SET
    txt_address = 'haris@dsg.com',
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
        WHEN @existing_user_id IS NOT NULL AND @admin_role_id IS NOT NULL THEN 'User "Haris" updated successfully!'
        WHEN @existing_user_id IS NULL THEN 'No existing user to update'
        ELSE 'ERROR: Admin role not found, cannot update user'
    END AS 'Update Status';

-- =====================================================
-- Step 6: Verification
-- =====================================================
SELECT 'Verifying user "Haris":' AS 'Verification';
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
WHERE txt_user_name = 'Haris'
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL);

-- =====================================================
-- Step 7: Login Credentials
-- =====================================================
SELECT 
    'Login Credentials for Haris:' AS 'Info',
    'Username: Haris' AS 'Username',
    CASE 
        WHEN @taimoor_password IS NOT NULL THEN 'Password: Same as taimoor user'
        ELSE 'Password: password (default)'
    END AS 'Password (Plain Text)',
    'Email: haris@dsg.com' AS 'Email',
    'Role: ADMIN' AS 'Role',
    CASE 
        WHEN @taimoor_password IS NOT NULL THEN 'SUCCESS: Password copied from taimoor user'
        ELSE 'WARNING: taimoor user not found, using default password'
    END AS 'Note';

