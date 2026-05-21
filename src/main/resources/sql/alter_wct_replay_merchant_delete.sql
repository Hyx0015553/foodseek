-- 商家「评价管理」：列表移除标记（非物理删除）。表前缀默认 fs_，若 application 中 tableprefix 不同请改表名。
-- 亦可依赖 Spring 启动时 ReplayMerchantDeleteSchemaInitializer 自动 ALTER。
ALTER TABLE `fs_replay`
  ADD COLUMN `merchant_delete` int DEFAULT NULL
  COMMENT '0/null=展示；1=商家评价管理列表移除（非物理删除）';
