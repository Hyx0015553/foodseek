-- 探店计划表（执行前请确认库名与表前缀 fs_ 一致）
CREATE TABLE IF NOT EXISTS `fs_blogplan` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uid` int(11) DEFAULT NULL COMMENT '用户id',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `sid` int(11) DEFAULT NULL COMMENT '店铺id',
  `stitle` varchar(200) DEFAULT NULL COMMENT '店铺名称快照',
  `plantime` varchar(50) DEFAULT NULL COMMENT '计划到店时间',
  `note` varchar(500) DEFAULT NULL COMMENT '计划备注',
  `state` varchar(32) DEFAULT NULL COMMENT '状态：待探店/已完成/已取消',
  `ndate` varchar(50) DEFAULT NULL COMMENT '创建/更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_blogplan_uid` (`uid`),
  KEY `idx_blogplan_sid` (`sid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探店计划';
