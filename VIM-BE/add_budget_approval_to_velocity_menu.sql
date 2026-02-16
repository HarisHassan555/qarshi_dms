-- =====================================================
-- SQL Script: Add Budget Approval Submenu to Velocity Menu
-- Database: vim_3
-- Description: Adds "Budget Approval" submenu under Velocity (VIM)
--              with role and user permissions
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find VIM/Velocity Menu ID
-- =====================================================
SET @vim_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE (txt_menu_name LIKE '%VIM%'
           OR txt_menu_name LIKE '%vim%'
           OR txt_menu_name LIKE '%Vim%'
           OR txt_menu_name LIKE '%Velocity%'
           OR txt_menu_name LIKE '%velocity%'
           OR txt_menu_name = 'Velocity')
      AND bl_is_deleted = 0
      AND bln_status = 1
    ORDER BY ser_menu_id
    LIMIT 1
);

SELECT
    @vim_menu_id AS 'VIM/Velocity Menu ID',
    CASE
        WHEN @vim_menu_id IS NULL THEN 'ERROR - VIM/Velocity menu not found'
        ELSE 'SUCCESS - VIM/Velocity menu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Get next order number for submenus
-- =====================================================
SET @base_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @vim_menu_id
), 1);

-- =====================================================
-- Step 3: Create or Update Budget Approval Submenu
-- =====================================================
SET @budget_approval_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Budget Approval'
      AND (@vim_menu_id IS NULL OR ser_menu_id = @vim_menu_id)
    LIMIT 1
);

-- Update existing submenu if it exists (even if soft-deleted)
UPDATE cfg_tbl_sub_menu
SET
    ser_menu_id = @vim_menu_id,
    txt_sub_menu_url = 'budget-approval',
    int_sub_menu_order = @base_order,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @budget_approval_submenu_id
  AND @vim_menu_id IS NOT NULL
  AND @budget_approval_submenu_id IS NOT NULL;

-- Insert new submenu if it doesn't exist
SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);

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
    @vim_menu_id,
    'Budget Approval',
    'budget-approval',
    @base_order,
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
WHERE @budget_approval_submenu_id IS NULL
  AND @vim_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Budget Approval'
      AND ser_menu_id = @vim_menu_id
  );

SET @budget_approval_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Budget Approval'
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 4: Create Role-Based Permissions for Budget Approval
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
    @budget_approval_submenu_id,
    r.ser_role_id,
    1,
    1,
    0,
    1,
    1,
    0,
    1,
    0,
    1,
    0,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @budget_approval_submenu_id
      AND smr.ser_role_id = r.ser_role_id
      AND smr.ser_user_id IS NULL
  )
  AND @budget_approval_submenu_id IS NOT NULL;

-- =====================================================
-- Step 5: Create User-Specific Permissions for Budget Approval
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
    @budget_approval_submenu_id,
    u.ser_role_id,
    u.ser_user_id,
    1,
    1,
    0,
    1,
    1,
    0,
    1,
    0,
    1,
    0,
    NOW(),
    1
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND u.ser_role_id IS NOT NULL
  AND @budget_approval_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @budget_approval_submenu_id
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- =====================================================
-- Step 6: Update Existing Permissions (if any)
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
WHERE smr.ser_sub_menu_id = @budget_approval_submenu_id
  AND @budget_approval_submenu_id IS NOT NULL;

-- =====================================================
-- Step 7: Verification
-- =====================================================
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
WHERE sm.txt_sub_menu_name = 'Budget Approval'
  AND sm.bl_is_deleted = 0;

SELECT
    sm.txt_sub_menu_name AS 'Submenu',
    COUNT(DISTINCT smr.ser_role_id) AS 'Roles with Permissions',
    COUNT(DISTINCT smr.ser_user_id) AS 'Users with Permissions',
    COUNT(*) AS 'Total Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Budget Approval'
GROUP BY sm.txt_sub_menu_name;

SELECT
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM cfg_tbl_sub_menu sm
            INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
            WHERE sm.txt_sub_menu_name = 'Budget Approval'
              AND (m.txt_menu_name LIKE '%VIM%' OR m.txt_menu_name LIKE '%vim%' OR m.txt_menu_name LIKE '%Vim%'
                   OR m.txt_menu_name LIKE '%Velocity%' OR m.txt_menu_name LIKE '%velocity%' OR m.txt_menu_name = 'Velocity')
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
              AND sm.bl_is_active = 1
        ) THEN 'Budget Approval menu item is correctly configured - RESTART BACKEND and LOGOUT/LOGIN to see changes'
        ELSE 'Budget Approval menu item needs to be fixed'
    END AS 'FINAL STATUS';
