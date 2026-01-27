-- =====================================================
-- SQL Script: Update VIM Menu to Velocity and Remove Submenus
-- Database: vim_3
-- Description: This script:
--              1. Renames VIM menu to "Velocity"
--              2. Removes all submenus EXCEPT: Application, Applications View, Form Builder
--              3. Soft-deletes removed submenus and disables related permissions
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
-- Step 2: Rename VIM Menu to "Velocity"
-- =====================================================
UPDATE cfg_tbl_menu 
SET 
    txt_menu_name = 'Velocity',
    dte_modified_date = NOW()
WHERE ser_menu_id = @vim_menu_id
  AND @vim_menu_id IS NOT NULL;

SELECT 
    'Menu Renamed' AS Check_Type,
    ser_menu_id AS 'Menu ID',
    txt_menu_name AS 'Menu Name',
    CASE 
        WHEN txt_menu_name = 'Velocity' THEN '✅ Successfully renamed to Velocity'
        ELSE '❌ Rename failed'
    END AS 'Status'
FROM cfg_tbl_menu
WHERE ser_menu_id = @vim_menu_id;

-- =====================================================
-- Step 3: Get all submenu IDs that need to be removed
-- (All except: Application, Applications View, Form Builder)
-- =====================================================

-- Get IDs of submenus to KEEP (these will NOT be deleted)
SET @application_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Application' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

SET @applications_view_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Applications View' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

SET @form_builder_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Form Builder' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

-- Display submenus to keep
SELECT 
    'Submenus to Keep' AS Check_Type,
    @application_submenu_id AS 'Application ID',
    @applications_view_submenu_id AS 'Applications View ID',
    @form_builder_submenu_id AS 'Form Builder ID';

-- =====================================================
-- Step 4: Soft Delete All Other Submenus
-- =====================================================
-- Soft delete all submenus except the three we want to keep
UPDATE cfg_tbl_sub_menu 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    dte_modified_date = NOW()
WHERE ser_menu_id = @vim_menu_id
  AND ser_sub_menu_id NOT IN (
    COALESCE(@application_submenu_id, -1),
    COALESCE(@applications_view_submenu_id, -1),
    COALESCE(@form_builder_submenu_id, -1)
  )
  AND @vim_menu_id IS NOT NULL;

-- Display count of deleted submenus
SELECT 
    'Deleted Submenus Count' AS Check_Type,
    COUNT(*) AS 'Total Deleted',
    GROUP_CONCAT(txt_sub_menu_name ORDER BY txt_sub_menu_name SEPARATOR ', ') AS 'Deleted Submenu Names'
FROM cfg_tbl_sub_menu
WHERE ser_menu_id = @vim_menu_id
  AND bl_is_deleted = 1
  AND ser_sub_menu_id NOT IN (
    COALESCE(@application_submenu_id, -1),
    COALESCE(@applications_view_submenu_id, -1),
    COALESCE(@form_builder_submenu_id, -1)
  );

-- =====================================================
-- Step 5: Disable Permissions for Deleted Submenus
-- =====================================================
-- Get all deleted submenu IDs
UPDATE cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
SET 
    smr.bl_is_deleted = 1,
    smr.bl_is_active = 0,
    smr.bln_status = 0,
    smr.bl_is_enabled = 0,
    smr.dte_modified_date = NOW()
WHERE sm.ser_menu_id = @vim_menu_id
  AND sm.bl_is_deleted = 1
  AND sm.ser_sub_menu_id NOT IN (
    COALESCE(@application_submenu_id, -1),
    COALESCE(@applications_view_submenu_id, -1),
    COALESCE(@form_builder_submenu_id, -1)
  )
  AND @vim_menu_id IS NOT NULL;

-- Display count of disabled permissions
SELECT 
    'Disabled Permissions Count' AS Check_Type,
    COUNT(*) AS 'Total Disabled Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.ser_menu_id = @vim_menu_id
  AND sm.bl_is_deleted = 1
  AND smr.bl_is_deleted = 1;

-- =====================================================
-- Step 6: Verification Queries
-- =====================================================

-- Verify menu name change
SELECT 
    'Menu Verification' AS Check_Type,
    ser_menu_id AS 'Menu ID',
    txt_menu_name AS 'Menu Name',
    bl_is_deleted AS 'Is Deleted',
    bl_is_active AS 'Is Active',
    bln_status AS 'Status'
FROM cfg_tbl_menu
WHERE ser_menu_id = @vim_menu_id;

-- Verify remaining active submenus (should only be 3)
SELECT 
    'Remaining Active Submenus' AS Check_Type,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.txt_sub_menu_url AS 'URL',
    sm.int_sub_menu_order AS 'Order',
    sm.bl_is_deleted AS 'Is Deleted',
    sm.bl_is_active AS 'Is Active',
    sm.bln_status AS 'Status'
FROM cfg_tbl_sub_menu sm
WHERE sm.ser_menu_id = @vim_menu_id
  AND sm.bl_is_deleted = 0
  AND sm.bln_status = 1
ORDER BY sm.int_sub_menu_order;

-- Verify deleted submenus
SELECT 
    'Deleted Submenus' AS Check_Type,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.bl_is_deleted AS 'Is Deleted',
    sm.bl_is_active AS 'Is Active'
FROM cfg_tbl_sub_menu sm
WHERE sm.ser_menu_id = @vim_menu_id
  AND sm.bl_is_deleted = 1
ORDER BY sm.txt_sub_menu_name;

-- =====================================================
-- Step 7: Final Status Check
-- =====================================================
SELECT 
    'FINAL STATUS' AS Check_Type,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM cfg_tbl_menu m
            WHERE m.ser_menu_id = @vim_menu_id
              AND m.txt_menu_name = 'Velocity'
              AND m.bl_is_deleted = 0
        )
        AND (
            SELECT COUNT(*) 
            FROM cfg_tbl_sub_menu sm
            WHERE sm.ser_menu_id = @vim_menu_id
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
        ) = 3
        AND EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            WHERE sm.ser_menu_id = @vim_menu_id
              AND sm.txt_sub_menu_name = 'Application'
              AND sm.bl_is_deleted = 0
        )
        AND EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            WHERE sm.ser_menu_id = @vim_menu_id
              AND sm.txt_sub_menu_name = 'Applications View'
              AND sm.bl_is_deleted = 0
        )
        AND EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            WHERE sm.ser_menu_id = @vim_menu_id
              AND sm.txt_sub_menu_name = 'Form Builder'
              AND sm.bl_is_deleted = 0
        )
        THEN '✅ VIM menu successfully renamed to Velocity with only Application, Applications View, and Form Builder remaining'
        ELSE '❌ Update needs attention - check the verification queries above'
    END AS Status;

-- =====================================================
-- Notes:
-- 1. After running this script, RESTART the backend application
-- 2. Clear browser cache and refresh the frontend
-- 3. The menu will now appear as "Velocity" with only 3 submenus:
--    - Application
--    - Applications View
--    - Form Builder
-- 4. This script uses soft delete - removed items are marked as deleted but not physically removed
-- 5. To restore removed items, you would need to set bl_is_deleted = 0 and restore permissions
-- =====================================================

