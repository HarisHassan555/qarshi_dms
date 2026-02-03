# Shopping Cart Menu Setup Guide

## 📋 Overview

This guide explains how to add shopping cart menu items to your VIM database so users can access the shopping cart pages from the navigation menu.

## 📁 File

**SQL Script**: `add_shopping_cart_menu_items.sql`

## 🎯 What This Script Does

The script adds **5 new menu items** to the VIM menu:

1. **Product Catalog** - Browse and shop products
2. **Shopping Cart** - View and manage cart items
3. **Checkout** - Complete purchase
4. **Receipt** - View order confirmation
5. **Feedback** - Submit feedback

## 🚀 How to Run

### Option 1: Using MySQL Command Line

```bash
mysql -u root -p vim_3 < add_shopping_cart_menu_items.sql
```

### Option 2: Using MySQL Workbench / phpMyAdmin

1. Open MySQL Workbench or phpMyAdmin
2. Select database: `vim_3`
3. Open the SQL script file: `add_shopping_cart_menu_items.sql`
4. Execute the script (Run button)

### Option 3: Using Command Line (Windows)

```cmd
mysql -u root -p vim_3 < VIM-BE\add_shopping_cart_menu_items.sql
```

## ✅ What Happens After Running

1. **Creates 5 submenu items** under VIM menu
2. **Assigns permissions** to all active roles
3. **Assigns permissions** to all active users
4. **Sets proper access levels** for each page

## 🔍 Verification

After running the script, you should see:

1. **Verification queries** showing:
   - All 5 submenus created
   - Permissions assigned to roles
   - Permissions assigned to users

2. **Final status message**:
   - ✅ Success: "Shopping Cart menu items are correctly configured"
   - ❌ Error: "Shopping Cart menu items need to be fixed"

## 🔄 After Running Script

### Required Steps:

1. **Restart Backend Server**
   - Stop your Spring Boot application
   - Start it again
   - This reloads menu configuration

2. **Logout and Login**
   - Logout from the frontend
   - Login again
   - This refreshes user permissions

3. **Check Menu**
   - Navigate to the sidebar menu
   - You should see the new shopping cart menu items

## 📊 Menu Structure

```
VIM Menu
├── Product Catalog (product-catalog)
├── Shopping Cart (cart)
├── Checkout (checkout)
├── Receipt (receipt)
└── Feedback (feedback)
```

## 🔐 Permissions Set

### Product Catalog
- ✅ View: Yes
- ✅ Add: Yes
- ❌ Delete: No
- ❌ Update: No

### Shopping Cart
- ✅ View: Yes
- ✅ Add: Yes
- ✅ Delete: Yes
- ✅ Update: Yes

### Checkout
- ✅ View: Yes
- ✅ Add: Yes
- ❌ Delete: No
- ❌ Update: No

### Receipt
- ✅ View: Yes
- ❌ Add: No
- ❌ Delete: No
- ❌ Update: No

### Feedback
- ✅ View: Yes
- ✅ Add: Yes
- ❌ Delete: No
- ❌ Update: No

## ⚠️ Important Notes

1. **User ID**: The script uses `ser_created_user = 1` as the creator. If your admin user has a different ID, you may want to update it in the script.

2. **Menu ID**: The script automatically finds the VIM menu. If you have multiple VIM menus, it uses the first active one.

3. **Order Numbers**: Menu items are added with sequential order numbers starting from the highest existing order + 1.

4. **Idempotent**: The script is safe to run multiple times. It checks if items exist before creating them.

## 🐛 Troubleshooting

### Menu items not showing?

1. **Check if script ran successfully**
   ```sql
   SELECT * FROM cfg_tbl_sub_menu 
   WHERE txt_sub_menu_name IN ('Product Catalog', 'Shopping Cart', 'Checkout', 'Receipt', 'Feedback');
   ```

2. **Check permissions**
   ```sql
   SELECT sm.txt_sub_menu_name, COUNT(*) as permission_count
   FROM cfg_tbl_sub_menu_role smr
   INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
   WHERE sm.txt_sub_menu_name IN ('Product Catalog', 'Shopping Cart', 'Checkout', 'Receipt', 'Feedback')
   GROUP BY sm.txt_sub_menu_name;
   ```

3. **Restart backend and re-login**
   - This is the most common solution

### Permission denied errors?

- Check if your user has the correct role
- Verify user-specific permissions were created
- Check if `bl_is_enabled = 1` in `cfg_tbl_sub_menu_role`

## 📝 Customization

If you want to change permissions, edit the script and modify:

- `bl_is_view` - View permission
- `bl_is_add` - Add permission
- `bl_is_delete` - Delete permission
- `bl_is_update` - Update permission
- `bl_is_approve` - Approve permission

## ✅ Success Checklist

- [ ] Script executed without errors
- [ ] Verification queries show 5 submenus
- [ ] Backend server restarted
- [ ] User logged out and logged in
- [ ] Menu items visible in sidebar
- [ ] Can navigate to all shopping cart pages

---

**Need Help?** Check the verification queries in the script output for detailed status information.



