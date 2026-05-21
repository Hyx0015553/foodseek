-- 「我的订单」列表移除（非物理删单）。表前缀默认 fs_；亦可依赖 BillMessageSchemaInitializer 启动时自动 ALTER。
ALTER TABLE `fs_bill`
  ADD COLUMN `user_delete` int DEFAULT NULL
  COMMENT '0/null=展示；1=用户从订单列表移除（非删单）';
