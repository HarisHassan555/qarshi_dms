-- =====================================================
-- SQL Script: Remove Shopping Cart Menu Items from VIM Menu
-- Database: vim_3
-- Description: Soft-deletes shopping cart pages as submenus:
--              - Product Catalog
--              - Shopping Cart
--              - Checkout
--              - Receipt
--              - Feedback
--              Also disables related submenu role permissions.
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
-- Step 2: Collect submenu IDs to remove
-- =====================================================
DROP TEMPORARY TABLE IF EXISTS tmp_cart_submenus;
CREATE TEMPORARY TABLE tmp_cart_submenus (
    ser_sub_menu_id INT PRIMARY KEY
);

INSERT INTO tmp_cart_submenus (ser_sub_menu_id)
SELECT sm.ser_sub_menu_id
FROM cfg_tbl_sub_menu sm
WHERE sm.ser_menu_id = @vim_menu_id
  AND sm.txt_sub_menu_name IN ('Product Catalog', 'Shopping Cart', 'Checkout', 'Receipt', 'Feedback')
  AND sm.bl_is_deleted = 0;

-- =====================================================
-- Step 3: Soft-delete the submenus
-- =====================================================
UPDATE cfg_tbl_sub_menu sm
SET
    sm.bl_is_active = 0,
    sm.bln_status = 0,
    sm.bl_is_deleted = 1,
    sm.dte_modified_date = NOW()
WHERE sm.ser_sub_menu_id IN (SELECT ser_sub_menu_id FROM tmp_cart_submenus);

-- =====================================================
-- Step 4: Disable related submenu role permissions
-- =====================================================
UPDATE cfg_tbl_sub_menu_role smr
SET
    smr.bl_is_active = 0,
    smr.bln_status = 0,
    smr.bl_is_deleted = 1,
    smr.bl_is_enabled = 0,
    smr.dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id IN (SELECT ser_sub_menu_id FROM tmp_cart_submenus);

-- =====================================================
-- Step 5: Verification
-- =====================================================
SELECT
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Submenu Name',
    sm.txt_sub_menu_url AS 'URL',
    sm.bl_is_active AS 'Active',
    sm.bln_status AS 'Status',
    sm.bl_is_deleted AS 'Deleted'
FROM cfg_tbl_sub_menu sm
WHERE sm.txt_sub_menu_name IN ('Product Catalog', 'Shopping Cart', 'Checkout', 'Receipt', 'Feedback')
  AND sm.ser_menu_id = @vim_menu_id
ORDER BY sm.txt_sub_menu_name;

SELECT
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM cfg_tbl_sub_menu sm
            WHERE sm.txt_sub_menu_name IN ('Product Catalog', 'Shopping Cart', 'Checkout', 'Receipt', 'Feedback')
              AND sm.ser_menu_id = @vim_menu_id
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
              AND sm.bl_is_active = 1
        ) THEN '❌ Shopping Cart menu items are still active'
        ELSE '✅ Shopping Cart menu items removed - RESTART BACKEND and LOGOUT/LOGIN to see changes'
    END AS 'FINAL STATUS';

DROP TEMPORARY TABLE IF EXISTS tmp_cart_submenus;
