-- =====================================================
-- Helper Script: Find Master Data Menu ID
-- Database: vim_3
-- Description: This script helps you find the Master Data menu ID
--              Use this before running the department menu scripts
-- =====================================================

USE vim_3;

-- Find all menus that might be Master Data
SELECT 
    ser_menu_id AS 'Menu ID',
    txt_menu_name AS 'Menu Name',
    txt_menu_icons AS 'Icon',
    bl_is_active AS 'Active',
    bln_status AS 'Status',
    bl_is_deleted AS 'Deleted'
FROM cfg_tbl_menu
WHERE txt_menu_name LIKE '%Master%'
   OR txt_menu_name LIKE '%master%'
   OR txt_menu_name LIKE '%MASTER%'
ORDER BY ser_menu_id;

-- Show all menus (in case Master Data has a different name)
SELECT 
    ser_menu_id AS 'Menu ID',
    txt_menu_name AS 'Menu Name',
    txt_menu_icons AS 'Icon'
FROM cfg_tbl_menu
WHERE bl_is_deleted = 0
  AND bln_status = 1
ORDER BY txt_menu_name;

-- Show existing submenus under potential Master Data menus
SELECT 
    m.ser_menu_id AS 'Menu ID',
    m.txt_menu_name AS 'Menu Name',
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Submenu Name',
    sm.txt_sub_menu_url AS 'URL',
    sm.int_sub_menu_order AS 'Order'
FROM cfg_tbl_menu m
LEFT JOIN cfg_tbl_sub_menu sm ON m.ser_menu_id = sm.ser_menu_id
WHERE m.txt_menu_name LIKE '%Master%'
   OR m.txt_menu_id IN (
       SELECT DISTINCT ser_menu_id 
       FROM cfg_tbl_sub_menu 
       WHERE txt_sub_menu_name IN ('Country', 'City', 'Product', 'Product Category', 'Tax Category')
   )
ORDER BY m.ser_menu_id, sm.int_sub_menu_order;

-- Find the menu that contains Country, City, Product submenus (likely Master Data)
SELECT DISTINCT
    m.ser_menu_id AS 'Master Data Menu ID',
    m.txt_menu_name AS 'Menu Name'
FROM cfg_tbl_menu m
INNER JOIN cfg_tbl_sub_menu sm ON m.ser_menu_id = sm.ser_menu_id
WHERE sm.txt_sub_menu_name IN ('Country', 'City', 'Product', 'Product Category', 'Tax Category')
  AND m.bl_is_deleted = 0
  AND m.bln_status = 1
LIMIT 1;

