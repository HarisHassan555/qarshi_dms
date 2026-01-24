-- =====================================================
-- SQL Script: Verify and Fix Department Menu Setup
-- Database: vim_3
-- Description: This script verifies Department submenu exists and fixes any issues
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Verify Department Submenu Exists
-- =====================================================
SELECT 
    'Department Submenu Status' AS Check_Type,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.txt_sub_menu_url AS 'URL',
    sm.ser_menu_id AS 'Menu ID',
    m.txt_menu_name AS 'Parent Menu',
    sm.int_sub_menu_order AS 'Order',
    sm.bl_is_active AS 'Active',
    sm.bln_status AS 'Status',
    sm.bl_is_deleted AS 'Deleted',
    sm.bl_is_view AS 'Can View',
    sm.bl_is_add AS 'Can Add',
    sm.bl_is_update AS 'Can Update'
FROM cfg_tbl_sub_menu sm
LEFT JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

-- =====================================================
-- Step 2: Verify it's linked to Master Data Menu
-- =====================================================
SELECT 
    'Menu Link Verification' AS Check_Type,
    CASE 
        WHEN m.txt_menu_name LIKE '%Master%' OR m.txt_menu_name LIKE '%master%' THEN 'CORRECT - Linked to Master Data'
        WHEN m.txt_menu_name IS NULL THEN 'ERROR - Menu not found'
        ELSE CONCAT('WARNING - Linked to: ', m.txt_menu_name)
    END AS Status,
    sm.ser_menu_id AS 'Menu ID',
    m.txt_menu_name AS 'Menu Name'
FROM cfg_tbl_sub_menu sm
LEFT JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

-- =====================================================
-- Step 3: Fix Department Submenu if needed
-- =====================================================
-- Get Master Data Menu ID
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

-- Update Department submenu to ensure it's correctly linked and active
-- Using ser_sub_menu_id in WHERE clause to satisfy safe update mode
UPDATE cfg_tbl_sub_menu 
SET 
    ser_menu_id = @master_data_menu_id,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_update = 1,
    bl_is_delete = 0,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE txt_sub_menu_name = 'Department'
  AND ser_sub_menu_id = (SELECT ser_sub_menu_id FROM (SELECT ser_sub_menu_id FROM cfg_tbl_sub_menu WHERE txt_sub_menu_name = 'Department' LIMIT 1) AS temp)
  AND @master_data_menu_id IS NOT NULL;

-- =====================================================
-- Step 4: Verify Role Permissions
-- =====================================================
SELECT 
    'Role Permissions' AS Check_Type,
    COUNT(*) AS 'Total Permissions',
    COUNT(CASE WHEN smr.bl_is_enabled = 1 THEN 1 END) AS 'Enabled Permissions',
    COUNT(CASE WHEN smr.bl_is_view = 1 THEN 1 END) AS 'Can View',
    COUNT(CASE WHEN smr.bl_is_add = 1 THEN 1 END) AS 'Can Add',
    COUNT(CASE WHEN smr.bl_is_update = 1 THEN 1 END) AS 'Can Update'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

-- =====================================================
-- Step 5: List all submenus under Master Data for comparison
-- =====================================================
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
WHERE (m.txt_menu_name LIKE '%Master Data%' 
       OR m.txt_menu_name LIKE '%Master%'
       OR m.txt_menu_name LIKE '%master%')
  AND m.bl_is_deleted = 0
  AND m.bln_status = 1
ORDER BY sm.int_sub_menu_order;

-- =====================================================
-- Step 6: Final Verification
-- =====================================================
SELECT 
    'FINAL STATUS' AS Check_Type,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
            WHERE sm.txt_sub_menu_name = 'Department'
              AND (m.txt_menu_name LIKE '%Master%' OR m.txt_menu_name LIKE '%master%')
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
              AND sm.bl_is_active = 1
        ) THEN '✅ Department is correctly configured'
        ELSE '❌ Department needs to be fixed'
    END AS Status;

