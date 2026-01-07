-- SQL Script to check existing users in the database
-- Run this in your MySQL client (MySQL Workbench, phpMyAdmin, or command line)

USE vim_3;

-- List all active users with their usernames
SELECT 
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_password AS 'Password (Base64 Encoded)',
    bln_status AS 'Is Active',
    bl_is_deleted AS 'Is Deleted',
    bl_is_active AS 'Is Active Flag'
FROM cfg_tbl_user
WHERE (bln_status = true OR bln_status IS NULL)
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
ORDER BY ser_user_id;

-- To see all users (including inactive ones), uncomment the line below:
-- SELECT ser_user_id, txt_user_name, txt_password, bln_status FROM cfg_tbl_user;

