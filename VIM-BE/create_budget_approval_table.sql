-- =====================================================
-- SQL Script: Create Budget Approval Table
-- Database: vim_3
-- Description: Creates table for storing budget approval notes
-- =====================================================

USE vim_3;

-- =====================================================
-- Table: budget_approval
-- Description: Stores budget approval notes and history
-- =====================================================
CREATE TABLE IF NOT EXISTS budget_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content LONGTEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Verification Queries
-- =====================================================
SELECT 'Table budget_approval Created Successfully' AS Status;

SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'budget_approval';
