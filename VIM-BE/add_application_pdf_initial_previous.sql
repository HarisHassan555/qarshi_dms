-- Add columns to store first and previous PDF versions for send-back behavior.
-- Run this once on your database (e.g. vim_3 or your schema).

ALTER TABLE cfg_tbl_custom_form_application
ADD COLUMN blb_pdf_data_initial LONGBLOB NULL COMMENT 'Very first PDF (at submission)' AFTER blb_pdf_data,
ADD COLUMN blb_pdf_data_previous LONGBLOB NULL COMMENT 'PDF one version before current' AFTER blb_pdf_data_initial;
