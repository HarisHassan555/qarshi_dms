-- =====================================================
-- SQL Script: Add Department Menu (Simplified Version)
-- Database: vim_3
-- Description: Simple script to add Department to Master Data menu
--              Assumes Master Data menu ID is known or needs to be found
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find Master Data Menu ID
-- Adjust the menu name pattern if your menu has a different name
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

-- Check if Department submenu already exists
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu 
            WHERE txt_sub_menu_name = 'Department' 
              AND ser_menu_id = @master_data_menu_id
        ) THEN 'Department submenu ALREADY EXISTS - No action needed'
        ELSE 'Department submenu does NOT exist - Will be created'
    END AS 'Department Status';

-- =====================================================
-- Step 3: Insert Department Submenu
-- =====================================================
-- IMPORTANT: If Master Data Menu ID is NULL, the script will not insert.
-- Check the diagnostic output above and manually set @master_data_menu_id if needed.

-- Check if Department submenu already exists for this menu
SET @department_exists = IFNULL((
    SELECT COUNT(*) 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
      AND ser_menu_id = @master_data_menu_id
), 0);

-- Get the next order number for submenu
SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1 
    FROM cfg_tbl_sub_menu 
    WHERE ser_menu_id = @master_data_menu_id
), 1);

-- =====================================================
-- Step 4: Check table structure and fix if needed
-- =====================================================
-- First, let's verify the table has AUTO_INCREMENT set up correctly
-- If not, we'll need to fix it or use a different approach

-- Check if ser_sub_menu_id is AUTO_INCREMENT
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_sub_menu'
  AND COLUMN_NAME = 'ser_sub_menu_id';

-- =====================================================
-- Step 5: Insert Department Submenu
-- =====================================================
-- Using a stored procedure that explicitly calculates the ID
-- This avoids the AUTO_INCREMENT issue when WHERE filters out rows

DROP PROCEDURE IF EXISTS InsertDepartmentSubmenu;

DELIMITER $$

CREATE PROCEDURE InsertDepartmentSubmenu()
BEGIN
    DECLARE next_id INT DEFAULT NULL;
    
    -- Only proceed if conditions are met
    IF @master_data_menu_id IS NOT NULL 
       AND @department_exists = 0 
       AND NOT EXISTS (
           SELECT 1 
           FROM cfg_tbl_sub_menu 
           WHERE txt_sub_menu_name = 'Department' 
             AND ser_menu_id = @master_data_menu_id
       ) THEN
        
        -- Get the next available ID manually
        SET next_id = IFNULL((
            SELECT MAX(ser_sub_menu_id) + 1 
            FROM cfg_tbl_sub_menu
        ), 1);
        
        -- Insert with explicit ID to avoid AUTO_INCREMENT issues
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
        ) VALUES (
            next_id,
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
        );
    END IF;
END$$

DELIMITER ;

-- Execute the procedure
CALL InsertDepartmentSubmenu();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS InsertDepartmentSubmenu;

-- =====================================================
-- Step 3: Get the inserted Department Submenu ID
-- =====================================================
SET @department_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 4: Create Role Permissions for Department
-- This grants permissions to all existing roles
-- =====================================================
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
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @department_submenu_id 
      AND smr.ser_role_id = r.ser_role_id
  )
  AND @department_submenu_id IS NOT NULL;

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

