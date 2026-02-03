-- =====================================================
-- SQL Script: Create Form Builder Tables
-- Database: vim_3
-- Description: Creates tables for storing custom forms and their fields
-- =====================================================

USE vim_3;

-- =====================================================
-- Table: cfg_tbl_custom_form
-- Description: Stores custom form definitions
-- =====================================================
CREATE TABLE IF NOT EXISTS cfg_tbl_custom_form (
    ser_form_id INT AUTO_INCREMENT PRIMARY KEY,
    txt_form_name VARCHAR(255) NOT NULL,
    txt_form_description TEXT,
    bl_is_active BOOLEAN DEFAULT TRUE,
    bl_is_deleted BOOLEAN DEFAULT FALSE,
    bln_status BOOLEAN DEFAULT TRUE,
    dte_created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user INT,
    ser_modified_user INT,
    INDEX idx_form_name (txt_form_name),
    INDEX idx_is_deleted (bl_is_deleted),
    INDEX idx_is_active (bl_is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: cfg_tbl_custom_form_field
-- Description: Stores fields for each custom form
-- =====================================================
CREATE TABLE IF NOT EXISTS cfg_tbl_custom_form_field (
    ser_field_id INT AUTO_INCREMENT PRIMARY KEY,
    ser_form_id INT NOT NULL,
    txt_field_label VARCHAR(255) NOT NULL,
    txt_field_type VARCHAR(50) NOT NULL,
    txt_placeholder VARCHAR(255),
    bl_is_required BOOLEAN DEFAULT FALSE,
    int_field_order INT DEFAULT 0,
    txt_field_options TEXT, -- For select, radio, checkbox options (JSON format)
    bl_is_active BOOLEAN DEFAULT TRUE,
    bl_is_deleted BOOLEAN DEFAULT FALSE,
    dte_created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user INT,
    ser_modified_user INT,
    FOREIGN KEY (ser_form_id) REFERENCES cfg_tbl_custom_form(ser_form_id) ON DELETE CASCADE,
    INDEX idx_form_id (ser_form_id),
    INDEX idx_field_order (int_field_order),
    INDEX idx_is_deleted (bl_is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Verification Queries
-- =====================================================
SELECT 'Tables Created Successfully' AS Status;

SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME IN ('cfg_tbl_custom_form', 'cfg_tbl_custom_form_field')
ORDER BY TABLE_NAME;



