-- =====================================================
-- SQL Script: Add Form Builder Submenu to VIM Menu
-- Database: vim_3
-- Description: This script ensures Form Builder submenu is properly
--              added to VIM menu with all permissions
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find VIM Menu ID
-- =====================================================
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

-- Display VIM Menu ID
SELECT 
    @vim_menu_id AS 'VIM Menu ID',
    CASE 
        WHEN @vim_menu_id IS NULL THEN 'ERROR - VIM menu not found'
        ELSE 'SUCCESS - VIM menu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Check if Form Builder submenu already exists
-- =====================================================
SET @form_builder_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Form Builder' 
      AND (@vim_menu_id IS NULL OR ser_menu_id = @vim_menu_id)
    LIMIT 1
);

SELECT 
    CASE 
        WHEN @form_builder_submenu_id IS NOT NULL THEN CONCAT('Form Builder submenu EXISTS with ID: ', @form_builder_submenu_id)
        ELSE 'Form Builder submenu does NOT exist - will be created'
    END AS 'Form Builder Submenu Status';

-- =====================================================
-- Step 3: Get next order number for Form Builder
-- =====================================================
SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1 
    FROM cfg_tbl_sub_menu 
    WHERE ser_menu_id = @vim_menu_id
), 1);

-- =====================================================
-- Step 4: Insert or Update Form Builder Submenu
-- =====================================================
-- Get the next available ID if we need to insert
SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);

-- Insert Form Builder submenu if it doesn't exist
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
    'Form Builder',
    'formbuilder',
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
WHERE @form_builder_submenu_id IS NULL
  AND @vim_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Form Builder' 
      AND ser_menu_id = @vim_menu_id
  );

-- Update Form Builder submenu if it exists
UPDATE cfg_tbl_sub_menu 
SET 
    ser_menu_id = @vim_menu_id,
    txt_sub_menu_url = 'formbuilder',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @form_builder_submenu_id
  AND @vim_menu_id IS NOT NULL
  AND @form_builder_submenu_id IS NOT NULL;

-- =====================================================
-- Step 5: Get Form Builder Submenu ID (after insert/update)
-- =====================================================
SET @form_builder_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Form Builder' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

SELECT 
    @form_builder_submenu_id AS 'Form Builder Submenu ID',
    CASE 
        WHEN @form_builder_submenu_id IS NOT NULL THEN 'SUCCESS - Form Builder submenu ID found'
        ELSE 'ERROR - Form Builder submenu ID not found'
    END AS 'Status';

-- =====================================================
-- Step 6: Create Role Permissions for Form Builder
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
    @form_builder_submenu_id,
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
    WHERE smr.ser_sub_menu_id = @form_builder_submenu_id
      AND smr.ser_role_id = r.ser_role_id
  )
  AND @form_builder_submenu_id IS NOT NULL;

-- =====================================================
-- Step 7: Create User-Specific Permissions for Form Builder
-- =====================================================
-- This is important - the backend requires user-specific permissions
-- Using the same approach as Department permissions
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
    @form_builder_submenu_id,
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
  AND @form_builder_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @form_builder_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- =====================================================
-- Step 8: Update Existing User Permissions (if any)
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
WHERE smr.ser_sub_menu_id = @form_builder_submenu_id
  AND smr.ser_user_id IS NOT NULL
  AND @form_builder_submenu_id IS NOT NULL;

-- =====================================================
-- Step 9: Verification Queries
-- =====================================================
-- Verify Form Builder submenu was created/updated
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
WHERE sm.txt_sub_menu_name = 'Form Builder';

-- Verify role permissions were created
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
WHERE sm.txt_sub_menu_name = 'Form Builder'
ORDER BY r.txt_role_name;

-- Verify user-specific permissions count
SELECT 
    COUNT(*) AS 'Total User Permissions',
    COUNT(DISTINCT smr.ser_user_id) AS 'Unique Users',
    COUNT(DISTINCT smr.ser_role_id) AS 'Unique Roles'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Form Builder'
  AND smr.ser_user_id IS NOT NULL;

-- Final status
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
            WHERE sm.txt_sub_menu_name = 'Form Builder'
              AND (m.txt_menu_name LIKE '%VIM%' OR m.txt_menu_name LIKE '%vim%' OR m.txt_menu_name LIKE '%Vim%')
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
              AND sm.bl_is_active = 1
        ) THEN '✅ Form Builder is correctly configured - RESTART BACKEND to see changes'
        ELSE '❌ Form Builder needs to be fixed'
    END AS 'FINAL STATUS';









