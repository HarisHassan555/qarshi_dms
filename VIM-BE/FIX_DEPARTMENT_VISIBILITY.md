# Fix Department Menu Visibility

## Current Status ✅
- ✅ Department submenu exists in database
- ✅ Linked to Master Data menu (Menu ID: 1)
- ✅ 13 role permissions created
- ✅ All status flags are correct (active, not deleted)
- ✅ Frontend code updated

## The Problem
Department is not appearing in the sidebar or permission page because **the backend application needs to be restarted** to clear JPA/Hibernate entity cache.

## Solution Steps

### Step 1: Restart Backend Application (REQUIRED)

**This is the most important step!** The JPA entity manager caches menu relationships, and it won't see the new Department submenu until restarted.

1. **Stop the Spring Boot application**
   - If running in IntelliJ: Click the stop button
   - If running via command line: Press `Ctrl+C`
   - If running as a service: Stop the service

2. **Start the application again**
   - In IntelliJ: Run the application
   - Via command line: `mvn spring-boot:run` or `java -jar target/VIM.war`
   - As a service: Start the service

3. **Wait for application to fully start**
   - Check logs to ensure it started successfully
   - Look for "Started SpringBootSecurityJwtApplication" message

### Step 2: Clear Browser Cache

1. **Hard refresh the browser:**
   - Windows/Linux: `Ctrl + Shift + R`
   - Mac: `Cmd + Shift + R`

2. **Or clear browser cache completely:**
   - Chrome: Settings → Privacy → Clear browsing data
   - Firefox: Settings → Privacy → Clear Data
   - Edge: Settings → Privacy → Clear browsing data

### Step 3: Verify API Response

Test if the backend is returning Department in the menu:

1. **Open browser DevTools** (F12)
2. **Go to Network tab**
3. **Navigate to the application** (or refresh)
4. **Look for API call:** `GET /VIM/allMenu`
5. **Check the response** - it should include Department under Master Data

**Expected response structure:**
```json
[
  {
    "menuName": "Master Data",
    "subMenus": [
      { "subMenuName": "Country", ... },
      { "subMenuName": "City", ... },
      { "subMenuName": "Department", ... }  // ← Should be here
    ]
  }
]
```

### Step 4: Test Permission Page

1. **Navigate to:** `/role` (Permission page)
2. **Select a role and user**
3. **Department should appear** in the Master Data section with checkboxes

### Step 5: Test Sidebar Menu

1. **Log in as a user with Department permissions**
2. **Expand Master Data menu** in sidebar
3. **Department should appear** in the list

## If Still Not Working

### Check Backend Logs

Look for any errors related to:
- Menu loading
- JPA queries
- Entity relationships

### Verify Database Directly

Run this query to confirm Department exists:
```sql
SELECT 
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.ser_menu_id,
    m.txt_menu_name,
    sm.bl_is_active,
    sm.bln_status,
    sm.bl_is_deleted
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Department';
```

Expected result:
- `ser_sub_menu_id`: Some number
- `txt_sub_menu_name`: "Department"
- `ser_menu_id`: 1 (Master Data)
- `txt_menu_name`: "Master Data"
- `bl_is_active`: 1
- `bln_status`: 1
- `bl_is_deleted`: 0

### Check User Permissions

Verify your user has permissions to see Department:
```sql
SELECT 
    u.txt_user_name,
    r.txt_role_name,
    sm.txt_sub_menu_name,
    smr.bl_is_enabled,
    smr.bl_is_view
FROM cfg_tbl_user u
INNER JOIN cfg_tbl_role r ON u.ser_role_id = r.ser_role_id
INNER JOIN cfg_tbl_sub_menu_role smr ON smr.ser_role_id = r.ser_role_id
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Department'
  AND u.txt_user_name = '<your_username>';
```

### Force JPA Cache Clear (Advanced)

If restart doesn't work, you can try:

1. **Add to application.properties:**
   ```properties
   spring.jpa.properties.hibernate.cache.use_second_level_cache=false
   spring.jpa.properties.hibernate.cache.use_query_cache=false
   ```

2. **Or use EntityManager refresh:**
   ```java
   entityManager.refresh(menu);
   ```

## Quick Checklist

- [ ] Backend application restarted
- [ ] Application started successfully (check logs)
- [ ] Browser cache cleared
- [ ] Hard refresh performed
- [ ] API response includes Department (check Network tab)
- [ ] User has Department permissions
- [ ] Tested on Permission page
- [ ] Tested on Sidebar menu

## Most Common Issue

**99% of the time, the issue is that the backend wasn't restarted.** JPA caches entity relationships, and the new Department submenu won't appear until the cache is cleared by restarting the application.

**After restarting, Department should appear immediately!**





