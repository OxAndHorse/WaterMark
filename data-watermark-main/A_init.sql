CREATE USER 'pkc_test'@'%' IDENTIFIED BY 'pkcgroup2023';
CREATE DATABASE IF NOT EXISTS `pkc_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
GRANT ALL PRIVILEGES ON pkc_db.* TO 'pkc_test'@'%';
FLUSH PRIVILEGES;

use dw;
DROP TABLE IF EXISTS demo_user;
CREATE TABLE IF NOT EXISTS demo_user (
    username VARCHAR(255) PRIMARY KEY COMMENT 'login username',
	uid VARCHAR(64) COMMENT 'user id',
    nickname TEXT NOT NULL,
	password VARCHAR(64) COMMENT 'hash-based password with hex encoding',
    role INT DEFAULT 0 COMMENT '0 for admin; 1 for owner; 2 for user',
    system_id VARCHAR(64) DEFAULT NULL,
    remark TEXT DEFAULT NULL
)ENGINE = INNODB DEFAULT CHARSET = utf8;
INSERT INTO demo_user (username,uid,nickname,password,role) VALUES
('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', '管理员','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 0);
-- ('owner', '4c1029697ee358715d3a14a2add817c4b01651440de808371f78165ac90dc581', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1),
-- ('dataUser', '40f3117f555534da639a067bbf8d609404de2d0469965619de4574c2fc15561d', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2);

CREATE TABLE IF NOT EXISTS app_register_info (
	id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
	system_id VARCHAR(64) NOT NULL,
    system_auth_key VARCHAR(64) NOT NULL,
	system_name VARCHAR(255) NOT NULL,
    system_nickname TEXT,
    functions INT DEFAULT false,
    dw_usage INT DEFAULT false,
    db_name VARCHAR(255),
    db_ip VARCHAR(32),
    db_port VARCHAR(8),
    db_type VARCHAR(32) DEFAULT 'MySQL',
    secret_seed VARCHAR(128)
)ENGINE = INNODB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `trace_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `system_id` varchar(64) NOT NULL,
  `db_name` varchar(255) DEFAULT NULL,
  `table_name` varchar(255) DEFAULT NULL,
  `primary_keys` text,
  `wm_fields` text,
  `allow_row_expansion` boolean,
  `row_expansion_algorithm` varchar(255) DEFAULT NULL,
  `allow_column_expansion` boolean,
  `column_expansion_algorithm` text DEFAULT NULL,
  `watermark` varchar(255) DEFAULT NULL,
  `embedded_msg` text,
  `record_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE IF NOT EXISTS numeric_opt_aux_info (
	id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    system_id VARCHAR(64) NOT NULL,
    watermark VARCHAR(255) DEFAULT NULL,
	threshold DOUBLE DEFAULT NULL,
	secret_code DOUBLE DEFAULT NULL,
    secret_key VARCHAR(255) DEFAULT NULL comment 'used for partition',
    db_name VARCHAR(255) DEFAULT NULL,
    table_name VARCHAR(255) DEFAULT NULL,
    wm_field VARCHAR(255) DEFAULT NULL,
	partition_count INT(11) DEFAULT NULL,
	embedded_msg TEXT
) ENGINE=INNODB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
