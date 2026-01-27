-- =====================================================
-- SQL Script: Remove Items from Master Data Menu
-- Database: vim_3
-- Description: This script removes the following items from Master Data menu:
--              - Country
--              - City
--              - Media House
--              - Product Category
--              - Product
--              - Tax Category
--              It soft-deletes the submenus and removes related permissions
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find Master Data Menu ID
-- =====================================================
SET @master_data_menu_id = (
    SELECT ser_menu_id 
    FROM cfg_tbl_menu 
    WHERE txt_menu_name = 'Master Data'
      AND bl_is_deleted = 0
      AND bln_status = 1
    LIMIT 1
);

-- Display Master Data Menu ID
SELECT 
    @master_data_menu_id AS 'Master Data Menu ID',
    CASE 
        WHEN @master_data_menu_id IS NULL THEN 'ERROR - Master Data menu not found'
        ELSE 'SUCCESS - Master Data menu found'
    END AS 'Status';

-- =====================================================
-- Step 2: Find Submenu IDs for items to be removed
-- =====================================================
-- Country
SET @country_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Country' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- City
SET @city_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'City' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- Media House
SET @media_house_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Media House' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- Product Category
SET @product_category_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Product Category' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- Product
SET @product_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Product' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- Tax Category
SET @tax_category_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Tax Category' 
      AND ser_menu_id = @master_data_menu_id
    LIMIT 1
);

-- Display found submenu IDs
SELECT 
    'Submenu IDs Found' AS Check_Type,
    @country_submenu_id AS 'Country ID',
    @city_submenu_id AS 'City ID',
    @media_house_submenu_id AS 'Media House ID',
    @product_category_submenu_id AS 'Product Category ID',
    @product_submenu_id AS 'Product ID',
    @tax_category_submenu_id AS 'Tax Category ID';

-- =====================================================
-- Step 3: Soft Delete Submenus (Set bl_is_deleted = 1)
-- =====================================================

-- Soft delete Country
UPDATE cfg_tbl_sub_menu 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @country_submenu_id
  AND @country_submenu_id IS NOT NULL;

-- Soft delete City
UPDATE cfg_tbl_sub_menu 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @city_submenu_id
  AND @city_submenu_id IS NOT NULL;

-- Soft delete Media House
UPDATE cfg_tbl_sub_menu 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @media_house_submenu_id
  AND @media_house_submenu_id IS NOT NULL;

-- Soft delete Product Category
UPDATE cfg_tbl_sub_menu 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @product_category_submenu_id
  AND @product_category_submenu_id IS NOT NULL;

-- Soft delete Product
UPDATE cfg_tbl_sub_menu 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @product_submenu_id
  AND @product_submenu_id IS NOT NULL;

-- Soft delete Tax Category
UPDATE cfg_tbl_sub_menu 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @tax_category_submenu_id
  AND @tax_category_submenu_id IS NOT NULL;

-- =====================================================
-- Step 4: Remove/Disable Related Permissions
-- =====================================================

-- Disable permissions for Country
UPDATE cfg_tbl_sub_menu_role 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    bl_is_enabled = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @country_submenu_id
  AND @country_submenu_id IS NOT NULL;

-- Disable permissions for City
UPDATE cfg_tbl_sub_menu_role 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    bl_is_enabled = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @city_submenu_id
  AND @city_submenu_id IS NOT NULL;

-- Disable permissions for Media House
UPDATE cfg_tbl_sub_menu_role 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    bl_is_enabled = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @media_house_submenu_id
  AND @media_house_submenu_id IS NOT NULL;

-- Disable permissions for Product Category
UPDATE cfg_tbl_sub_menu_role 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    bl_is_enabled = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @product_category_submenu_id
  AND @product_category_submenu_id IS NOT NULL;

-- Disable permissions for Product
UPDATE cfg_tbl_sub_menu_role 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    bl_is_enabled = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @product_submenu_id
  AND @product_submenu_id IS NOT NULL;

