-- Fix MySQL error 3144: JSON column + Hibernate @Lob sends binary charset
USE velocity;

ALTER TABLE cfg_tbl_sap_hcm_employee
    MODIFY COLUMN txt_record_json LONGTEXT NULL
    COMMENT 'Full row as JSON without PICTURE_BASE64';
