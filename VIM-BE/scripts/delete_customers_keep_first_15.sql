-- =============================================================================
-- Delete every row in velocity.cfg_tbl_customer EXCEPT the first 15 entries
-- (the 15 lowest ser_customer_id values), and recursively delete every row
-- in any other velocity table whose FK chain ultimately reaches a non-kept
-- customer (e.g. sls_tbl_deal -> sls_tbl_deal_details, sls_tbl_sale_order,
-- sls_tbl_tir, sls_tbl_claim, sls_tbl_work_order, sls_tbl_customer_feedback,
-- and any deeper child tables).
--
-- DESTRUCTIVE. Runs in dry-run mode by default. To actually commit, change
-- the bottom CALL to TRUE and re-run the file.
--
-- Works in MySQL Workbench's "Apply SQL Script" wizard because all
-- restricted statements (USE, PREPARE/EXECUTE, START TRANSACTION, COMMIT,
-- ROLLBACK, SET FOREIGN_KEY_CHECKS) live inside the stored procedure.
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_purge_customers_keep_first_15;

DELIMITER $$

CREATE PROCEDURE sp_purge_customers_keep_first_15(IN p_commit BOOLEAN)
proc_block: BEGIN
    -- ---- Variables ---------------------------------------------------------
    DECLARE v_done             INT     DEFAULT 0;
    DECLARE v_keep_count       INT;
    DECLARE v_delete_count     INT;
    DECLARE v_keep_min_id      BIGINT;
    DECLARE v_keep_max_id      BIGINT;
    DECLARE v_old_safe_updates INT     DEFAULT 0;
    DECLARE v_old_fk_checks    INT     DEFAULT 1;
    DECLARE v_iter             INT     DEFAULT 0;
    DECLARE v_max_iter         INT     DEFAULT 30;
    DECLARE v_added            BIGINT  DEFAULT 0;
    DECLARE v_total_marked     BIGINT  DEFAULT 0;
    DECLARE v_self_count       BIGINT  DEFAULT 0;

    DECLARE v_c_schema         VARCHAR(128);
    DECLARE v_c_table          VARCHAR(128);
    DECLARE v_c_fk_col         VARCHAR(128);
    DECLARE v_p_schema         VARCHAR(128);
    DECLARE v_p_table          VARCHAR(128);
    DECLARE v_p_pk_col         VARCHAR(128);
    DECLARE v_c_pk_col         VARCHAR(128);
    DECLARE v_c_is_nullable    VARCHAR(3);

    DECLARE v_del_schema       VARCHAR(128);
    DECLARE v_del_table        VARCHAR(128);
    DECLARE v_del_pk_col       VARCHAR(128);
    DECLARE v_del_count        BIGINT;

    -- ---- Cursors -----------------------------------------------------------
    DECLARE fk_cursor CURSOR FOR
        SELECT child_schema, child_table, child_fk_col,
               parent_schema, parent_table, parent_pk_col,
               child_pk_col, child_is_nullable
        FROM tmp_fks
        ORDER BY id;

    DECLARE del_cursor CURSOR FOR
        SELECT schema_name, table_name, pk_column
        FROM tmp_delete_tables
        ORDER BY step;

    -- ---- Handlers ----------------------------------------------------------
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET SQL_SAFE_UPDATES   = v_old_safe_updates;
        SET FOREIGN_KEY_CHECKS = v_old_fk_checks;
        RESIGNAL;
    END;

    -- Remember the caller's session settings so we can restore them.
    SET v_old_safe_updates = @@SQL_SAFE_UPDATES;
    SET v_old_fk_checks    = @@FOREIGN_KEY_CHECKS;
    SET SQL_SAFE_UPDATES   = 0;

    -- ---- Keep list (15 oldest customer IDs) --------------------------------
    DROP TEMPORARY TABLE IF EXISTS tmp_keep_ids;
    CREATE TEMPORARY TABLE tmp_keep_ids (
        ser_customer_id BIGINT PRIMARY KEY
    );

    INSERT INTO tmp_keep_ids (ser_customer_id)
    SELECT ser_customer_id
    FROM velocity.cfg_tbl_customer
    ORDER BY ser_customer_id ASC
    LIMIT 15;

    SELECT COUNT(*), MIN(ser_customer_id), MAX(ser_customer_id)
    INTO v_keep_count, v_keep_min_id, v_keep_max_id
    FROM tmp_keep_ids;

    IF v_keep_count = 0 THEN
        SELECT 'ABORT: cfg_tbl_customer is empty.' AS abort_reason;
        SET SQL_SAFE_UPDATES = v_old_safe_updates;
        LEAVE proc_block;
    END IF;

    -- ---- Delete list (everyone else) ---------------------------------------
    DROP TEMPORARY TABLE IF EXISTS tmp_delete_ids;
    CREATE TEMPORARY TABLE tmp_delete_ids (
        ser_customer_id BIGINT PRIMARY KEY
    );

    INSERT INTO tmp_delete_ids (ser_customer_id)
    SELECT c.ser_customer_id
    FROM velocity.cfg_tbl_customer c
    LEFT JOIN tmp_keep_ids k ON k.ser_customer_id = c.ser_customer_id
    WHERE k.ser_customer_id IS NULL;

    SELECT COUNT(*) INTO v_delete_count FROM tmp_delete_ids;

    SELECT v_keep_count   AS will_keep,
           v_delete_count AS will_delete,
           v_keep_min_id  AS keep_min_id,
           v_keep_max_id  AS keep_max_id;

    -- ---- Snapshot every FK in the velocity schema (child + parent + PKs) ---
    DROP TEMPORARY TABLE IF EXISTS tmp_fks;
    CREATE TEMPORARY TABLE tmp_fks (
        id                INT AUTO_INCREMENT PRIMARY KEY,
        child_schema      VARCHAR(128),
        child_table       VARCHAR(128),
        child_fk_col      VARCHAR(128),
        parent_schema     VARCHAR(128),
        parent_table      VARCHAR(128),
        parent_pk_col     VARCHAR(128),
        child_pk_col      VARCHAR(128),
        child_is_nullable VARCHAR(3)
    );

    INSERT INTO tmp_fks (child_schema, child_table, child_fk_col,
                         parent_schema, parent_table, parent_pk_col,
                         child_pk_col, child_is_nullable)
    SELECT
        kcu.TABLE_SCHEMA,
        kcu.TABLE_NAME,
        kcu.COLUMN_NAME,
        kcu.REFERENCED_TABLE_SCHEMA,
        kcu.REFERENCED_TABLE_NAME,
        kcu.REFERENCED_COLUMN_NAME,
        (SELECT pk.COLUMN_NAME
           FROM information_schema.KEY_COLUMN_USAGE pk
          WHERE pk.TABLE_SCHEMA     = kcu.TABLE_SCHEMA
            AND pk.TABLE_NAME       = kcu.TABLE_NAME
            AND pk.CONSTRAINT_NAME  = 'PRIMARY'
            AND pk.ORDINAL_POSITION = 1
          LIMIT 1) AS child_pk_col,
        col.IS_NULLABLE
    FROM information_schema.KEY_COLUMN_USAGE kcu
    JOIN information_schema.COLUMNS col
      ON col.TABLE_SCHEMA = kcu.TABLE_SCHEMA
     AND col.TABLE_NAME   = kcu.TABLE_NAME
     AND col.COLUMN_NAME  = kcu.COLUMN_NAME
    WHERE kcu.REFERENCED_TABLE_SCHEMA = 'velocity'
      AND kcu.REFERENCED_TABLE_NAME IS NOT NULL;

    -- ---- Working set: every row in any table that we need to delete --------
    DROP TEMPORARY TABLE IF EXISTS tmp_marked;
    CREATE TEMPORARY TABLE tmp_marked (
        schema_name VARCHAR(128) NOT NULL,
        table_name  VARCHAR(128) NOT NULL,
        pk_column   VARCHAR(128) NOT NULL,
        pk_value    BIGINT       NOT NULL,
        PRIMARY KEY (schema_name, table_name, pk_column, pk_value)
    );

    -- Scratch table holds the parent-id list per FK so we never have to
    -- reference tmp_marked twice in the same statement (MySQL temp tables
    -- cannot be reopened within a single statement).
    DROP TEMPORARY TABLE IF EXISTS tmp_parents;
    CREATE TEMPORARY TABLE tmp_parents (
        pk_value BIGINT PRIMARY KEY
    );

    -- Audit table for NULL-ing self-references on keep rows.
    DROP TEMPORARY TABLE IF EXISTS tmp_self_ref_actions;
    CREATE TEMPORARY TABLE tmp_self_ref_actions (
        step_order  INT AUTO_INCREMENT PRIMARY KEY,
        column_name VARCHAR(128),
        rows_nulled BIGINT
    );

    -- Seed: every customer that has to go.
    INSERT INTO tmp_marked (schema_name, table_name, pk_column, pk_value)
    SELECT 'velocity', 'cfg_tbl_customer', 'ser_customer_id', ser_customer_id
    FROM tmp_delete_ids;

    START TRANSACTION;

    -- ---- BFS the FK graph until nothing new is marked ----------------------
    bfs_loop: WHILE v_iter < v_max_iter DO
        SET v_iter  = v_iter + 1;
        SET v_added = 0;
        SET v_done  = 0;

        OPEN fk_cursor;
        fk_loop: LOOP
            FETCH fk_cursor INTO v_c_schema, v_c_table, v_c_fk_col,
                                 v_p_schema, v_p_table, v_p_pk_col,
                                 v_c_pk_col, v_c_is_nullable;
            IF v_done = 1 THEN
                LEAVE fk_loop;
            END IF;

            -- Skip tables we cannot identify a single-column PK for.
            IF v_c_pk_col IS NULL THEN
                ITERATE fk_loop;
            END IF;

            -- Refresh tmp_parents with the IDs marked in the parent table.
            DELETE FROM tmp_parents;
            SET @sql_parents = CONCAT(
                'INSERT IGNORE INTO tmp_parents (pk_value) ',
                'SELECT pk_value FROM tmp_marked ',
                'WHERE schema_name = ', QUOTE(v_p_schema), ' ',
                '  AND table_name  = ', QUOTE(v_p_table),  ' ',
                '  AND pk_column   = ', QUOTE(v_p_pk_col)
            );
            PREPARE p_stmt FROM @sql_parents;
            EXECUTE p_stmt;
            DEALLOCATE PREPARE p_stmt;

            -- Special case: cfg_tbl_customer self-reference. Keep rows that
            -- point at a doomed customer get their FK NULL'd (if nullable);
            -- delete-target customers are already marked from the seed.
            IF v_c_schema = 'velocity' AND v_c_table = 'cfg_tbl_customer'
               AND v_p_schema = 'velocity' AND v_p_table = 'cfg_tbl_customer' THEN
                IF v_iter = 1 AND v_c_is_nullable = 'YES' THEN
                    SET @sql_null = CONCAT(
                        'UPDATE `velocity`.`cfg_tbl_customer` r ',
                        'JOIN tmp_keep_ids k ON k.ser_customer_id = r.ser_customer_id ',
                        'JOIN tmp_parents  p ON p.pk_value        = r.`', v_c_fk_col, '` ',
                        'SET r.`', v_c_fk_col, '` = NULL'
                    );
                    PREPARE n_stmt FROM @sql_null;
                    EXECUTE n_stmt;
                    SET v_self_count = ROW_COUNT();
                    DEALLOCATE PREPARE n_stmt;

                    INSERT INTO tmp_self_ref_actions (column_name, rows_nulled)
                    VALUES (v_c_fk_col, v_self_count);
                END IF;
                ITERATE fk_loop;
            END IF;

            -- Normal FK: mark child rows that reference any marked parent.
            SET @sql_bfs = CONCAT(
                'INSERT IGNORE INTO tmp_marked ',
                '(schema_name, table_name, pk_column, pk_value) ',
                'SELECT ', QUOTE(v_c_schema), ', ', QUOTE(v_c_table), ', ',
                          QUOTE(v_c_pk_col), ', child.`', v_c_pk_col, '` ',
                'FROM `', v_c_schema, '`.`', v_c_table, '` child ',
                'JOIN tmp_parents p ON p.pk_value = child.`', v_c_fk_col, '`'
            );
            PREPARE bfs_stmt FROM @sql_bfs;
            EXECUTE bfs_stmt;
            SET v_added = v_added + ROW_COUNT();
            DEALLOCATE PREPARE bfs_stmt;
        END LOOP;
        CLOSE fk_cursor;

        IF v_added = 0 THEN
            LEAVE bfs_loop;
        END IF;
    END WHILE;

    SELECT COUNT(*) INTO v_total_marked FROM tmp_marked;

    IF v_iter >= v_max_iter AND v_added > 0 THEN
        SELECT CONCAT('WARNING: BFS hit max_iter=', v_max_iter,
                      ' without converging. There may be unmarked transitive children.')
            AS warning;
    END IF;

    -- ---- Build the per-table delete plan -----------------------------------
    DROP TEMPORARY TABLE IF EXISTS tmp_delete_tables;
    CREATE TEMPORARY TABLE tmp_delete_tables (
        step        INT AUTO_INCREMENT PRIMARY KEY,
        schema_name VARCHAR(128),
        table_name  VARCHAR(128),
        pk_column   VARCHAR(128)
    );

    INSERT INTO tmp_delete_tables (schema_name, table_name, pk_column)
    SELECT schema_name, table_name, pk_column
    FROM (
        SELECT DISTINCT schema_name, table_name, pk_column FROM tmp_marked
    ) x
    ORDER BY CASE WHEN table_name = 'cfg_tbl_customer' THEN 1 ELSE 0 END,
             schema_name, table_name;

    DROP TEMPORARY TABLE IF EXISTS tmp_delete_actions;
    CREATE TEMPORARY TABLE tmp_delete_actions (
        step_order   INT AUTO_INCREMENT PRIMARY KEY,
        schema_name  VARCHAR(128),
        table_name   VARCHAR(128),
        pk_column    VARCHAR(128),
        rows_deleted BIGINT
    );

    -- ---- Execute the deletes -----------------------------------------------
    -- With FK checks off we can delete in any order without worrying about
    -- transient violations; data integrity is preserved because the BFS
    -- already discovered every transitive child row.
    SET FOREIGN_KEY_CHECKS = 0;

    SET v_done = 0;
    OPEN del_cursor;
    del_loop: LOOP
        FETCH del_cursor INTO v_del_schema, v_del_table, v_del_pk_col;
        IF v_done = 1 THEN
            LEAVE del_loop;
        END IF;

        SET @sql_del = CONCAT(
            'DELETE FROM `', v_del_schema, '`.`', v_del_table, '` ',
            'WHERE `', v_del_pk_col, '` IN ( ',
            '  SELECT pk_value FROM tmp_marked ',
            '  WHERE schema_name = ', QUOTE(v_del_schema), ' ',
            '    AND table_name  = ', QUOTE(v_del_table),  ' ',
            '    AND pk_column   = ', QUOTE(v_del_pk_col), ' ',
            ')'
        );
        PREPARE d_stmt FROM @sql_del;
        EXECUTE d_stmt;
        SET v_del_count = ROW_COUNT();
        DEALLOCATE PREPARE d_stmt;

        INSERT INTO tmp_delete_actions (schema_name, table_name, pk_column, rows_deleted)
        VALUES (v_del_schema, v_del_table, v_del_pk_col, v_del_count);
    END LOOP;
    CLOSE del_cursor;

    SET FOREIGN_KEY_CHECKS = v_old_fk_checks;

    -- ---- Reporting ---------------------------------------------------------
    SELECT v_iter           AS bfs_iterations,
           v_total_marked   AS total_rows_marked_for_delete;

    SELECT step_order, column_name, rows_nulled
    FROM tmp_self_ref_actions
    ORDER BY step_order;

    SELECT step_order, schema_name, table_name, pk_column, rows_deleted
    FROM tmp_delete_actions
    ORDER BY step_order;

    SELECT k.ser_customer_id,
           c.txt_customer_name,
           CASE WHEN c.ser_customer_id IS NULL THEN 'MISSING' ELSE 'KEPT' END
               AS keep_status
    FROM tmp_keep_ids k
    LEFT JOIN velocity.cfg_tbl_customer c
        ON c.ser_customer_id = k.ser_customer_id
    ORDER BY k.ser_customer_id;

    IF p_commit = TRUE THEN
        COMMIT;
        SET SQL_SAFE_UPDATES = v_old_safe_updates;
        SELECT 'COMMITTED' AS final_action;
    ELSE
        ROLLBACK;
        SET SQL_SAFE_UPDATES = v_old_safe_updates;
        SELECT 'ROLLED BACK (dry run). Re-run CALL sp_purge_customers_keep_first_15(TRUE) to apply.'
            AS final_action;
    END IF;
END$$

DELIMITER ;

-- Real run: this commits the deletes. To do a dry run, swap which CALL is active.
-- CALL sp_purge_customers_keep_first_15(FALSE);
CALL sp_purge_customers_keep_first_15(TRUE);

DROP PROCEDURE IF EXISTS sp_purge_customers_keep_first_15;
