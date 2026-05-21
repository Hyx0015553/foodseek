-- 用户系统通知表（消息-小程序「消息-系统-系统通知」、探店计划 T-3 定时写入）
-- 表前缀默认为 fs_，若 application 里 tableprefix 不同请改表名。
CREATE TABLE IF NOT EXISTS `fs_system_notify` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正文',
  `ndate` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建时间',
  `type` int NULL DEFAULT NULL COMMENT '0=已读 1=未读',
  `msgtype` int NULL DEFAULT NULL COMMENT '子类型：20=探店计划到店日前第3天提醒',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_system_notify_uid` (`uid`) USING BTREE,
  KEY `idx_system_notify_uid_type` (`uid`, `type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户系统通知（消息-系统）' ROW_FORMAT=DYNAMIC;
