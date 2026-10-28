-- Backfill cfg_tbl_user org labels from SAP HCM employee (Employee ID = txt_cnic).
-- Text columns only; ser_organization_id / ser_subarea_id / ser_division_id / ser_department_id stay NULL unless already set.
-- Note: MySQL reserves DIV — never use alias "div" in SQL.
-- Database: velocity (change USE if needed)

USE velocity;

-- ---------------------------------------------------------------------------
-- Step 1: Add text columns (safe if already present)
-- ---------------------------------------------------------------------------
SET @db = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cfg_tbl_user' AND COLUMN_NAME = 'txt_organization_name') = 0,
    'ALTER TABLE cfg_tbl_user ADD COLUMN txt_organization_name VARCHAR(255) NULL AFTER txt_department_name',
    'SELECT ''txt_organization_name exists'' AS msg'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cfg_tbl_user' AND COLUMN_NAME = 'txt_subarea_name') = 0,
    'ALTER TABLE cfg_tbl_user ADD COLUMN txt_subarea_name VARCHAR(255) NULL AFTER txt_organization_name',
    'SELECT ''txt_subarea_name exists'' AS msg'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cfg_tbl_user' AND COLUMN_NAME = 'txt_division_name') = 0,
    'ALTER TABLE cfg_tbl_user ADD COLUMN txt_division_name VARCHAR(255) NULL AFTER txt_subarea_name',
    'SELECT ''txt_division_name exists'' AS msg'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Optional ID columns (left NULL by this script)
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cfg_tbl_user' AND COLUMN_NAME = 'ser_organization_id') = 0,
    'ALTER TABLE cfg_tbl_user ADD COLUMN ser_organization_id INT NULL AFTER ser_department_id',
    'SELECT ''ser_organization_id exists'' AS msg'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cfg_tbl_user' AND COLUMN_NAME = 'ser_subarea_id') = 0,
    'ALTER TABLE cfg_tbl_user ADD COLUMN ser_subarea_id INT NULL AFTER ser_organization_id',
    'SELECT ''ser_subarea_id exists'' AS msg'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cfg_tbl_user' AND COLUMN_NAME = 'ser_division_id') = 0,
    'ALTER TABLE cfg_tbl_user ADD COLUMN ser_division_id INT NULL AFTER ser_subarea_id',
    'SELECT ''ser_division_id exists'' AS msg'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------------------------------------------------------------------------
-- Updates below: Workbench "Safe Updates" (1175) needs a KEY in WHERE or safe mode off.
-- ---------------------------------------------------------------------------
SET @OLD_SQL_SAFE_UPDATES = @@SESSION.SQL_SAFE_UPDATES;
SET SESSION SQL_SAFE_UPDATES = 0;

-- ---------------------------------------------------------------------------
-- Step 2: Copy org labels from SAP employee row (no master-table joins)
-- ---------------------------------------------------------------------------
UPDATE cfg_tbl_user u
INNER JOIN cfg_tbl_sap_hcm_employee e
    ON TRIM(e.txt_employee_id) = TRIM(u.txt_cnic)
   AND (e.bl_is_deleted = 0 OR e.bl_is_deleted IS NULL)
SET
    u.txt_organization_name = NULLIF(TRIM(e.txt_company_name), ''),
    u.txt_subarea_name = NULLIF(TRIM(e.txt_subarea), ''),
    u.txt_division_name = NULLIF(TRIM(e.txt_division), ''),
    u.txt_department_name = NULLIF(TRIM(e.txt_department_name), ''),
    u.dte_modified_date = NOW()
WHERE u.ser_user_id > 0
  AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL);

-- ---------------------------------------------------------------------------
-- Step 3 (optional): If user already has ser_department_id, refresh department name only
-- ---------------------------------------------------------------------------
UPDATE cfg_tbl_user u
INNER JOIN hr_tbl_department d
    ON d.ser_department_id = u.ser_department_id
   AND (d.bl_is_deleted = 0 OR d.bl_is_deleted IS NULL)
SET
    u.txt_department_name = d.txt_department_name,
    u.dte_modified_date = NOW()
WHERE u.ser_user_id > 0
  AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
  AND u.ser_department_id IS NOT NULL;

SET SESSION SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- ---------------------------------------------------------------------------
-- Step 4: Verification
-- ---------------------------------------------------------------------------
SELECT
    COUNT(*) AS total_users,
    SUM(CASE WHEN txt_organization_name IS NOT NULL AND txt_organization_name <> '' THEN 1 ELSE 0 END) AS with_org_text,
    SUM(CASE WHEN txt_department_name IS NOT NULL AND txt_department_name <> '' THEN 1 ELSE 0 END) AS with_dept_text
FROM cfg_tbl_user
WHERE bl_is_deleted = 0 OR bl_is_deleted IS NULL;

SELECT
    u.ser_user_id,
    u.txt_user_name,
    u.txt_cnic,
    u.txt_organization_name,
    u.txt_subarea_name,
    u.txt_division_name,
    u.txt_department_name
FROM cfg_tbl_user u
WHERE (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
ORDER BY u.txt_user_name
LIMIT 50;
