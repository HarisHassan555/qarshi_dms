-- Optional supporting document when admin records delegation without on-behalf user consent
-- Database: velocity (change USE if your schema differs)

USE velocity;

ALTER TABLE cfg_tbl_user_delegation
    ADD COLUMN txt_attachment_path VARCHAR(512) NULL
        COMMENT 'Relative path under ~/.vim_dms_uploads (user-delegations/)'
        AFTER txt_notes,
    ADD COLUMN txt_attachment_name VARCHAR(255) NULL
        COMMENT 'Original uploaded file name for display/download'
        AFTER txt_attachment_path;

SELECT 'cfg_tbl_user_delegation attachment columns added' AS message;
