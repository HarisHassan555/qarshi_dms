-- =====================================================
-- SQL Script: Create Department Permissions for ALL Users
-- Database: vim_3
-- Description: Creates user-specific permissions for Department
--              for ALL users so they can see it in the menu
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
-- Step 2: Create User-Specific Permissions for ALL Users
-- =====================================================
-- This creates Department permissions for all active users
-- based on their role

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
    1   -- Created by user ID 1 (CHANGE THIS TO YOUR ADMIN USER ID)
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND u.ser_role_id IS NOT NULL
  AND @department_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @department_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- =====================================================
-- Step 3: Update Existing Permissions (if any)
-- =====================================================
-- Ensure all existing user-specific permissions are enabled
UPDATE cfg_tbl_sub_menu_role smr
SET 
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    bl_is_enabled = 1,
    bl_is_all = 0,
    dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @department_submenu_id
  AND smr.ser_user_id IS NOT NULL
  AND @department_submenu_id IS NOT NULL;

-- =====================================================
-- Step 4: Verification
-- =====================================================

-- Count total user-specific permissions for Department
SELECT 
    'Department Permissions Summary' AS Check_Type,
    COUNT(*) AS 'Total User Permissions',
    COUNT(DISTINCT smr.ser_user_id) AS 'Unique Users',
    COUNT(DISTINCT smr.ser_role_id) AS 'Unique Roles',
    COUNT(CASE WHEN smr.bl_is_enabled = 1 THEN 1 END) AS 'Enabled Permissions',
    COUNT(CASE WHEN smr.bl_is_view = 1 THEN 1 END) AS 'Can View',
    COUNT(CASE WHEN smr.bl_is_add = 1 THEN 1 END) AS 'Can Add',
    COUNT(CASE WHEN smr.ser_user_id IS NOT NULL THEN 1 END) AS 'User-Specific Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

-- List permissions by user
SELECT 
    'Permissions by User' AS Check_Type,
    u.txt_user_name AS 'Username',
    r.txt_role_name AS 'Role',
    smr.bl_is_enabled AS 'Enabled',
    smr.bl_is_view AS 'View',
    smr.bl_is_add AS 'Add',
    smr.bl_is_update AS 'Update'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_user u ON smr.ser_user_id = u.ser_user_id
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
WHERE sm.txt_sub_menu_name = 'Department'
  AND smr.ser_user_id IS NOT NULL
ORDER BY u.txt_user_name
LIMIT 50;

-- Check specific user (taimoor)
SELECT 
    'Taimoor Permissions' AS Check_Type,
    u.txt_user_name AS 'Username',
    r.txt_role_name AS 'Role',
    smr.bl_is_enabled AS 'Enabled',
    smr.bl_is_view AS 'View',
    smr.bl_is_add AS 'Add',
    smr.bl_is_update AS 'Update',
    smr.ser_sub_menu_role_id AS 'Permission ID'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_user u ON smr.ser_user_id = u.ser_user_id
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
WHERE sm.txt_sub_menu_name = 'Department'
  AND u.txt_user_name LIKE '%taimoor%';

-- =====================================================
-- FINAL STATUS
-- =====================================================
SELECT 
    'FINAL STATUS' AS Check_Type,
    CASE 
        WHEN @department_submenu_id IS NOT NULL
         AND EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu_role smr
            WHERE smr.ser_sub_menu_id = @department_submenu_id
              AND smr.ser_user_id IS NOT NULL
              AND smr.bl_is_enabled = 1
        )
        THEN '✅ Department permissions created for all users - RESTART BACKEND and LOGOUT/LOGIN'
        ELSE '❌ Department permissions need attention'
    END AS Status;

-- =====================================================
-- IMPORTANT: After running this script
-- 1. RESTART the backend Spring Boot application
-- 2. Clear browser cache (Ctrl+Shift+R)
-- 3. LOGOUT and LOGIN again as taimoor (or any user)
-- 4. Department should now appear in Master Data menu
-- 5. Department should also appear in Permission page (/role)
-- =====================================================









