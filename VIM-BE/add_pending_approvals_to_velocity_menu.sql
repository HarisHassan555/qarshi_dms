-- =====================================================
-- SQL Script: Add Pending Approvals Submenu to Velocity Menu
-- Database: vim_3
-- Description: Adds "Pending Approvals" submenu under Velocity
--              with role and user permissions for visibility
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find Velocity Menu ID
-- =====================================================
SET @velocity_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE (txt_menu_name = 'Velocity'
           OR txt_menu_name LIKE '%Velocity%'
           OR txt_menu_name LIKE '%VIM%')
      AND bl_is_deleted = 0
      AND bln_status = 1
    ORDER BY ser_menu_id
    LIMIT 1
);

SELECT
    @velocity_menu_id AS 'Velocity Menu ID',
    CASE
        WHEN @velocity_menu_id IS NULL THEN 'ERROR - Velocity menu not found'
        ELSE 'SUCCESS - Velocity menu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Get next order number for submenus
-- =====================================================
SET @base_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
), 1);

-- =====================================================
-- Step 3: Create or Update Pending Approvals Submenu
-- =====================================================
SET @pending_approvals_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Pending Approvals'
      AND (@velocity_menu_id IS NULL OR ser_menu_id = @velocity_menu_id)
    LIMIT 1
);

-- Update existing submenu if it exists (restore if soft-deleted)
UPDATE cfg_tbl_sub_menu
SET
    ser_menu_id = @velocity_menu_id,
    txt_sub_menu_url = 'pending-approvals',
    int_sub_menu_order = @base_order,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 0,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 1,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @pending_approvals_submenu_id
  AND @velocity_menu_id IS NOT NULL
  AND @pending_approvals_submenu_id IS NOT NULL;

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
    @velocity_menu_id,
    'Pending Approvals',
    'pending-approvals',
    @base_order,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    0,  -- Add permission
    0,  -- Delete permission
    1,  -- Update permission
    1,  -- Approve permission
    NOW(),
    1   -- Created by user ID 1
WHERE @pending_approvals_submenu_id IS NULL
  AND @velocity_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Pending Approvals'
      AND ser_menu_id = @velocity_menu_id
  );

-- Re-fetch ID if it was just inserted
SET @pending_approvals_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Pending Approvals'
      AND ser_menu_id = @velocity_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 4: Create Role-Based Permissions for Pending Approvals
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
    @pending_approvals_submenu_id,
    r.ser_role_id,
    1,
    1,
    0,
    1,
    0,
    0,
    1,
    1,
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
    WHERE smr.ser_sub_menu_id = @pending_approvals_submenu_id
      AND smr.ser_role_id = r.ser_role_id
      AND smr.ser_user_id IS NULL
  )
  AND @pending_approvals_submenu_id IS NOT NULL;

-- =====================================================
-- Step 5: Create User-Specific Permissions for Pending Approvals
-- =====================================================
-- Backend logic often checks for user-specific entries in sub_menu_role
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
    @pending_approvals_submenu_id,
    u.ser_role_id,
    u.ser_user_id,
    1,
    1,
    0,
    1,
    0,
    0,
    1,
    1,
    1,
    0,
    NOW(),
    1
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND u.ser_role_id IS NOT NULL
  AND @pending_approvals_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @pending_approvals_submenu_id
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
    bl_is_add = 0,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 1,
    bl_is_enabled = 1,
    bl_is_all = 0,
    dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @pending_approvals_submenu_id
  AND @pending_approvals_submenu_id IS NOT NULL;

-- =====================================================
-- Step 7: Final Verification
-- =====================================================
SELECT
    sm.txt_sub_menu_name AS 'Submenu Name',
    sm.txt_sub_menu_url AS 'URL',
    m.txt_menu_name AS 'Parent Menu',
    sm.bl_is_active AS 'Active',
    sm.bl_is_deleted AS 'Deleted'
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Pending Approvals'
  AND sm.ser_menu_id = @velocity_menu_id;

SELECT
    'SUCCESS' AS 'Status',
    'Pending Approvals menu item configured. RESTART BACKEND and refresh frontend.' AS 'Message';
