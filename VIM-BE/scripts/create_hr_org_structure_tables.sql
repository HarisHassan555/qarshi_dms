-- Organization → Subarea → Division → Department (FK on hr_tbl_department)
-- Database: velocity (change USE if your schema differs)

USE velocity;

CREATE TABLE IF NOT EXISTS hr_tbl_organization (
    ser_organization_id INT NOT NULL AUTO_INCREMENT,
    logo VARCHAR(512) NULL,
    txt_organization_name VARCHAR(255) NOT NULL,
    txt_organization_code VARCHAR(64) NULL,
    txt_address VARCHAR(512) NULL,
    bln_status TINYINT(1) NULL DEFAULT 1,
    bl_is_deleted TINYINT(1) NULL DEFAULT 0,
    dte_created_date DATETIME NULL,
    dte_modified_date DATETIME NULL,
    PRIMARY KEY (ser_organization_id),
    UNIQUE KEY uk_hr_org_name (txt_organization_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_tbl_subarea (
    ser_subarea_id INT NOT NULL AUTO_INCREMENT,
    ser_organization_id INT NOT NULL,
    txt_subarea_name VARCHAR(255) NOT NULL,
    txt_subarea_code VARCHAR(64) NULL,
    bln_status TINYINT(1) NULL DEFAULT 1,
    bl_is_deleted TINYINT(1) NULL DEFAULT 0,
    dte_created_date DATETIME NULL,
    dte_modified_date DATETIME NULL,
    PRIMARY KEY (ser_subarea_id),
    UNIQUE KEY uk_hr_subarea_org_name (ser_organization_id, txt_subarea_name),
    CONSTRAINT fk_subarea_organization
        FOREIGN KEY (ser_organization_id) REFERENCES hr_tbl_organization (ser_organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_tbl_division (
    ser_division_id INT NOT NULL AUTO_INCREMENT,
    ser_subarea_id INT NOT NULL,
    txt_division_name VARCHAR(255) NOT NULL,
    txt_division_code VARCHAR(64) NULL,
    bln_status TINYINT(1) NULL DEFAULT 1,
    bl_is_deleted TINYINT(1) NULL DEFAULT 0,
    dte_created_date DATETIME NULL,
    dte_modified_date DATETIME NULL,
    PRIMARY KEY (ser_division_id),
    UNIQUE KEY uk_hr_division_subarea_name (ser_subarea_id, txt_division_name),
    CONSTRAINT fk_division_subarea
        FOREIGN KEY (ser_subarea_id) REFERENCES hr_tbl_subarea (ser_subarea_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'hr_tbl_department'
      AND COLUMN_NAME = 'ser_division_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE hr_tbl_department
        ADD COLUMN ser_division_id INT NULL AFTER ser_parent_department_id,
        ADD CONSTRAINT fk_department_division
            FOREIGN KEY (ser_division_id) REFERENCES hr_tbl_division (ser_division_id)',
    'SELECT ''ser_division_id already exists on hr_tbl_department'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
