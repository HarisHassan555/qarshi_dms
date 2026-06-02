USE velocity_workbench;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @new_password_hash = '$2a$10$FCsLPVhZu7PND1PeSx4lkey1L533bEDdNafR2y0gWsxoeAb5kvWgq';

UPDATE cfg_tbl_user
SET
    txt_password = @new_password_hash,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE ser_user_id = 406
   OR LOWER(TRIM(txt_address)) = 'noor.munir@qarshi.com';

SELECT
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_address AS 'Email',
    txt_password AS 'Password Hash',
    dte_modified_date AS 'Last Modified'
FROM cfg_tbl_user
WHERE ser_user_id = 406
   OR LOWER(TRIM(txt_address)) = 'noor.munir@qarshi.com';

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
