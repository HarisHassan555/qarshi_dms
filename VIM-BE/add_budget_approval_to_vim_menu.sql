-- =====================================================
-- SQL Script: Add Budget Approval to Master Data Menu
-- Database: vim_3
-- Description: Adds "Budget Approval" to the Master Data menu and assigns permissions.
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

-- Check if found
SELECT 
    @master_data_menu_id AS 'Master Data Menu ID',
    CASE 
        WHEN @master_data_menu_id IS NULL THEN 'NOT FOUND - Please set manually'
        ELSE 'Found'
    END AS 'Menu Status';

-- =====================================================
-- Step 2: Insert Budget Approval Submenu
-- =====================================================

-- Get the next order number for submenu
SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1 
    FROM cfg_tbl_sub_menu 
    WHERE ser_menu_id = @master_data_menu_id
), 1);

-- Drop procedure if exists (clean start)
DROP PROCEDURE IF EXISTS InsertBudgetApprovalSubmenu;

DELIMITER $$

CREATE PROCEDURE InsertBudgetApprovalSubmenu()
BEGIN
    DECLARE next_id INT DEFAULT NULL;
    
    -- Only proceed if Master Data menu is found and "Budget Approval" doesn't exist yet
    IF @master_data_menu_id IS NOT NULL 
       AND NOT EXISTS (
           SELECT 1 
           FROM cfg_tbl_sub_menu 
           WHERE txt_sub_menu_name = 'Budget Approval' 
             AND ser_menu_id = @master_data_menu_id
       ) THEN
        
        -- Get the next available ID manually (to avoid AI issues)
        SET next_id = IFNULL((
            SELECT MAX(ser_sub_menu_id) + 1 
            FROM cfg_tbl_sub_menu
        ), 1);
        
        -- Insert Budget Approval submenu
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
            'Budget Approval',
            '/budget-approval', -- Matches the Angular route
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
            1   -- Created by user ID 1
        );
    END IF;
END$$

DELIMITER ;

-- Execute and drop
CALL InsertBudgetApprovalSubmenu();
DROP PROCEDURE IF EXISTS InsertBudgetApprovalSubmenu;


-- =====================================================
-- Step 3: Insert Budget Approval View Submenu
-- =====================================================

-- Get the next order number for the View submenu
SET @next_order_view = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1 
    FROM cfg_tbl_sub_menu 
    WHERE ser_menu_id = @master_data_menu_id
), 1);

-- Drop procedure if exists
DROP PROCEDURE IF EXISTS InsertBudgetApprovalViewSubmenu;

DELIMITER $$

CREATE PROCEDURE InsertBudgetApprovalViewSubmenu()
BEGIN
    DECLARE next_id INT DEFAULT NULL;
    
    -- Only proceed if Master Data menu is found and "Budget Approval View" doesn't exist yet
    IF @master_data_menu_id IS NOT NULL 
       AND NOT EXISTS (
           SELECT 1 
           FROM cfg_tbl_sub_menu 
           WHERE txt_sub_menu_name = 'Budget Approval View' 
             AND ser_menu_id = @master_data_menu_id
       ) THEN
        
        -- Get the next available ID manually
        SET next_id = IFNULL((
            SELECT MAX(ser_sub_menu_id) + 1 
            FROM cfg_tbl_sub_menu
        ), 1);
        
        -- Insert Budget Approval View submenu
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
            'Budget Approval View',
            '/budgetapprovalview', -- Matches the Angular route
            @next_order_view,
            1,  -- Active
            1,  -- Status active
            0,  -- Not deleted
            1,  -- View permission
            0,  -- Add permission (Read only view usually)
            0,  -- Delete permission
            0,  -- Update permission
            0,  -- Approve permission
            NOW(),
            1   -- Created by user ID 1
        );
    END IF;
END$$

DELIMITER ;

-- Execute and drop
CALL InsertBudgetApprovalViewSubmenu();
DROP PROCEDURE IF EXISTS InsertBudgetApprovalViewSubmenu;


-- =====================================================
-- Step 4: Create Role Permissions
-- Grants permissions to all existing roles for both new submenus
-- =====================================================

-- 4a. Budget Approval Permissions
SET @budget_approval_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Budget Approval' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

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
    @budget_approval_id,
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
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @budget_approval_id 
      AND smr.ser_role_id = r.ser_role_id
  )
  AND @budget_approval_id IS NOT NULL;


-- 4b. Budget Approval View Permissions
SET @budget_approval_view_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Budget Approval View' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

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
    @budget_approval_view_id,
    r.ser_role_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    0,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1   -- Created by user ID 1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @budget_approval_view_id 
      AND smr.ser_role_id = r.ser_role_id
  )
  AND @budget_approval_view_id IS NOT NULL;


-- =====================================================
-- Verification
-- =====================================================
SELECT 
    'Submenus Created' AS Status,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.txt_sub_menu_url AS 'URL',
    m.txt_menu_name AS 'Parent Menu'
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name IN ('Budget Approval', 'Budget Approval View');
