-- =====================================================
-- SQL Script: Add Shopping Cart Menu Items to VIM Menu
-- Database: vim_3
-- Description: This script adds shopping cart pages as submenus:
--              - Product Catalog
--              - Shopping Cart
--              - Checkout
--              - Receipt
--              - Feedback
--              All with proper permissions for all roles and users
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

-- Display VIM/Velocity Menu ID
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
-- Step 3: Create or Update Product Catalog Submenu
-- =====================================================
SET @product_catalog_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Product Catalog' 
      AND (@vim_menu_id IS NULL OR ser_menu_id = @vim_menu_id)
    LIMIT 1
);

-- Update existing submenu if it exists (even if soft-deleted)
UPDATE cfg_tbl_sub_menu 
SET 
    ser_menu_id = @vim_menu_id,
    txt_sub_menu_url = 'product-catalog',
    int_sub_menu_order = @base_order,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @product_catalog_submenu_id
  AND @vim_menu_id IS NOT NULL
  AND @product_catalog_submenu_id IS NOT NULL;

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
    'Product Catalog',
    'product-catalog',
    @base_order,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    NOW(),
    1   -- Created by user ID 1 (CHANGE THIS TO YOUR ADMIN USER ID)
WHERE @product_catalog_submenu_id IS NULL
  AND @vim_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Product Catalog' 
      AND ser_menu_id = @vim_menu_id
  );

SET @product_catalog_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Product Catalog' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 4: Create or Update Shopping Cart Submenu
-- =====================================================
SET @shopping_cart_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Shopping Cart' 
      AND (@vim_menu_id IS NULL OR ser_menu_id = @vim_menu_id)
    LIMIT 1
);

-- Update existing submenu if it exists
UPDATE cfg_tbl_sub_menu 
SET 
    ser_menu_id = @vim_menu_id,
    txt_sub_menu_url = 'cart',
    int_sub_menu_order = @base_order + 1,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 1,
    bl_is_update = 1,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @shopping_cart_submenu_id
  AND @vim_menu_id IS NOT NULL
  AND @shopping_cart_submenu_id IS NOT NULL;

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
    'Shopping Cart',
    'cart',
    @base_order + 1,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    1,  -- Delete permission
    1,  -- Update permission
    0,  -- Approve permission
    NOW(),
    1
WHERE @shopping_cart_submenu_id IS NULL
  AND @vim_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Shopping Cart' 
      AND ser_menu_id = @vim_menu_id
  );

SET @shopping_cart_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Shopping Cart' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 5: Create or Update Checkout Submenu
-- =====================================================
SET @checkout_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Checkout' 
      AND (@vim_menu_id IS NULL OR ser_menu_id = @vim_menu_id)
    LIMIT 1
);

-- Update existing submenu if it exists
UPDATE cfg_tbl_sub_menu 
SET 
    ser_menu_id = @vim_menu_id,
    txt_sub_menu_url = 'checkout',
    int_sub_menu_order = @base_order + 2,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @checkout_submenu_id
  AND @vim_menu_id IS NOT NULL
  AND @checkout_submenu_id IS NOT NULL;

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
    'Checkout',
    'checkout',
    @base_order + 2,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    NOW(),
    1
WHERE @checkout_submenu_id IS NULL
  AND @vim_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Checkout' 
      AND ser_menu_id = @vim_menu_id
  );

SET @checkout_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Checkout' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 6: Create or Update Receipt Submenu
-- =====================================================
SET @receipt_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Receipt' 
      AND (@vim_menu_id IS NULL OR ser_menu_id = @vim_menu_id)
    LIMIT 1
);

-- Update existing submenu if it exists
UPDATE cfg_tbl_sub_menu 
SET 
    ser_menu_id = @vim_menu_id,
    txt_sub_menu_url = 'receipt',
    int_sub_menu_order = @base_order + 3,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 0,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @receipt_submenu_id
  AND @vim_menu_id IS NOT NULL
  AND @receipt_submenu_id IS NOT NULL;

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
    'Receipt',
    'receipt',
    @base_order + 3,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    0,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    NOW(),
    1
WHERE @receipt_submenu_id IS NULL
  AND @vim_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Receipt' 
      AND ser_menu_id = @vim_menu_id
  );

SET @receipt_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Receipt' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 7: Create or Update Feedback Submenu
-- =====================================================
SET @feedback_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Feedback' 
      AND (@vim_menu_id IS NULL OR ser_menu_id = @vim_menu_id)
    LIMIT 1
);

-- Update existing submenu if it exists
UPDATE cfg_tbl_sub_menu 
SET 
    ser_menu_id = @vim_menu_id,
    txt_sub_menu_url = 'feedback',
    int_sub_menu_order = @base_order + 4,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @feedback_submenu_id
  AND @vim_menu_id IS NOT NULL
  AND @feedback_submenu_id IS NOT NULL;

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
    'Feedback',
    'feedback',
    @base_order + 4,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    NOW(),
    1
WHERE @feedback_submenu_id IS NULL
  AND @vim_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Feedback' 
      AND ser_menu_id = @vim_menu_id
  );

SET @feedback_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Feedback' 
      AND ser_menu_id = @vim_menu_id
    LIMIT 1
);

-- =====================================================
-- Step 8: Create Role-Based Permissions for All Shopping Cart Submenus
-- =====================================================

-- Product Catalog Permissions
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
    @product_catalog_submenu_id,
    r.ser_role_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @product_catalog_submenu_id
      AND smr.ser_role_id = r.ser_role_id
      AND smr.ser_user_id IS NULL
  )
  AND @product_catalog_submenu_id IS NOT NULL;

