-- =====================================================
-- SQL Script: Add Department Menu (Version 2 - No Stored Procedures)
-- Database: vim_3
-- Description: Simple script to add Department to Master Data menu
--              This version avoids stored procedures for better compatibility
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find Master Data Menu ID
-- =====================================================
SET @master_data_menu_id = (
    SELECT ser_menu_id 
    FROM cfg_tbl_menu 
    WHERE (txt_menu_name LIKE '%Master Data%' 
           OR txt_menu_name LIKE '%Master%'
           OR txt_menu_name LIKE '%master%')
      AND bl_is_deleted = 0
      AND bln_status = 1
    ORDER BY ser_menu_id
    LIMIT 1
);

-- If Master Data menu not found, you may need to create it or use a different menu
-- Uncomment the line below and set the menu ID manually if needed:
-- SET @master_data_menu_id = 1; -- Replace 1 with your Master Data menu ID

-- =====================================================
-- Step 2: Diagnostic - Check values
-- =====================================================
SELECT 
    @master_data_menu_id AS 'Master Data Menu ID',
    CASE 
        WHEN @master_data_menu_id IS NULL THEN 'NOT FOUND - Please set manually'
        ELSE 'Found'
    END AS 'Menu Status';

-- =====================================================
-- Step 3: Insert Department Submenu
-- =====================================================
-- IMPORTANT: If the diagnostic shows "NOT FOUND", you must manually set @master_data_menu_id
-- Example: SET @master_data_menu_id = 1; (replace 1 with your actual Master Data menu ID)

-- Pre-check: Only proceed if menu ID exists and Department doesn't exist
SET @can_insert = (
    SELECT CASE 
        WHEN @master_data_menu_id IS NOT NULL 
         AND NOT EXISTS (
             SELECT 1 
             FROM cfg_tbl_sub_menu 
             WHERE txt_sub_menu_name = 'Department' 
               AND ser_menu_id = @master_data_menu_id
         ) THEN 1
        ELSE 0
    END
);

-- Get the next order number
SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1 
    FROM cfg_tbl_sub_menu 
    WHERE ser_menu_id = @master_data_menu_id
), 1);

-- Insert only if conditions are met
-- This pattern ensures we always have exactly one row when @can_insert = 1
INSERT INTO cfg_tbl_sub_menu (
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
FROM (SELECT @can_insert AS can_insert) AS check_table
WHERE can_insert = 1;

-- =====================================================
-- Step 4: Get the inserted Department Submenu ID
-- =====================================================
SET @department_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 5: Create Role Permissions for Department
-- This grants permissions to all existing roles
-- =====================================================
INSERT IGNORE INTO cfg_tbl_sub_menu_role (
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
-- Verification
-- =====================================================
SELECT 
    'Department Submenu Created' AS Status,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.txt_sub_menu_url AS 'URL',
    m.txt_menu_name AS 'Parent Menu'
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

SELECT 
    'Role Permissions Created' AS Status,
    COUNT(*) AS 'Total Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

