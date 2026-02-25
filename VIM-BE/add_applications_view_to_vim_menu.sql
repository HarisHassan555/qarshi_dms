-- =====================================================
-- SQL Script: Add Applications View Submenu to VIM Menu
-- Database: vim_3
-- Description: Adds "Applications View" as a submenu under VIM menu with permissions
-- =====================================================

USE vim_3;

-- Step 1: Find VIM menu ID
SET @vim_menu_id = (
    SELECT ser_menu_id 
    FROM cfg_tbl_menu 
    WHERE (txt_menu_name LIKE '%VIM%' 
           OR txt_menu_name LIKE '%vim%'
           OR txt_menu_name LIKE '%Vim%')
      AND bl_is_deleted = 0
      AND bln_status = 1
    ORDER BY ser_menu_id
    LIMIT 1
);

SELECT 
    @vim_menu_id AS 'VIM Menu ID',
    CASE 
        WHEN @vim_menu_id IS NULL THEN 'ERROR - VIM menu not found'
        ELSE 'SUCCESS - VIM menu found'
    END AS 'Status';

-- Step 2: Check if Applications View submenu already exists
SET @applications_view_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Applications View' 
      AND (@vim_menu_id IS NULL OR ser_menu_id = @vim_menu_id)
    LIMIT 1
);

SELECT 
    CASE 
        WHEN @applications_view_submenu_id IS NOT NULL THEN CONCAT('Applications View submenu EXISTS with ID: ', @applications_view_submenu_id)
        ELSE 'Applications View submenu does NOT exist - will be created'
    END AS 'Applications View Submenu Status';

-- Step 3: Get next order number
SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1 
    FROM cfg_tbl_sub_menu 
    WHERE ser_menu_id = @vim_menu_id
), 1);

-- Step 4: Get next submenu ID
SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);

-- Step 5: Insert Applications View submenu
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
    'Applications View',
    'applicationsview',
    @next_order,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    1,  -- Delete permission
    1,  -- Update permission
    0,  -- Approve permission
    NOW(),
    1   -- Created by user ID 1 (CHANGE THIS TO YOUR ADMIN USER ID)
WHERE @applications_view_submenu_id IS NULL
  AND @vim_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Applications View' 
      AND ser_menu_id = @vim_menu_id
  );

-- Step 6: Get the Applications View submenu ID (after insertion or if it already existed)
SET @applications_view_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Applications View' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

SELECT 
    @applications_view_submenu_id AS 'Applications View Submenu ID',
    CASE 
        WHEN @applications_view_submenu_id IS NOT NULL THEN 'SUCCESS - Applications View submenu ID found'
        ELSE 'ERROR - Applications View submenu ID not found'
    END AS 'Status';

-- Step 7: Insert role-based permissions for Applications View
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
    @applications_view_submenu_id,
    r.ser_role_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    1,  -- Delete permission
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
    WHERE smr.ser_sub_menu_id = @applications_view_submenu_id
      AND smr.ser_role_id = r.ser_role_id
  )
  AND @applications_view_submenu_id IS NOT NULL;

-- Step 8: Insert user-specific permissions for Applications View
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
    @applications_view_submenu_id,
    u.ser_role_id,
    u.ser_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    1,  -- Delete permission
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
  AND @applications_view_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @applications_view_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- Step 9: Update existing user-specific permissions (if any)
UPDATE cfg_tbl_sub_menu_role smr
SET 
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 1,
    bl_is_update = 1,
    bl_is_approve = 0,
    bl_is_enabled = 1,
    bl_is_all = 0,
    dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @applications_view_submenu_id
  AND smr.ser_user_id IS NOT NULL
  AND @applications_view_submenu_id IS NOT NULL;

-- Step 10: Verification queries
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
WHERE sm.txt_sub_menu_name = 'Applications View'
LIMIT 0, 1000;

SELECT 
    COUNT(*) AS 'Total User Permissions',
    COUNT(DISTINCT smr.ser_user_id) AS 'Unique Users',
    COUNT(DISTINCT smr.ser_role_id) AS 'Unique Roles'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Applications View'
  AND smr.ser_user_id IS NOT NULL
LIMIT 0, 1000;

SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
            WHERE sm.txt_sub_menu_name = 'Applications View'
              AND (m.txt_menu_name LIKE '%VIM%' OR m.txt_menu_name LIKE '%vim%' OR m.txt_menu_name LIKE '%Vim%')
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
              AND sm.bl_is_active = 1
        ) THEN '✅ Applications View is correctly configured - RESTART BACKEND and LOGOUT/LOGIN to see changes'
        ELSE '❌ Applications View needs to be fixed'
    END AS 'FINAL STATUS'
LIMIT 0, 1000;






