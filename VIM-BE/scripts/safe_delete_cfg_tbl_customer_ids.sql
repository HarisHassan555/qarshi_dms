-- =============================================================================
-- Safely delete scoped cfg_tbl_customer rows from `velocity` except selected
-- rows. Works in MySQL Workbench's "Apply SQL Script" wizard (which uses the
-- binary prepared statement protocol and therefore rejects top-level USE,
-- PREPARE/EXECUTE, START TRANSACTION, COMMIT, and ROLLBACK).
--
-- All sensitive operations are wrapped inside a stored procedure where those
-- statements are allowed. The script:
--   1) discovers every FK that references velocity.cfg_tbl_customer.ser_customer_id,
--   2) reports blocker rows by referencing table/column/id,
--   3) deletes only unreferenced scoped rows outside the keep list,
--   4) ROLLBACKs by default. Pass TRUE to sp_safe_delete_customers to COMMIT.
--
-- How to use:
--   - Edit the keep list and scope list inside sp_safe_delete_customers below.
--   - Run the whole file. The procedure runs in dry-run mode (FALSE) by default.
--   - Review the result sets, then re-run with CALL sp_safe_delete_customers(TRUE).
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_safe_delete_customers;

DELIMITER $$

CREATE PROCEDURE sp_safe_delete_customers(IN p_commit BOOLEAN)
proc_block: BEGIN
    DECLARE v_done       INT DEFAULT 0;
    DECLARE v_schema     VARCHAR(128);
    DECLARE v_table      VARCHAR(128);
    DECLARE v_column     VARCHAR(128);
    DECLARE v_keep_count INT DEFAULT 0;

    DECLARE fk_cursor CURSOR FOR
        SELECT kcu.TABLE_SCHEMA, kcu.TABLE_NAME, kcu.COLUMN_NAME
        FROM information_schema.KEY_COLUMN_USAGE kcu
        WHERE kcu.REFERENCED_TABLE_SCHEMA = 'velocity'
          AND kcu.REFERENCED_TABLE_NAME   = 'cfg_tbl_customer'
          AND kcu.REFERENCED_COLUMN_NAME  = 'ser_customer_id';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SET SESSION group_concat_max_len = 1000000;

    -- -----------------------------------------------------------------------
    -- Scope list: customer IDs that the script is ALLOWED to touch.
    -- Keep this narrow to avoid accidental mass deletion.
    -- -----------------------------------------------------------------------
    DROP TEMPORARY TABLE IF EXISTS tmp_customer_scope_ids;
    CREATE TEMPORARY TABLE tmp_customer_scope_ids (
        ser_customer_id BIGINT PRIMARY KEY
    );

    INSERT INTO tmp_customer_scope_ids (ser_customer_id) VALUES
        (65), (66), (67), (68), (69),
        (70), (71), (72), (73), (74), (75);

    -- -----------------------------------------------------------------------
    -- Keep list: customer IDs inside the scope that must NOT be deleted.
    -- -----------------------------------------------------------------------
    DROP TEMPORARY TABLE IF EXISTS tmp_customer_keep_ids;
    CREATE TEMPORARY TABLE tmp_customer_keep_ids (
        ser_customer_id BIGINT PRIMARY KEY
    );

    INSERT INTO tmp_customer_keep_ids (ser_customer_id) VALUES
        (65);

    -- Sanity check: refuse to run if the keep list is empty, to avoid wiping
    -- the entire scoped range by accident.
    SELECT COUNT(*) INTO v_keep_count FROM tmp_customer_keep_ids;
    IF v_keep_count = 0 THEN
        SELECT 'ABORT: keep list is empty. Refusing to delete the entire scope.'
            AS abort_reason;
        LEAVE proc_block;
    END IF;

    -- -----------------------------------------------------------------------
    -- Delete candidates: scope minus keep list.
    -- -----------------------------------------------------------------------
    DROP TEMPORARY TABLE IF EXISTS tmp_customer_delete_ids;
    CREATE TEMPORARY TABLE tmp_customer_delete_ids (
        ser_customer_id BIGINT PRIMARY KEY
    );

    INSERT INTO tmp_customer_delete_ids (ser_customer_id)
    SELECT s.ser_customer_id
    FROM tmp_customer_scope_ids s
    LEFT JOIN tmp_customer_keep_ids k
        ON k.ser_customer_id = s.ser_customer_id
    WHERE k.ser_customer_id IS NULL;

    -- -----------------------------------------------------------------------
    -- Blocker collection table.
    -- -----------------------------------------------------------------------
    DROP TEMPORARY TABLE IF EXISTS tmp_customer_delete_blockers;
    CREATE TEMPORARY TABLE tmp_customer_delete_blockers (
        referencing_table  VARCHAR(128) NOT NULL,
        referencing_column VARCHAR(128) NOT NULL,
        ser_customer_id    BIGINT       NOT NULL,
        referenced_rows    BIGINT       NOT NULL,
        KEY idx_customer_id (ser_customer_id)
    );

    -- -----------------------------------------------------------------------
    -- Walk every FK pointing at velocity.cfg_tbl_customer.ser_customer_id and
    -- count rows referencing each delete-candidate id.
    -- -----------------------------------------------------------------------
    OPEN fk_cursor;
    fk_loop: LOOP
        FETCH fk_cursor INTO v_schema, v_table, v_column;
        IF v_done = 1 THEN
            LEAVE fk_loop;
        END IF;

        SET @blocker_sql = CONCAT(
            'INSERT INTO tmp_customer_delete_blockers ',
            '(referencing_table, referencing_column, ser_customer_id, referenced_rows) ',
            'SELECT ', QUOTE(v_table), ', ', QUOTE(v_column), ', ',
            't.ser_customer_id, COUNT(*) ',
            'FROM tmp_customer_delete_ids t ',
            'JOIN `', v_schema, '`.`', v_table, '` r ',
            'ON r.`', v_column, '` = t.ser_customer_id ',
            'GROUP BY t.ser_customer_id ',
            'HAVING COUNT(*) > 0'
        );

        PREPARE blocker_stmt FROM @blocker_sql;
        EXECUTE blocker_stmt;
        DEALLOCATE PREPARE blocker_stmt;
    END LOOP;
    CLOSE fk_cursor;

    -- -----------------------------------------------------------------------
    -- Reporting result sets (returned to the client by CALL).
    -- -----------------------------------------------------------------------
    SELECT k.ser_customer_id,
           CASE WHEN c.ser_customer_id IS NULL THEN 'MISSING' ELSE 'EXISTS' END
               AS keep_status,
           c.txt_customer_name
    FROM tmp_customer_keep_ids k
    LEFT JOIN velocity.cfg_tbl_customer c
        ON c.ser_customer_id = k.ser_customer_id
    ORDER BY k.ser_customer_id;

    SELECT COUNT(*) AS candidate_customers_in_scope_outside_keep_list
    FROM tmp_customer_delete_ids;

    SELECT referencing_table, referencing_column, ser_customer_id, referenced_rows
    FROM tmp_customer_delete_blockers
    ORDER BY ser_customer_id, referencing_table, referencing_column;

    SELECT t.ser_customer_id,
           CASE
               WHEN c.ser_customer_id IS NULL
                   THEN 'SKIP: customer row does not exist'
               WHEN EXISTS (
                   SELECT 1 FROM tmp_customer_delete_blockers b
                   WHERE b.ser_customer_id = t.ser_customer_id
               )
                   THEN 'BLOCKED: referenced by child rows'
               ELSE 'READY: will be deleted'
           END AS delete_decision
    FROM tmp_customer_delete_ids t
    LEFT JOIN velocity.cfg_tbl_customer c
        ON c.ser_customer_id = t.ser_customer_id
    ORDER BY t.ser_customer_id;

    -- -----------------------------------------------------------------------
    -- Transactional delete. Only unreferenced rows in the candidate set are
    -- deleted; if anything fails inside the transaction we roll back.
    -- -----------------------------------------------------------------------
    START TRANSACTION;

    DELETE c
    FROM velocity.cfg_tbl_customer c
    JOIN tmp_customer_delete_ids t
        ON t.ser_customer_id = c.ser_customer_id
    WHERE NOT EXISTS (
        SELECT 1 FROM tmp_customer_delete_blockers b
        WHERE b.ser_customer_id = c.ser_customer_id
    );

    SELECT ROW_COUNT() AS deleted_customers_in_transaction;

    SELECT t.ser_customer_id,
           CASE WHEN c.ser_customer_id IS NULL
                   THEN 'NOT PRESENT AFTER DELETE'
                ELSE 'STILL PRESENT'
           END AS post_delete_status
    FROM tmp_customer_delete_ids t
    LEFT JOIN velocity.cfg_tbl_customer c
        ON c.ser_customer_id = t.ser_customer_id
    ORDER BY t.ser_customer_id;

    IF p_commit = TRUE THEN
        COMMIT;
        SELECT 'COMMITTED' AS final_action;
    ELSE
        ROLLBACK;
        SELECT 'ROLLED BACK (dry run). Re-run CALL sp_safe_delete_customers(TRUE) to apply.'
            AS final_action;
    END IF;
END$$

DELIMITER ;

-- Toggle the CALLs to switch between dry run and apply. Run exactly one.
-- CALL sp_safe_delete_customers(FALSE);
CALL sp_safe_delete_customers(TRUE);

DROP PROCEDURE IF EXISTS sp_safe_delete_customers;