-- Disable permissions for Tax Category
UPDATE cfg_tbl_sub_menu_role 
SET 
    bl_is_deleted = 1,
    bl_is_active = 0,
    bln_status = 0,
    bl_is_enabled = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @tax_category_submenu_id
  AND @tax_category_submenu_id IS NOT NULL;

-- =====================================================
-- Step 5: Verification Queries
-- =====================================================

-- Verify removed submenus are marked as deleted
SELECT 
    'Removed Submenus Status' AS Check_Type,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.bl_is_deleted AS 'Is Deleted',
    sm.bl_is_active AS 'Is Active',
    sm.bln_status AS 'Status'
FROM cfg_tbl_sub_menu sm
WHERE sm.ser_sub_menu_id IN (
    @country_submenu_id,
    @city_submenu_id,
    @media_house_submenu_id,
    @product_category_submenu_id,
    @product_submenu_id,
    @tax_category_submenu_id
)
AND sm.ser_menu_id = @master_data_menu_id;

-- Verify remaining active submenus in Master Data
SELECT 
    'Remaining Active Master Data Submenus' AS Check_Type,
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Name',
    sm.txt_sub_menu_url AS 'URL',
    sm.int_sub_menu_order AS 'Order',
    sm.bl_is_deleted AS 'Is Deleted',
    sm.bl_is_active AS 'Is Active'
FROM cfg_tbl_sub_menu sm
WHERE sm.ser_menu_id = @master_data_menu_id
  AND sm.bl_is_deleted = 0
  AND sm.bln_status = 1
ORDER BY sm.int_sub_menu_order;

-- Count disabled permissions
SELECT 
    'Disabled Permissions Count' AS Check_Type,
    COUNT(*) AS 'Total Disabled Permissions',
    COUNT(CASE WHEN ser_sub_menu_id = @country_submenu_id THEN 1 END) AS 'Country Permissions',
    COUNT(CASE WHEN ser_sub_menu_id = @city_submenu_id THEN 1 END) AS 'City Permissions',
    COUNT(CASE WHEN ser_sub_menu_id = @media_house_submenu_id THEN 1 END) AS 'Media House Permissions',
    COUNT(CASE WHEN ser_sub_menu_id = @product_category_submenu_id THEN 1 END) AS 'Product Category Permissions',
    COUNT(CASE WHEN ser_sub_menu_id = @product_submenu_id THEN 1 END) AS 'Product Permissions',
    COUNT(CASE WHEN ser_sub_menu_id = @tax_category_submenu_id THEN 1 END) AS 'Tax Category Permissions'
FROM cfg_tbl_sub_menu_role
WHERE ser_sub_menu_id IN (
    @country_submenu_id,
    @city_submenu_id,
    @media_house_submenu_id,
    @product_category_submenu_id,
    @product_submenu_id,
    @tax_category_submenu_id
)
AND bl_is_deleted = 1;

-- =====================================================
-- Step 6: Final Status Check
-- =====================================================
SELECT 
    'FINAL STATUS' AS Check_Type,
    CASE 
        WHEN NOT EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            WHERE sm.ser_menu_id = @master_data_menu_id
              AND sm.txt_sub_menu_name IN ('Country', 'City', 'Media House', 'Product Category', 'Product', 'Tax Category')
              AND sm.bl_is_deleted = 0
        )
        THEN '✅ All specified items have been removed from Master Data menu'
        ELSE '❌ Some items may still be active - check verification queries above'
    END AS Status;

-- =====================================================
-- Notes:
-- 1. After running this script, RESTART the backend application
-- 2. Clear browser cache and refresh the frontend
-- 3. The removed items will no longer appear in:
--    - Master Data sidebar menu
--    - Permission page (/role) for assigning controls
-- 4. This script uses soft delete - items are marked as deleted but not physically removed
-- 5. To restore these items, you would need to set bl_is_deleted = 0 and restore permissions
-- =====================================================

