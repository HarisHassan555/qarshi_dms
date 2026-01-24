-- =====================================================
-- SQL Script: Add Department to Master Data Menu
-- Database: vim_3
-- Description: This script adds Department submenu to Master Data menu
--              and sets up permissions for all roles
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Add department column to cfg_tbl_user if it doesn't exist
-- =====================================================
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'vim_3' 
    AND TABLE_NAME = 'cfg_tbl_user' 
    AND COLUMN_NAME = 'ser_department_id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE cfg_tbl_user 
     ADD COLUMN ser_department_id INT NULL,
     ADD CONSTRAINT fk_user_department 
     FOREIGN KEY (ser_department_id) 
     REFERENCES hr_tbl_department(ser_department_id) 
     ON DELETE SET NULL',
    'SELECT "Column ser_department_id already exists in cfg_tbl_user" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- Step 2: Find or Get Master Data Menu ID
-- =====================================================
-- First, let's check if Master Data menu exists
SET @master_data_menu_id = (
    SELECT ser_menu_id 
    FROM cfg_tbl_menu 
    WHERE txt_menu_name LIKE '%Master Data%' 
       OR txt_menu_name LIKE '%Master%'
    LIMIT 1
);

-- If Master Data menu doesn't exist, you may need to create it first
-- For now, we'll assume it exists. If not, uncomment and adjust the INSERT below:
/*
INSERT INTO cfg_tbl_menu (
    txt_menu_name,
    txt_menu_icons,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    dte_created_date,
    ser_created_user
) VALUES (
    'Master Data',
    'icon-folder',
    1,
    1,
    0,
    NOW(),
    1
);
SET @master_data_menu_id = LAST_INSERT_ID();
*/

-- =====================================================
-- Step 3: Insert Department Submenu
-- =====================================================
-- Check if Department submenu already exists
SET @department_submenu_exists = (
    SELECT COUNT(*) 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
       OR txt_sub_menu_url LIKE '%department%'
);

-- Get the next order number for submenu
SET @next_order = (
    SELECT COALESCE(MAX(int_sub_menu_order), 0) + 1 
    FROM cfg_tbl_sub_menu 
    WHERE ser_menu_id = @master_data_menu_id
);

-- Insert Department submenu if it doesn't exist
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
    0,  -- Delete permission (set to 0, can be enabled later)
    1,  -- Update permission
    0,  -- Approve permission (not needed for department)
    NOW(),
    1   -- Created by user ID 1 (adjust as needed)
WHERE @department_submenu_exists = 0;

-- Get the Department submenu ID
SET @department_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 4: Create Submenu Role Permissions for All Roles
-- =====================================================
-- This will create permissions for all existing roles
-- You can customize permissions per role as needed

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
    1   -- Created by user ID 1
FROM cfg_tbl_role r
WHERE NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @department_submenu_id 
      AND smr.ser_role_id = r.ser_role_id
)
AND @department_submenu_id IS NOT NULL;

-- =====================================================
-- Step 5: Create User-Specific Submenu Role Permissions
-- =====================================================
-- This creates user-specific permissions (optional)
-- Uncomment if you need user-specific permissions

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
    u.cfgTblRole.ser_role_id,
    u.ser_user_id,
    1, 1, 0, 1, 1, 0, 1, 0, 1, 0,
    NOW(),
    1
FROM cfg_tbl_user u
WHERE u.bln_status = 1 
  AND u.bl_is_deleted = 0
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @department_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
  )
AND @department_submenu_id IS NOT NULL;
*/

-- =====================================================
-- Verification Queries
-- =====================================================

-- Verify Department submenu was created
SELECT 
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Submenu Name',
    sm.txt_sub_menu_url AS 'URL',
    m.txt_menu_name AS 'Menu Name',
    sm.int_sub_menu_order AS 'Order',
    sm.bl_is_active AS 'Active',
    sm.bln_status AS 'Status'
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

-- Verify Role Permissions
SELECT 
    smr.ser_sub_menu_role_id AS 'Permission ID',
    sm.txt_sub_menu_name AS 'Submenu',
    r.txt_role_name AS 'Role',
    smr.bl_is_view AS 'View',
    smr.bl_is_add AS 'Add',
    smr.bl_is_update AS 'Update',
    smr.bl_is_delete AS 'Delete',
    smr.bl_is_enabled AS 'Enabled'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
WHERE sm.txt_sub_menu_name = 'Department'
ORDER BY r.txt_role_name;

-- Verify Department column in user table
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_user'
  AND COLUMN_NAME = 'ser_department_id';

-- =====================================================
-- Notes:
-- 1. Replace @master_data_menu_id with actual Master Data menu ID if known
-- 2. Adjust ser_created_user (currently set to 1) to your admin user ID
-- 3. Customize permissions (bl_is_view, bl_is_add, etc.) per your requirements
-- 4. Run this script as a user with appropriate database permissions
-- =====================================================

