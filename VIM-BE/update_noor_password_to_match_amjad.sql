USE velocity_workbench;

-- =====================================================
-- Copy Amjad Aqeel's password to Noor Munir
-- Source: ser_user_id 401 / amjad.aqeel@qarshi.com
-- Target: ser_user_id 406 / noor.munir@qarshi.com
-- =====================================================

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- Step 1: Get Amjad's BCrypt password hash
SET @amjad_password = (
    SELECT txt_password
    FROM cfg_tbl_user
    WHERE (
        ser_user_id = 401
        OR LOWER(TRIM(txt_address)) = 'amjad.aqeel@qarshi.com'
        OR LOWER(TRIM(txt_user_name)) IN ('amjad.aqeel', 'amjad.aqeel@qarshi.com', 'amjad aqeel')
    )
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY
        CASE
            WHEN ser_user_id = 401 THEN 1
            WHEN LOWER(TRIM(txt_address)) = 'amjad.aqeel@qarshi.com' THEN 2
            ELSE 3
        END
    LIMIT 1
);

SET @amjad_user_id = (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE (
        ser_user_id = 401
        OR LOWER(TRIM(txt_address)) = 'amjad.aqeel@qarshi.com'
        OR LOWER(TRIM(txt_user_name)) IN ('amjad.aqeel', 'amjad.aqeel@qarshi.com', 'amjad aqeel')
    )
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY
        CASE
            WHEN ser_user_id = 401 THEN 1
            WHEN LOWER(TRIM(txt_address)) = 'amjad.aqeel@qarshi.com' THEN 2
            ELSE 3
        END
    LIMIT 1
);

SET @noor_user_id = (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE (
        ser_user_id = 406
        OR LOWER(TRIM(txt_address)) = 'noor.munir@qarshi.com'
        OR LOWER(TRIM(txt_user_name)) IN ('noor.munir', 'noor.munir@qarshi.com', 'noor munir')
    )
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY
        CASE
            WHEN ser_user_id = 406 THEN 1
            WHEN LOWER(TRIM(txt_address)) = 'noor.munir@qarshi.com' THEN 2
            ELSE 3
        END
    LIMIT 1
);

SELECT
    @amjad_user_id AS 'Amjad User ID',
    @noor_user_id AS 'Noor User ID',
    CASE
        WHEN @amjad_password IS NOT NULL THEN CONCAT('Amjad hash: ', LEFT(@amjad_password, 27), '...')
        ELSE 'ERROR - Amjad Aqeel not found'
    END AS 'Password Source',
    CASE
        WHEN @amjad_user_id IS NULL THEN 'ERROR - Amjad user not found'
        WHEN @noor_user_id IS NULL THEN 'ERROR - Noor Munir not found'
        WHEN @amjad_password IS NULL THEN 'ERROR - Amjad password missing'
        ELSE 'OK - Ready to update'
    END AS 'Status';

-- Step 2: Update Noor to use Amjad's password hash
UPDATE cfg_tbl_user
SET
    txt_password = @amjad_password,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE ser_user_id = @noor_user_id
  AND @noor_user_id IS NOT NULL
  AND @amjad_password IS NOT NULL;

SELECT
    CASE
        WHEN @noor_user_id IS NULL THEN 'ERROR - Noor Munir not found'
        WHEN @amjad_password IS NULL THEN 'ERROR - Amjad password not found'
        WHEN ROW_COUNT() > 0 THEN 'SUCCESS - Noor Munir password updated to match Amjad Aqeel'
        ELSE 'WARNING - No rows updated (password may already match)'
    END AS 'Update Status';

-- Step 3: Verification
SELECT
    ser_user_id AS 'User ID',
    txt_user_name AS 'Username',
    txt_address AS 'Email',
    LEFT(txt_password, 27) AS 'Password Hash (prefix)',
    CASE
        WHEN txt_password = @amjad_password THEN 'Matches Amjad'
        ELSE 'Does NOT match Amjad'
    END AS 'Password Match',
    txt_role AS 'Role',
    txt_designation AS 'Designation',
    dte_modified_date AS 'Last Modified'
FROM cfg_tbl_user
WHERE ser_user_id IN (@amjad_user_id, @noor_user_id)
  AND @amjad_user_id IS NOT NULL
  AND @noor_user_id IS NOT NULL
ORDER BY ser_user_id;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
