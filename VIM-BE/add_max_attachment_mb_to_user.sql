-- Per-user max attachment size (MB). NULL / unset means default 5 MB in application code.
ALTER TABLE cfg_tbl_user
ADD COLUMN num_max_attachment_mb INT NULL;
