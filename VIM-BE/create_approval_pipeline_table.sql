-- =====================================================
-- SQL Script: Create Approval Pipeline Table
-- Database: vim_3
-- Description: Creates table for storing form approval pipelines
-- =====================================================

USE vim_3;

-- =====================================================
-- Table: cfg_tbl_custom_form_approval_pipeline
-- Description: Stores approval pipeline steps for custom forms
-- =====================================================
CREATE TABLE IF NOT EXISTS cfg_tbl_custom_form_approval_pipeline (
    ser_approval_pipeline_id INT AUTO_INCREMENT PRIMARY KEY,
    ser_form_id INT NOT NULL,
    ser_department_id INT NOT NULL,
    int_approval_order INT NOT NULL DEFAULT 1,
    bl_is_active BOOLEAN DEFAULT TRUE,
    bl_is_deleted BOOLEAN DEFAULT FALSE,
    dte_created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user INT,
    ser_modified_user INT,
    FOREIGN KEY (ser_form_id) REFERENCES cfg_tbl_custom_form(ser_form_id) ON DELETE CASCADE,
    FOREIGN KEY (ser_department_id) REFERENCES hr_tbl_department(ser_department_id) ON DELETE CASCADE,
    INDEX idx_form_id (ser_form_id),
    INDEX idx_department_id (ser_department_id),
    INDEX idx_approval_order (int_approval_order),
    INDEX idx_is_deleted (bl_is_deleted),
    UNIQUE KEY unique_form_department_order (ser_form_id, ser_department_id, int_approval_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Verification Queries
-- =====================================================
SELECT 'Table Created Successfully' AS Status;

SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form_approval_pipeline'
ORDER BY TABLE_NAME;







