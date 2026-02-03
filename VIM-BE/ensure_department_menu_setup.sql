-- =====================================================
-- SQL Script: Ensure Department Menu Setup
-- Database: vim_3
-- Description: This script ensures Department is properly configured
--              in Master Data menu with all permissions
--              Safe to run multiple times (idempotent)
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find Master Data Menu ID
-- =====================================================
SET @master_data_menu_id = (
    SELECT ser_menu_id 
    FROM cfg_tbl_menu 
    WHERE txt_menu_name = 'Master Data'
      AND bl_is_deleted = 0
      AND bln_status = 1
    LIMIT 1
);

SELECT 
    @master_data_menu_id AS 'Master Data Menu ID',
    CASE 
        WHEN @master_data_menu_id IS NULL THEN 'ERROR - Master Data menu not found'
        ELSE 'SUCCESS - Master Data menu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Get Department Submenu ID
-- =====================================================
SET @department_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

SELECT 
    CASE 
        WHEN @department_submenu_id IS NOT NULL THEN CONCAT('Department submenu EXISTS with ID: ', @department_submenu_id)
        ELSE 'Department submenu does NOT exist'
    END AS 'Department Submenu Status';

-- =====================================================
-- Step 3: Update Department Submenu Configuration
-- =====================================================
-- Ensure Department is correctly configured
UPDATE cfg_tbl_sub_menu 
SET 
    ser_menu_id = @master_data_menu_id,
    txt_sub_menu_url = 'department',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @department_submenu_id
  AND @master_data_menu_id IS NOT NULL
  AND @department_submenu_id IS NOT NULL;

-- =====================================================
-- Step 4: Ensure Permissions Exist for All Roles
-- =====================================================
-- Create role-based permissions (without ser_user_id) for all roles
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
    r.ser_role_id,
    NULL,  -- NULL for role-based permissions (not user-specific)
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission (bl_is_view)
    1,  -- Add permission (bl_is_add)
    0,  -- Delete permission (bl_is_delete)
    1,  -- Update permission (bl_is_update)
    0,  -- Approve permission (bl_is_approve)
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1   -- Created by user ID 1 (CHANGE THIS TO YOUR ADMIN USER ID)
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND @department_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @department_submenu_id 
      AND smr.ser_role_id = r.ser_role_id
      AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)  -- Role-based permission
  );

-- =====================================================
-- Step 5: Update Existing Permissions to Ensure Correct Values
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
WHERE smr.ser_sub_menu_id = @department_submenu_id
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)  -- Only update role-based permissions
  AND @department_submenu_id IS NOT NULL;

-- =====================================================
-- Step 6: Verification Queries
-- =====================================================

-- Verify Department submenu configuration
SELECT 
    'Department Submenu Configuration' AS Check_Type,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.txt_sub_menu_url AS 'URL',
    m.txt_menu_name AS 'Parent Menu',
    sm.int_sub_menu_order AS 'Order',
    sm.bl_is_active AS 'Active',
    sm.bln_status AS 'Status',
    sm.bl_is_deleted AS 'Deleted'
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.ser_sub_menu_id = @department_submenu_id;

-- Verify Role Permissions Count
SELECT 
    'Role Permissions Summary' AS Check_Type,
    COUNT(*) AS 'Total Role Permissions',
    COUNT(CASE WHEN smr.bl_is_enabled = 1 THEN 1 END) AS 'Enabled',
    COUNT(CASE WHEN smr.bl_is_view = 1 THEN 1 END) AS 'Can View',
    COUNT(CASE WHEN smr.bl_is_add = 1 THEN 1 END) AS 'Can Add',
    COUNT(CASE WHEN smr.bl_is_update = 1 THEN 1 END) AS 'Can Update',
    COUNT(DISTINCT smr.ser_role_id) AS 'Unique Roles'
FROM cfg_tbl_sub_menu_role smr
WHERE smr.ser_sub_menu_id = @department_submenu_id
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0);

-- List all Master Data submenus
SELECT 
    'All Master Data Submenus' AS Check_Type,
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.txt_sub_menu_url,
    sm.int_sub_menu_order,
    sm.bl_is_active,
    sm.bln_status
FROM cfg_tbl_sub_menu sm
WHERE sm.ser_menu_id = @master_data_menu_id
  AND sm.bl_is_deleted = 0
ORDER BY sm.int_sub_menu_order;

-- List permissions per role
SELECT 
    'Permissions by Role' AS Check_Type,
    r.txt_role_name AS 'Role',
    smr.bl_is_enabled AS 'Enabled',
    smr.bl_is_view AS 'View',
    smr.bl_is_add AS 'Add',
    smr.bl_is_update AS 'Update',
    smr.bl_is_delete AS 'Delete'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
WHERE smr.ser_sub_menu_id = @department_submenu_id
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
ORDER BY r.txt_role_name;

-- =====================================================
-- Step 7: Final Status
-- =====================================================
SELECT 
    'FINAL STATUS' AS Check_Type,
    CASE 
        WHEN @department_submenu_id IS NOT NULL
         AND EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            WHERE sm.ser_sub_menu_id = @department_submenu_id
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
              AND sm.bl_is_active = 1
              AND sm.ser_menu_id = @master_data_menu_id
        )
        AND EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu_role smr
            WHERE smr.ser_sub_menu_id = @department_submenu_id
              AND smr.bl_is_enabled = 1
              AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
        )
        THEN '✅ Department is correctly configured - RESTART BACKEND to see changes'
        ELSE '❌ Department needs attention - check verification queries above'
    END AS Status;

-- =====================================================
-- IMPORTANT: After running this script
-- 1. RESTART the backend Spring Boot application
-- 2. Clear browser cache (Ctrl+Shift+R)
-- 3. Department should appear in:
--    - Master Data sidebar menu
--    - Permission page (/role)
-- =====================================================



