# How to Find Login Credentials

## Password Encoding
The application uses **Base64 encoding** (not encryption) for passwords. When you enter a password, it's Base64 encoded and compared with the stored value in the database.

## Method 1: Check Database Directly

### Option A: Using MySQL Command Line
```bash
mysql -u root -pdsg123 -h localhost -P 3308 vim_3 -e "SELECT ser_user_id, txt_user_name, txt_password, bln_status FROM cfg_tbl_user WHERE (bln_status = true OR bln_status IS NULL) AND (bl_is_deleted = false OR bl_is_deleted IS NULL);"
```

### Option B: Using MySQL Workbench or phpMyAdmin
1. Connect to MySQL:
   - Host: `localhost`
   - Port: `3308`
   - Username: `root`
   - Password: `dsg123`
   - Database: `vim_3`

2. Run this SQL query:
```sql
SELECT 
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_password AS 'Password (Base64)',
    bln_status AS 'Is Active'
FROM cfg_tbl_user
WHERE (bln_status = true OR bln_status IS NULL)
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
ORDER BY ser_user_id;
```

## Method 2: Decode Base64 Password

If you find a user but don't know the password, you can decode the Base64 password:

### Using Online Tool:
1. Copy the `txt_password` value from the database
2. Go to https://www.base64decode.org/
3. Decode the value to get the plain text password

### Using PowerShell:
```powershell
[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String("YOUR_BASE64_PASSWORD_HERE"))
```

### Using Java (if you have access):
```java
import java.util.Base64;
String decoded = new String(Base64.getDecoder().decode("YOUR_BASE64_PASSWORD_HERE"));
```

## Method 3: Create a Test User

If no users exist, you'll need to create one. The password should be Base64 encoded before storing.

### Example: Create user with password "admin123"
1. Encode "admin123" to Base64: `YWRtaW4xMjM=`
2. Insert into database:
```sql
INSERT INTO cfg_tbl_user (
    txt_user_name, 
    txt_password, 
    bln_status, 
    bl_is_deleted, 
    bl_is_active
) VALUES (
    'admin',
    'YWRtaW4xMjM=',  -- Base64 encoded "admin123"
    true,
    false,
    true
);
```

## Common Default Credentials to Try

Try these common combinations (if users exist):
- **Username:** `admin` / **Password:** `admin` or `admin123` or `password`
- **Username:** `user` / **Password:** `user` or `user123`
- **Username:** `test` / **Password:** `test` or `test123`

## Quick Password Encoding Reference

| Plain Text | Base64 Encoded |
|------------|----------------|
| admin | YWRtaW4= |
| admin123 | YWRtaW4xMjM= |
| password | cGFzc3dvcmQ= |
| test | dGVzdA== |
| test123 | dGVzdDEyMw== |

## Notes
- Passwords are **Base64 encoded**, not encrypted (this is not secure for production!)
- User must have `bln_status = true` (or NULL) to login
- User must have `bl_is_deleted = false` (or NULL) to login
- Failed login attempts are tracked and may lock the account