-- Shopping Cart Permissions
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
    @shopping_cart_submenu_id,
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
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @shopping_cart_submenu_id
      AND smr.ser_role_id = r.ser_role_id
      AND smr.ser_user_id IS NULL
  )
  AND @shopping_cart_submenu_id IS NOT NULL;

-- Checkout Permissions
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
    @checkout_submenu_id,
    r.ser_role_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @checkout_submenu_id
      AND smr.ser_role_id = r.ser_role_id
      AND smr.ser_user_id IS NULL
  )
  AND @checkout_submenu_id IS NOT NULL;

-- Receipt Permissions
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
    @receipt_submenu_id,
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
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @receipt_submenu_id
      AND smr.ser_role_id = r.ser_role_id
      AND smr.ser_user_id IS NULL
  )
  AND @receipt_submenu_id IS NOT NULL;

-- Feedback Permissions
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
    @feedback_submenu_id,
    r.ser_role_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @feedback_submenu_id
      AND smr.ser_role_id = r.ser_role_id
      AND smr.ser_user_id IS NULL
  )
  AND @feedback_submenu_id IS NOT NULL;

-- =====================================================
-- Step 9: Create User-Specific Permissions for All Shopping Cart Submenus
-- =====================================================

-- Product Catalog User Permissions
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
    @product_catalog_submenu_id,
    u.ser_role_id,
    u.ser_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND u.ser_role_id IS NOT NULL
  AND @product_catalog_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @product_catalog_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- Shopping Cart User Permissions
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
    @shopping_cart_submenu_id,
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
    1
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND u.ser_role_id IS NOT NULL
  AND @shopping_cart_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @shopping_cart_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- Checkout User Permissions
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
    @checkout_submenu_id,
    u.ser_role_id,
    u.ser_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND u.ser_role_id IS NOT NULL
  AND @checkout_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @checkout_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- Receipt User Permissions
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
    @receipt_submenu_id,
    u.ser_role_id,
    u.ser_user_id,
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
    1
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND u.ser_role_id IS NOT NULL
  AND @receipt_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @receipt_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- Feedback User Permissions
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
    @feedback_submenu_id,
    u.ser_role_id,
    u.ser_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    0,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1
FROM cfg_tbl_user u
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND u.ser_role_id IS NOT NULL
  AND @feedback_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @feedback_submenu_id 
      AND smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = u.ser_role_id
  );

-- =====================================================
-- Step 10: Update Existing Permissions (if any)
-- =====================================================

UPDATE cfg_tbl_sub_menu_role smr
SET 
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    bl_is_enabled = 1,
    bl_is_all = 0,
    dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @product_catalog_submenu_id
  AND @product_catalog_submenu_id IS NOT NULL;

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
WHERE smr.ser_sub_menu_id = @shopping_cart_submenu_id
  AND @shopping_cart_submenu_id IS NOT NULL;

UPDATE cfg_tbl_sub_menu_role smr
SET 
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    bl_is_enabled = 1,
    bl_is_all = 0,
    dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @checkout_submenu_id
  AND @checkout_submenu_id IS NOT NULL;

UPDATE cfg_tbl_sub_menu_role smr
SET 
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 0,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    bl_is_enabled = 1,
    bl_is_all = 0,
    dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @receipt_submenu_id
  AND @receipt_submenu_id IS NOT NULL;

UPDATE cfg_tbl_sub_menu_role smr
SET 
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    bl_is_enabled = 1,
    bl_is_all = 0,
    dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @feedback_submenu_id
  AND @feedback_submenu_id IS NOT NULL;

-- =====================================================
-- Step 11: Verification Queries
-- =====================================================

-- Verify all shopping cart submenus were created
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
WHERE sm.txt_sub_menu_name IN ('Product Catalog', 'Shopping Cart', 'Checkout', 'Receipt', 'Feedback')
  AND sm.bl_is_deleted = 0
ORDER BY sm.int_sub_menu_order;

-- Verify role permissions count
SELECT 
    sm.txt_sub_menu_name AS 'Submenu',
    COUNT(DISTINCT smr.ser_role_id) AS 'Roles with Permissions',
    COUNT(DISTINCT smr.ser_user_id) AS 'Users with Permissions',
    COUNT(*) AS 'Total Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name IN ('Product Catalog', 'Shopping Cart', 'Checkout', 'Receipt', 'Feedback')
GROUP BY sm.txt_sub_menu_name
ORDER BY sm.txt_sub_menu_name;

-- Final status
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM cfg_tbl_sub_menu sm
            INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
            WHERE sm.txt_sub_menu_name IN ('Product Catalog', 'Shopping Cart', 'Checkout', 'Receipt', 'Feedback')
              AND (m.txt_menu_name LIKE '%VIM%' OR m.txt_menu_name LIKE '%vim%' OR m.txt_menu_name LIKE '%Vim%'
                   OR m.txt_menu_name LIKE '%Velocity%' OR m.txt_menu_name LIKE '%velocity%' OR m.txt_menu_name = 'Velocity')
              AND sm.bl_is_deleted = 0
              AND sm.bln_status = 1
              AND sm.bl_is_active = 1
        ) THEN '✅ Shopping Cart menu items are correctly configured - RESTART BACKEND and LOGOUT/LOGIN to see changes'
        ELSE '❌ Shopping Cart menu items need to be fixed'
    END AS 'FINAL STATUS';

