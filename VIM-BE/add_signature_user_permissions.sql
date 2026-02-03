-- =====================================================
-- SQL Script: Add User-Specific Permissions for Signature
-- Database: vim_3
-- Description: This script adds user-specific permissions for Signature submenu
--              so it appears in the sidebar menu and /role page
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find Signature Submenu ID
-- =====================================================
SET @signature_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Signature'
    LIMIT 1
);

SELECT 
    @signature_submenu_id AS 'Signature Submenu ID',
    CASE 
        WHEN @signature_submenu_id IS NULL THEN 'ERROR - Signature submenu not found'
        ELSE 'SUCCESS - Signature submenu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Create User-Specific Permissions for Signature
-- =====================================================
-- Insert permissions for all active users
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
    @signature_submenu_id,
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
  AND @signature_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @signature_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

SELECT CONCAT('Created user-specific permissions: ', ROW_COUNT(), ' rows') AS 'Insert Status';

-- =====================================================
-- Step 3: Update existing user-specific permissions
-- =====================================================
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
WHERE smr.ser_sub_menu_id = @signature_submenu_id
  AND smr.ser_user_id IS NOT NULL
  AND @signature_submenu_id IS NOT NULL;

SELECT CONCAT('Updated user-specific permissions: ', ROW_COUNT(), ' rows') AS 'Update Status';

-- =====================================================
-- Step 4: Verification
-- =====================================================
SELECT 
    'User Permissions Summary' AS Check_Type,
    COUNT(*) AS 'Total User Permissions',
    COUNT(DISTINCT smr.ser_user_id) AS 'Unique Users',
    COUNT(DISTINCT smr.ser_role_id) AS 'Unique Roles',
    COUNT(CASE WHEN smr.bl_is_enabled = 1 THEN 1 END) AS 'Enabled Permissions',
    COUNT(CASE WHEN smr.bl_is_view = 1 THEN 1 END) AS 'Can View',
    COUNT(CASE WHEN smr.bl_is_add = 1 THEN 1 END) AS 'Can Add',
    COUNT(CASE WHEN smr.ser_user_id IS NOT NULL THEN 1 END) AS 'User-Specific Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Signature'
  AND smr.ser_user_id IS NOT NULL;

-- =====================================================
-- Step 5: Final Status
-- =====================================================
SELECT 
    'FINAL STATUS' AS Check_Type,
    CASE 
        WHEN @signature_submenu_id IS NOT NULL
         AND EXISTS (
             SELECT 1 
             FROM cfg_tbl_sub_menu_role smr
             WHERE smr.ser_sub_menu_id = @signature_submenu_id
               AND smr.ser_user_id IS NOT NULL
               AND smr.bl_is_enabled = 1
         )
        THEN '✅ Signature user permissions created - RESTART BACKEND and LOGOUT/LOGIN to see changes'
        ELSE '❌ Signature user permissions need attention'
    END AS Status;

-- =====================================================
-- Notes:
-- 1. After running this script, RESTART the backend application
-- 2. LOGOUT and LOGIN again in the frontend
-- 3. Signature should now appear in:
--    - Master Data sidebar menu
--    - Permission page (/role) for assigning controls
-- =====================================================



