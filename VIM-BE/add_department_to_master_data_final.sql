-- =====================================================
-- SQL Script: Add Department to Master Data Menu
-- Database: vim_3
-- Description: This script ensures Department submenu is properly
--              added to Master Data menu with all permissions
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

-- Display Master Data Menu ID
SELECT 
    @master_data_menu_id AS 'Master Data Menu ID',
    CASE 
        WHEN @master_data_menu_id IS NULL THEN 'ERROR - Master Data menu not found'
        ELSE 'SUCCESS - Master Data menu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Check if Department submenu already exists
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
        ELSE 'Department submenu does NOT exist - will be created'
    END AS 'Department Submenu Status';

-- =====================================================
-- Step 3: Get next order number for Department
-- =====================================================
SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1 
    FROM cfg_tbl_sub_menu 
    WHERE ser_menu_id = @master_data_menu_id
), 1);

-- =====================================================
-- Step 4: Insert or Update Department Submenu
-- =====================================================
-- If Department doesn't exist, insert it
-- If it exists, update it to ensure correct configuration

-- First, get the next available ID if we need to insert
SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);

-- Insert Department submenu if it doesn't exist
INSERT INTO cfg_tbl_sub_menu (
    ser_sub_menu_id,
    ser_menu_id,
    txt_sub_menu_name,
    txt_sub_menu_url,
    int_sub_menu_order,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    bl_is_view,
    bl_is_add,
    bl_is_delete,
    bl_is_update,
    bl_is_approve,
    dte_created_date,
    ser_created_user
)
SELECT 
    @next_submenu_id,
    @master_data_menu_id,
    'Department',
    'department',
    @next_order,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    1,  -- Update permission
    0,  -- Approve permission
    NOW(),
    1   -- Created by user ID 1 (CHANGE THIS TO YOUR ADMIN USER ID)
WHERE @department_submenu_id IS NULL
  AND @master_data_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
      AND ser_menu_id = @master_data_menu_id
  );

-- Update Department submenu if it exists (using the ID we already have)
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
-- Step 5: Get Department Submenu ID (after insert/update)
-- =====================================================
SET @department_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

SELECT 
    @department_submenu_id AS 'Department Submenu ID',
    CASE 
        WHEN @department_submenu_id IS NOT NULL THEN 'SUCCESS - Department submenu ID found'
        ELSE 'ERROR - Department submenu ID not found'
    END AS 'Status';

-- =====================================================
-- Step 6: Create Role Permissions for Department
-- =====================================================
-- Insert permissions for all active roles
INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id,
    ser_role_id,
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
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND @department_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @department_submenu_id 
      AND smr.ser_role_id = r.ser_role_id
  );

-- =====================================================
-- Step 7: Update existing permissions to ensure they're correct
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
  AND @department_submenu_id IS NOT NULL;

-- =====================================================
-- Step 8: Verification Queries
-- =====================================================

-- Verify Department submenu exists and is configured correctly
SELECT 
    'Department Submenu Verification' AS Check_Type,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.txt_sub_menu_url AS 'URL',
    m.txt_menu_name AS 'Parent Menu',
    sm.int_sub_menu_order AS 'Order',
    sm.bl_is_active AS 'Active',
    sm.bln_status AS 'Status',
    sm.bl_is_deleted AS 'Deleted',
    sm.bl_is_view AS 'Can View',
    sm.bl_is_add AS 'Can Add',
    sm.bl_is_update AS 'Can Update'
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Department'
  AND sm.ser_menu_id = @master_data_menu_id;

-- Verify Role Permissions
SELECT 
    'Role Permissions Verification' AS Check_Type,
    COUNT(*) AS 'Total Permissions',
    COUNT(CASE WHEN smr.bl_is_enabled = 1 THEN 1 END) AS 'Enabled Permissions',
    COUNT(CASE WHEN smr.bl_is_view = 1 THEN 1 END) AS 'Can View',
    COUNT(CASE WHEN smr.bl_is_add = 1 THEN 1 END) AS 'Can Add',
    COUNT(CASE WHEN smr.bl_is_update = 1 THEN 1 END) AS 'Can Update',
    COUNT(DISTINCT smr.ser_role_id) AS 'Roles with Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Department'
  AND sm.ser_menu_id = @master_data_menu_id;

-- List all Master Data submenus (including Department)
SELECT 
    'All Master Data Submenus' AS Check_Type,
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.txt_sub_menu_url,
    sm.int_sub_menu_order,
    sm.bl_is_active,
    sm.bln_status,
    sm.bl_is_deleted
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE m.ser_menu_id = @master_data_menu_id
  AND m.bl_is_deleted = 0
  AND m.bln_status = 1
ORDER BY sm.int_sub_menu_order;

-- List permissions per role for Department
SELECT 
    'Permissions by Role' AS Check_Type,
    r.txt_role_name AS 'Role',
    smr.bl_is_enabled AS 'Enabled',
    smr.bl_is_view AS 'View',
    smr.bl_is_add AS 'Add',
    smr.bl_is_update AS 'Update',
    smr.bl_is_delete AS 'Delete'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
WHERE sm.txt_sub_menu_name = 'Department'
  AND sm.ser_menu_id = @master_data_menu_id
ORDER BY r.txt_role_name;

-- =====================================================
-- Step 9: Final Status Check
-- =====================================================
SELECT 
    'FINAL STATUS' AS Check_Type,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
            WHERE sm.txt_sub_menu_name = 'Department'
              AND m.txt_menu_name = 'Master Data'
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
              AND sm.bl_is_active = 1
        ) 
        AND EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu_role smr
            INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
            WHERE sm.txt_sub_menu_name = 'Department'
              AND smr.bl_is_enabled = 1
        )
        THEN '✅ Department is correctly configured and ready to use'
        ELSE '❌ Department needs attention - check the verification queries above'
    END AS Status;

-- =====================================================
-- Notes:
-- 1. After running this script, RESTART the backend application
-- 2. Clear browser cache and refresh the frontend
-- 3. Department should appear in:
--    - Master Data sidebar menu
--    - Permission page (/role) for assigning controls
-- 4. The script is idempotent - safe to run multiple times
-- =====================================================

