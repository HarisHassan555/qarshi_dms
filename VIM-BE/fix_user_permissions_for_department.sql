-- =====================================================
-- SQL Script: Fix User Permissions for Department
-- Database: vim_3
-- Description: Creates user-specific permissions for Department
--              so users can see it in the menu
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Get Department Submenu ID
-- =====================================================
SET @department_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
    LIMIT 1
);

SELECT 
    @department_submenu_id AS 'Department Submenu ID',
    CASE 
        WHEN @department_submenu_id IS NULL THEN 'ERROR - Department submenu not found'
        ELSE 'SUCCESS - Department submenu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Get User ID for "taimoor"
-- =====================================================
SET @taimoor_user_id = (
    SELECT ser_user_id 
    FROM cfg_tbl_user 
    WHERE txt_user_name = 'taimoor' 
       OR txt_user_name LIKE '%taimoor%'
    LIMIT 1
);

SELECT 
    @taimoor_user_id AS 'Taimoor User ID',
    CASE 
        WHEN @taimoor_user_id IS NULL THEN 'ERROR - User taimoor not found'
        ELSE 'SUCCESS - User taimoor found'
    END AS 'Status';

-- If user not found, show all users (run this separately if needed)
-- SELECT 
--     'Available Users' AS Info,
--     ser_user_id AS 'User ID',
--     txt_user_name AS 'Username',
--     txt_user_email AS 'Email'
-- FROM cfg_tbl_user
-- WHERE bl_is_deleted = 0
-- ORDER BY txt_user_name
-- LIMIT 20;

-- =====================================================
-- Step 3: Get Role ID for taimoor
-- =====================================================
SET @taimoor_role_id = (
    SELECT ser_role_id 
    FROM cfg_tbl_user 
    WHERE ser_user_id = @taimoor_user_id
    LIMIT 1
);

SELECT 
    @taimoor_role_id AS 'Taimoor Role ID',
    CASE 
        WHEN @taimoor_role_id IS NULL THEN 'ERROR - Role not found for user'
        ELSE 'SUCCESS - Role found'
    END AS 'Status';

-- =====================================================
-- Step 4: Create User-Specific Permissions for Department
-- =====================================================
-- Delete existing user-specific permissions for Department (if any)
DELETE FROM cfg_tbl_sub_menu_role
WHERE ser_sub_menu_id = @department_submenu_id
  AND ser_user_id = @taimoor_user_id
  AND ser_role_id = @taimoor_role_id;

-- Insert user-specific permissions for taimoor
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
    dte_created_date,
    ser_created_user
)
SELECT 
    @department_submenu_id,
    @taimoor_role_id,
    @taimoor_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    1,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1   -- Created by user ID 1 (CHANGE THIS TO YOUR ADMIN USER ID)
WHERE @department_submenu_id IS NOT NULL
  AND @taimoor_user_id IS NOT NULL
  AND @taimoor_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @department_submenu_id 
      AND smr.ser_user_id = @taimoor_user_id
      AND smr.ser_role_id = @taimoor_role_id
  );

-- =====================================================
-- Step 5: Create Permissions for ALL Users (Optional)
-- =====================================================
-- This will create Department permissions for all users
-- Uncomment the section below if you want to enable Department for all users

/*
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
    dte_created_date,
    ser_created_user
)
SELECT 
    @department_submenu_id,
    u.ser_role_id,
    u.ser_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    1,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1   -- Created by user ID 1
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND @department_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @department_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );
*/

-- =====================================================
-- Step 6: Verification
-- =====================================================

-- Check taimoor's permissions for Department
SELECT 
    'Taimoor Department Permissions' AS Check_Type,
    smr.ser_sub_menu_role_id AS 'Permission ID',
    sm.txt_sub_menu_name AS 'Submenu',
    u.txt_user_name AS 'User',
    r.txt_role_name AS 'Role',
    smr.bl_is_enabled AS 'Enabled',
    smr.bl_is_view AS 'View',
    smr.bl_is_add AS 'Add',
    smr.bl_is_update AS 'Update',
    smr.bl_is_delete AS 'Delete'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_user u ON smr.ser_user_id = u.ser_user_id
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
WHERE sm.txt_sub_menu_name = 'Department'
  AND u.txt_user_name LIKE '%taimoor%';

-- Count total user-specific permissions for Department
SELECT 
    'Total User Permissions' AS Check_Type,
    COUNT(*) AS 'User-Specific Permissions',
    COUNT(DISTINCT smr.ser_user_id) AS 'Unique Users',
    COUNT(CASE WHEN smr.bl_is_enabled = 1 THEN 1 END) AS 'Enabled Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Department'
  AND smr.ser_user_id IS NOT NULL;

-- =====================================================
-- IMPORTANT: After running this script
-- 1. RESTART the backend Spring Boot application
-- 2. Clear browser cache (Ctrl+Shift+R)
-- 3. Logout and login again as taimoor
-- 4. Department should now appear in Master Data menu
-- =====================================================

