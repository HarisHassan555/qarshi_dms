-- =====================================================
-- SQL Script: Create Custom Form Application Table
-- Database: vim_3
-- Description: Table to store submitted application data from custom forms
-- =====================================================

USE vim_3;

-- Create the application submission table
CREATE TABLE IF NOT EXISTS cfg_tbl_custom_form_application (
    ser_application_id INT AUTO_INCREMENT PRIMARY KEY,
    ser_form_id INT NOT NULL,
    txt_form_code VARCHAR(50) NULL COMMENT 'Form code from the form template (e.g., CAPF-0001)',
    txt_application_data JSON NOT NULL COMMENT 'Stores all form field values as JSON',
    txt_status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, IN_PROGRESS',
    int_current_approval_level INT DEFAULT 0 COMMENT 'Current step in approval pipeline',
    ser_submitted_by INT COMMENT 'User ID who submitted the application',
    ser_current_approver INT NULL COMMENT 'Current approver user ID',
    txt_remarks TEXT NULL COMMENT 'Any remarks or comments',
    bl_is_active BOOLEAN DEFAULT TRUE,
    bl_is_deleted BOOLEAN DEFAULT FALSE,
    bln_status BOOLEAN DEFAULT TRUE,
    dte_created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user INT NULL,
    ser_modified_user INT NULL,
    
    -- Foreign key constraint
    CONSTRAINT fk_custom_form_application_form
        FOREIGN KEY (ser_form_id)
        REFERENCES cfg_tbl_custom_form(ser_form_id)
        ON DELETE CASCADE,
    
    -- Indexes for better query performance
    INDEX idx_form_id (ser_form_id),
    INDEX idx_status (txt_status),
    INDEX idx_submitted_by (ser_submitted_by),
    INDEX idx_created_date (dte_created_date),
    INDEX idx_is_deleted (bl_is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verify table creation
SELECT 'Table cfg_tbl_custom_form_application created successfully!' AS Status;

-- Show table structure
DESCRIBE cfg_tbl_custom_form_application;

