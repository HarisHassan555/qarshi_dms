# Department Menu Troubleshooting Guide

## Problem
Department submenu was created in the database but is not appearing in:
1. Master Data sidebar menu
2. Permission page (/role)

## Root Causes & Solutions

### 1. Backend Cache Issue (Most Common)

**Problem:** JPA/Hibernate might be caching the menu data and not picking up the new Department submenu.

**Solution:**
1. **Restart the backend application** (Spring Boot)
   - Stop the application
   - Start it again
   - This will clear JPA entity manager cache

2. **Clear browser cache** on the frontend
   - Hard refresh: `Ctrl + Shift + R` (Windows) or `Cmd + Shift + R` (Mac)
   - Or clear browser cache completely

### 2. Database Verification

Run the verification script to check if Department is properly configured:

```sql
-- Run: verify_and_fix_department_menu.sql
```

This will:
- Check if Department submenu exists
- Verify it's linked to Master Data menu
- Fix any configuration issues
- Show all Master Data submenus for comparison

### 3. Check Submenu Status Flags

The Department submenu must have these values:
- `bl_is_deleted = 0` (NOT deleted)
- `bln_status = 1` (Active status)
- `bl_is_active = 1` (Active)
- `ser_menu_id` must match Master Data menu ID

**Fix if needed:**
```sql
UPDATE cfg_tbl_sub_menu 
SET 
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0
WHERE txt_sub_menu_name = 'Department';
```

### 4. Check Menu Link

Verify Department is linked to the correct Master Data menu:

```sql
SELECT 
    sm.txt_sub_menu_name,
    m.txt_menu_name AS parent_menu,
    sm.ser_menu_id
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Department';
```

**If wrong menu:**
```sql
-- First, find Master Data menu ID
SELECT ser_menu_id, txt_menu_name 
FROM cfg_tbl_menu 
WHERE txt_menu_name LIKE '%Master%';

-- Then update Department submenu
UPDATE cfg_tbl_sub_menu 
SET ser_menu_id = <master_data_menu_id>
WHERE txt_sub_menu_name = 'Department';
```

### 5. Permission Page Issue

The permission page uses `getAllSubMenuRoles` which requires:
- Role permissions to exist in `cfg_tbl_sub_menu_role`
- User-specific permissions if applicable

**Check permissions:**
```sql
SELECT 
    sm.txt_sub_menu_name,
    r.txt_role_name,
    smr.bl_is_enabled,
    smr.bl_is_view,
    smr.bl_is_add,
    smr.bl_is_update
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
WHERE sm.txt_sub_menu_name = 'Department';
```

**If no permissions exist, create them:**
```sql
-- Get Department submenu ID
SET @dept_submenu_id = (
    SELECT ser_sub_menu_id 
    FROM cfg_tbl_sub_menu 
    WHERE txt_sub_menu_name = 'Department'
    LIMIT 1
);

-- Create permissions for all roles
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
    @dept_submenu_id,
    r.ser_role_id,
    1, 1, 0, 1, 1, 0, 1, 0, 1, 0,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND NOT EXISTS (
    SELECT 1 
    FROM cfg_tbl_sub_menu_role smr 
    WHERE smr.ser_sub_menu_id = @dept_submenu_id 
      AND smr.ser_role_id = r.ser_role_id
  )
  AND @dept_submenu_id IS NOT NULL;
```

### 6. Frontend Menu Structure

The frontend has hardcoded menu structure in:
- `permission-component.ts` - `frontendMenuStructure`
- `permission-service.ts` - `moduleSubMenuMap`

**Already fixed:** Department has been added to both files.

**If still not showing:**
1. Clear browser cache
2. Rebuild frontend: `npm run build` or restart dev server
3. Check browser console for errors

### 7. Sidebar Menu Filtering

The sidebar filters menus based on user permissions. Check:

1. **User has permissions for Department:**
   ```sql
   SELECT 
       u.txt_user_name,
       r.txt_role_name,
       sm.txt_sub_menu_name,
       smr.bl_is_enabled,
       smr.bl_is_view
   FROM cfg_tbl_sub_menu_role smr
   INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
   INNER JOIN cfg_tbl_role r ON smr.ser_role_id = r.ser_role_id
   INNER JOIN cfg_tbl_user u ON smr.ser_user_id = u.ser_user_id
   WHERE sm.txt_sub_menu_name = 'Department'
     AND u.txt_user_name = '<your_username>';
   ```

2. **If no user-specific permissions, check role permissions:**
   ```sql
   SELECT 
       u.txt_user_name,
       r.txt_role_name,
       sm.txt_sub_menu_name,
       smr.bl_is_enabled
   FROM cfg_tbl_user u
   INNER JOIN cfg_tbl_role r ON u.ser_role_id = r.ser_role_id
   INNER JOIN cfg_tbl_sub_menu_role smr ON smr.ser_role_id = r.ser_role_id
   INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
   WHERE sm.txt_sub_menu_name = 'Department'
     AND u.txt_user_name = '<your_username>';
   ```

### 8. JPA Entity Relationship

The menu entity uses `@OneToMany(fetch=FetchType.EAGER)` which should load submenus automatically. However:

1. **Check if submenus are being filtered:**
   - Look at `CfgTblMenuDAO.getAllMenu()` - it only filters menus, not submenus
   - Submenus are loaded via JPA relationship

2. **Force refresh:**
   - Restart backend to clear entity manager cache
   - Or use `EntityManager.refresh()` if needed

### 9. Complete Reset (Last Resort)

If nothing works, completely remove and re-add Department:

```sql
-- 1. Remove permissions
DELETE smr FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Department';

-- 2. Remove submenu
DELETE FROM cfg_tbl_sub_menu 
WHERE txt_sub_menu_name = 'Department';

-- 3. Re-run the add_department_menu_simple.sql script
```

## Quick Checklist

- [ ] Backend application restarted
- [ ] Browser cache cleared
- [ ] Department submenu exists in database
- [ ] Department is linked to Master Data menu
- [ ] Department has `bl_is_deleted = 0`, `bln_status = 1`, `bl_is_active = 1`
- [ ] Role permissions exist for Department
- [ ] User has permissions (role-based or user-specific)
- [ ] Frontend code updated (already done)
- [ ] Frontend rebuilt/restarted

## Testing Steps

1. **Check Database:**
   ```sql
   SELECT * FROM cfg_tbl_sub_menu WHERE txt_sub_menu_name = 'Department';
   ```

2. **Check API Response:**
   - Call: `GET /VIM/allMenu`
   - Look for Department in Master Data submenus

3. **Check Permissions API:**
   - Call: `GET /VIM/allSubMenuRole?roleId=<roleId>&userId=<userId>`
   - Look for Department in response

4. **Check Frontend:**
   - Open browser DevTools → Network tab
   - Navigate to Permission page
   - Check API calls and responses

## Still Not Working?

1. Check backend logs for errors
2. Check browser console for JavaScript errors
3. Verify the exact menu name in database matches "Department" (case-sensitive)
4. Check if there are multiple Department entries (should be only one)
5. Verify Master Data menu name matches exactly what the code expects




