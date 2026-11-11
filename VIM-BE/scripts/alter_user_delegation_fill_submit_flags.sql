-- =====================================================
-- Delegation: separate Form Fill vs Form Submit on-behalf flags
-- Database: velocity (change USE if your schema differs)
-- Defaults to 1 so existing rows keep current fill+submit behavior.
-- =====================================================

USE velocity;

-- Ignore "Duplicate column name" if columns already exist.
ALTER TABLE cfg_tbl_user_delegation
    ADD COLUMN bln_allow_form_fill_on_behalf TINYINT(1) NOT NULL DEFAULT 1
        COMMENT 'Delegate may save/edit drafts on behalf of on-behalf user' AFTER txt_notes;

ALTER TABLE cfg_tbl_user_delegation
    ADD COLUMN bln_allow_form_submit_on_behalf TINYINT(1) NOT NULL DEFAULT 1
        COMMENT 'Delegate may submit forms on behalf of on-behalf user' AFTER bln_allow_form_fill_on_behalf;

UPDATE cfg_tbl_user_delegation
SET bln_allow_form_fill_on_behalf = 1
WHERE bln_allow_form_fill_on_behalf IS NULL;

UPDATE cfg_tbl_user_delegation
SET bln_allow_form_submit_on_behalf = 1
WHERE bln_allow_form_submit_on_behalf IS NULL;

SELECT 'cfg_tbl_user_delegation fill/submit flags ready' AS message;
