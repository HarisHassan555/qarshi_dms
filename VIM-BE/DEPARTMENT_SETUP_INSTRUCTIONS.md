# Department Setup Instructions

This document provides instructions for setting up the Department feature in the database.

## Prerequisites

- MySQL 8.x database
- Database name: `vim_3`
- Access to the database with appropriate permissions (ALTER, INSERT, SELECT, INSERT)

## Scripts Overview

Three SQL scripts have been created:

1. **`add_department_column.sql`** - Adds the `ser_department_id` column to `cfg_tbl_user` table
2. **`add_department_menu_simple.sql`** - Simple script to add Department to Master Data menu (Recommended)
3. **`add_department_menu.sql`** - Comprehensive script with all features (Advanced)

## Step-by-Step Setup

### Step 1: Add Department Column to User Table

Run the following script to add the foreign key column:

```bash
mysql -u root -p vim_3 < add_department_column.sql
```

Or execute in MySQL Workbench/phpMyAdmin:
- Open `add_department_column.sql`
- Execute the script

**What it does:**
- Adds `ser_department_id` column to `cfg_tbl_user` table
- Creates foreign key constraint to `hr_tbl_department` table
- Sets ON DELETE to SET NULL (users won't be deleted if department is deleted)

### Step 2: Add Department to Master Data Menu

**Option A: Simple Script (Recommended)**

Run the simple script:
```bash
mysql -u root -p vim_3 < add_department_menu_simple.sql
```

**Option B: Comprehensive Script**

If you need more control or want to see detailed verification:
```bash
mysql -u root -p vim_3 < add_department_menu.sql
```

**What it does:**
- Finds the Master Data menu (searches for menu containing "Master Data" or "Master")
- Creates a new submenu entry for "Department"
- Sets up default permissions for all existing roles
- Grants View, Add, and Update permissions (Delete and Approve are disabled by default)

### Step 3: Verify Setup

Run these queries to verify everything was set up correctly:

```sql
-- Check if column was added
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_user'
  AND COLUMN_NAME = 'ser_department_id';

-- Check if Department submenu exists
SELECT 
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.txt_sub_menu_url,
    m.txt_menu_name AS parent_menu
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

-- Check role permissions
SELECT 
    r.txt_role_name,
    smr.bl_is_view AS can_view,
    smr.bl_is_add AS can_add,
    smr.bl_is_update AS can_update,
    smr.bl_is_enabled AS is_enabled
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
WHERE sm.txt_sub_menu_name = 'Department';
```

## Customization

### Adjust Menu ID

If the script cannot find the Master Data menu automatically, you can manually set it:

1. Find your Master Data menu ID:
```sql
SELECT ser_menu_id, txt_menu_name 
FROM cfg_tbl_menu 
WHERE txt_menu_name LIKE '%Master%';
```

2. Edit `add_department_menu_simple.sql` and replace:
```sql
SET @master_data_menu_id = 1; -- Replace 1 with your actual menu ID
```

### Adjust Permissions

To customize permissions for specific roles, you can update the permissions after running the script:

```sql
-- Example: Disable Add permission for a specific role
UPDATE cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
SET smr.bl_is_add = 0
WHERE sm.txt_sub_menu_name = 'Department'
  AND r.txt_role_name = 'Viewer'; -- Replace with your role name
```

### Adjust Created User ID

In the scripts, `ser_created_user` is set to `1`. Change this to your admin user ID:

```sql
-- Find your admin user ID
SELECT ser_user_id, txt_user_name 
FROM cfg_tbl_user 
WHERE txt_user_name = 'admin'; -- Replace with your admin username

-- Then update the scripts to use that ID
```

## Troubleshooting

### Issue: "Master Data menu not found"

**Solution:**
1. Check if Master Data menu exists:
```sql
SELECT * FROM cfg_tbl_menu WHERE txt_menu_name LIKE '%Master%';
```

2. If it doesn't exist, create it first or use a different parent menu ID

3. Manually set the menu ID in the script

### Issue: "Foreign key constraint fails"

**Solution:**
- Ensure `hr_tbl_department` table exists
- Ensure there are no orphaned department references in `cfg_tbl_user`
- Check that department IDs are valid

### Issue: "Duplicate entry" errors

**Solution:**
- The scripts check for existing entries before inserting
- If you get duplicate errors, the menu/submenu may already exist
- Check existing entries:
```sql
SELECT * FROM cfg_tbl_sub_menu WHERE txt_sub_menu_name = 'Department';
```

## After Setup

Once the scripts are executed:

1. **Restart the backend application** to ensure JPA picks up the new column
2. **Clear browser cache** and refresh the frontend
3. **Navigate to `/department`** - it should now work without redirecting
4. **Test permissions** - verify users can create/edit departments based on their roles

## Rollback (If Needed)

If you need to remove the Department menu:

```sql
-- Remove role permissions
DELETE smr FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

-- Remove submenu
DELETE FROM cfg_tbl_sub_menu 
WHERE txt_sub_menu_name = 'Department';

-- Remove column (optional - be careful!)
-- ALTER TABLE cfg_tbl_user DROP FOREIGN KEY fk_user_department;
-- ALTER TABLE cfg_tbl_user DROP COLUMN ser_department_id;
```

## Notes

- The scripts use `NOW()` for timestamps
- Default permissions: View=1, Add=1, Update=1, Delete=0, Approve=0
- All permissions are enabled (`bl_is_enabled = 1`) by default
- The submenu order is automatically calculated based on existing submenus
- The foreign key uses `ON DELETE SET NULL` to prevent data loss

