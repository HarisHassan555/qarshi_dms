-- =====================================================
-- All Applications page data scope permissions
-- Table: all_application_page_permission
-- Purpose: control which applications a role (and optional user)
--          can see on /velocity/all-applications
-- Database: velocity
-- =====================================================

USE velocity;

CREATE TABLE IF NOT EXISTS all_application_page_permission (
    ser_permission_id INT NOT NULL AUTO_INCREMENT,
    ser_role_id INT NOT NULL COMMENT 'Role that receives this scope',
    ser_user_id INT NULL COMMENT 'Optional viewer user; NULL = whole role',
    ser_department_id INT NULL COMMENT 'Show apps submitted from this department',
    ser_submitter_user_id INT NULL COMMENT 'Show apps submitted by this user',
    ser_approver_user_id INT NULL COMMENT 'Show apps currently with / approved by this user',
    ser_form_id INT NULL COMMENT 'Optional template/form filter',
    ser_application_id INT NULL COMMENT 'Optional specific application id',
    bl_is_enabled BIT(1) NULL DEFAULT b'1',
    bl_is_deleted BIT(1) NULL DEFAULT b'0',
    bln_status BIT(1) NULL DEFAULT b'1',
    dte_created_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user INT NULL,
    ser_modified_user INT NULL,
    PRIMARY KEY (ser_permission_id),
    KEY idx_all_app_page_perm_role (ser_role_id),
    KEY idx_all_app_page_perm_user (ser_user_id),
    KEY idx_all_app_page_perm_dept (ser_department_id),
    KEY idx_all_app_page_perm_submitter (ser_submitter_user_id),
    KEY idx_all_app_page_perm_approver (ser_approver_user_id),
    KEY idx_all_app_page_perm_form (ser_form_id),
    KEY idx_all_app_page_perm_app (ser_application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notes:
-- 1) Menu access still comes from cfg_tbl_sub_menu_role (all-applications View).
-- 2) If a role/user has ZERO enabled rows here => unrestricted list (backward compatible).
-- 3) If one or more rows exist => show apps matching ANY row (OR).
-- 4) Within a row, non-null columns are combined with AND.
