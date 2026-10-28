-- =====================================================
-- SAP HCM employee master (from ZHCM_EMP_INFO_PAD_WS XML)
-- Pictures stored on disk; DB holds path + JSON snapshot (no base64)
-- Database: velocity
-- =====================================================

USE velocity;

CREATE TABLE IF NOT EXISTS cfg_tbl_sap_hcm_employee (
    ser_sap_hcm_employee_id INT NOT NULL AUTO_INCREMENT,
    txt_employee_id VARCHAR(32) NOT NULL COMMENT 'SAP EMPLOYEE',
    txt_emp_name VARCHAR(255) NULL,
    txt_department_name VARCHAR(255) NULL,
    txt_division VARCHAR(255) NULL,
    txt_designation VARCHAR(255) NULL,
    txt_email VARCHAR(255) NULL,
    txt_pay_scale VARCHAR(32) NULL,
    txt_subarea VARCHAR(255) NULL,
    txt_company_name VARCHAR(255) NULL,
    txt_hiring_date VARCHAR(32) NULL,
    txt_confirmation_date VARCHAR(32) NULL,
    txt_retirement_date VARCHAR(32) NULL,
    txt_separation_date VARCHAR(32) NULL,
    txt_rehiring_date VARCHAR(32) NULL,
    txt_annual_inc_type VARCHAR(64) NULL,
    txt_active VARCHAR(64) NULL,
    txt_report_to VARCHAR(32) NULL,
    txt_report_to_name VARCHAR(255) NULL,
    txt_picture_path VARCHAR(512) NULL COMMENT 'Relative to ~/.vim_dms_uploads',
    bl_has_picture TINYINT(1) NOT NULL DEFAULT 0,
    txt_record_json LONGTEXT NULL COMMENT 'Full row as JSON without PICTURE_BASE64',
    dte_last_synced_date DATETIME NULL,
    dte_created_date DATETIME NULL,
    dte_modified_date DATETIME NULL,
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (ser_sap_hcm_employee_id),
    UNIQUE KEY uk_cfg_sap_hcm_employee_id (txt_employee_id),
    KEY idx_cfg_sap_hcm_company (txt_company_name),
    KEY idx_cfg_sap_hcm_dept (txt_department_name),
    KEY idx_cfg_sap_hcm_active (txt_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
