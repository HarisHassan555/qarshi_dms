-- Run this in MySQL Workbench (or mysql CLI) BEFORE restoring the dump.
SET GLOBAL innodb_default_row_format = 'DYNAMIC';
SET GLOBAL innodb_strict_mode = 0;

DROP DATABASE IF EXISTS velocity;
CREATE DATABASE velocity CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE velocity;
