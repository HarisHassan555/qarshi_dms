-- Document header branding on organizations (logo + address for letterheads)
-- Column logo is first after ser_organization_id (see rename_organization_logo_column.sql)
USE velocity;

SET @col_address = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_tbl_organization' AND COLUMN_NAME = 'txt_address'
);
SET @sql_address = IF(@col_address = 0,
    'ALTER TABLE hr_tbl_organization ADD COLUMN txt_address VARCHAR(512) NULL AFTER txt_organization_code',
    'SELECT ''txt_address already exists'' AS message');
PREPARE stmt FROM @sql_address;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_logo_path = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_tbl_organization' AND COLUMN_NAME = 'txt_logo_path'
);
SET @has_logo = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_tbl_organization' AND COLUMN_NAME = 'logo'
);

SET @sql_logo = IF(@has_logo_path > 0 AND @has_logo = 0,
    'ALTER TABLE hr_tbl_organization CHANGE COLUMN txt_logo_path logo VARCHAR(512) NULL AFTER ser_organization_id',
    IF(@has_logo = 0,
        'ALTER TABLE hr_tbl_organization ADD COLUMN logo VARCHAR(512) NULL AFTER ser_organization_id',
        'SELECT ''logo column already exists'' AS message'));
PREPARE stmt FROM @sql_logo;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- SAP-synced org mapping (see VIM-FE document-header.util.ts)

UPDATE hr_tbl_organization
SET txt_address = '15-G, Jam-e-Shirin Boulevard, Gulberg-III, Lahore',
    logo = 'assets/images/qarshi-logo.png',
    dte_modified_date = NOW()
WHERE ser_organization_id = 1
LIMIT 1;

UPDATE hr_tbl_organization
SET txt_address = '149, 1st Floor, Al Hassan Plaza, Near Jamia Ashrafia, Ferozpur Road, Lahore',
    logo = 'assets/images/QRI Logo.jpg.jpeg',
    dte_modified_date = NOW()
WHERE ser_organization_id = 2
LIMIT 1;

UPDATE hr_tbl_organization
SET txt_address = '15-G, Jam-e-Shirin Boulevard, Gulberg-III, Lahore',
    logo = 'assets/images/qarshi-logo.png',
    dte_modified_date = NOW()
WHERE ser_organization_id = 3
LIMIT 1;

UPDATE hr_tbl_organization
SET txt_address = '15-G, Jam-e-Shirin Boulevard, Gulberg-III, Lahore',
    logo = 'assets/images/QU_logo.png',
    dte_modified_date = NOW()
WHERE ser_organization_id = 4
LIMIT 1;

UPDATE hr_tbl_organization
SET txt_address = '15-G, Jam-e-Shirin Boulevard, Gulberg-III, Lahore',
    logo = 'assets/images/QF_logo.png',
    dte_modified_date = NOW()
WHERE ser_organization_id = 5
LIMIT 1;

UPDATE hr_tbl_organization
SET bl_is_deleted = 1,
    bln_status = 0,
    dte_modified_date = NOW()
WHERE ser_organization_id = 6
  AND txt_organization_code = 'QI'
LIMIT 1;
