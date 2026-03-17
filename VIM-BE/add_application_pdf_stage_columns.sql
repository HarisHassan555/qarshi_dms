-- One PDF per approval stage (max 10 stages). Run once on your database.
-- If you previously ran add_application_pdf_initial_previous.sql, you can optionally drop those columns
-- after running this (see bottom).

ALTER TABLE cfg_tbl_custom_form_application
ADD COLUMN blb_pdf_stage_0 LONGBLOB NULL COMMENT 'PDF for stage 0 (initiator)' AFTER blb_pdf_data,
ADD COLUMN blb_pdf_stage_1 LONGBLOB NULL COMMENT 'PDF for stage 1' AFTER blb_pdf_stage_0,
ADD COLUMN blb_pdf_stage_2 LONGBLOB NULL COMMENT 'PDF for stage 2' AFTER blb_pdf_stage_1,
ADD COLUMN blb_pdf_stage_3 LONGBLOB NULL COMMENT 'PDF for stage 3' AFTER blb_pdf_stage_2,
ADD COLUMN blb_pdf_stage_4 LONGBLOB NULL COMMENT 'PDF for stage 4' AFTER blb_pdf_stage_3,
ADD COLUMN blb_pdf_stage_5 LONGBLOB NULL COMMENT 'PDF for stage 5' AFTER blb_pdf_stage_4,
ADD COLUMN blb_pdf_stage_6 LONGBLOB NULL COMMENT 'PDF for stage 6' AFTER blb_pdf_stage_5,
ADD COLUMN blb_pdf_stage_7 LONGBLOB NULL COMMENT 'PDF for stage 7' AFTER blb_pdf_stage_6,
ADD COLUMN blb_pdf_stage_8 LONGBLOB NULL COMMENT 'PDF for stage 8' AFTER blb_pdf_stage_7,
ADD COLUMN blb_pdf_stage_9 LONGBLOB NULL COMMENT 'PDF for stage 9' AFTER blb_pdf_stage_8;

-- Optional: if you had run add_application_pdf_initial_previous.sql, uncomment to remove old columns:
-- ALTER TABLE cfg_tbl_custom_form_application
-- DROP COLUMN blb_pdf_data_initial,
-- DROP COLUMN blb_pdf_data_previous;
