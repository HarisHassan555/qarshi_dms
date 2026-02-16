-- =====================================================
-- SQL Script: Fix CAPF User-Specific Permissions
-- Database: vim_3
-- Description: Creates user-specific permissions for CAPF submenu
--              Run this if Step 7 in add_capf_to_vim_menu.sql failed
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Get CAPF Submenu ID
-- =====================================================
SET @capf_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'CAPF' 
    LIMIT 1
);

SELECT 
    @capf_submenu_id AS 'CAPF Submenu ID',
    CASE 
        WHEN @capf_submenu_id IS NULL THEN 'ERROR - CAPF submenu not found'
        ELSE 'SUCCESS - CAPF submenu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Create User-Specific Permissions for CAPF
-- =====================================================
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
    @capf_submenu_id,
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
  AND @capf_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @capf_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- =====================================================
-- Step 3: Update Existing User Permissions (if any)
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
WHERE smr.ser_sub_menu_id = @capf_submenu_id
  AND smr.ser_user_id IS NOT NULL
  AND @capf_submenu_id IS NOT NULL;

-- =====================================================
-- Step 4: Verification
-- =====================================================
SELECT 
    'CAPF Permissions Summary' AS Check_Type,
    COUNT(*) AS 'Total User Permissions',
    COUNT(DISTINCT smr.ser_user_id) AS 'Unique Users',
    COUNT(DISTINCT smr.ser_role_id) AS 'Unique Roles',
    COUNT(CASE WHEN smr.bl_is_enabled = 1 THEN 1 END) AS 'Enabled Permissions',
    COUNT(CASE WHEN smr.bl_is_view = 1 THEN 1 END) AS 'Can View',
    COUNT(CASE WHEN smr.bl_is_add = 1 THEN 1 END) AS 'Can Add',
    COUNT(CASE WHEN smr.ser_user_id IS NOT NULL THEN 1 END) AS 'User-Specific Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'CAPF';

-- Final status
SELECT 
    CASE 
        WHEN @capf_submenu_id IS NOT NULL
         AND EXISTS (
             SELECT 1 
             FROM cfg_tbl_sub_menu_role smr
             WHERE smr.ser_sub_menu_id = @capf_submenu_id
               AND smr.ser_user_id IS NOT NULL
               AND smr.bl_is_enabled = 1
         )
        THEN '✅ CAPF permissions created for all users - RESTART BACKEND and LOGOUT/LOGIN'
        ELSE '❌ CAPF permissions need attention'
    END AS 'FINAL STATUS';





