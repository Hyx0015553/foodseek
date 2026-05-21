-- 线上库迁移：表重命名 + 删列（列已不存在时对应语句会失败，可忽略）
RENAME TABLE `fs_huihua` TO `fs_shop_qa_notice`;

ALTER TABLE `fs_shop_qa_notice` DROP COLUMN `zan`;
ALTER TABLE `fs_shop_qa_notice` DROP COLUMN `attach`;
ALTER TABLE `fs_shop_qa_notice` DROP COLUMN `attachname`;
ALTER TABLE `fs_shop_qa_notice` DROP COLUMN `biztype`;
ALTER TABLE `fs_shop_qa_notice` DROP COLUMN `qnote`;
ALTER TABLE `fs_shop_qa_notice` DROP COLUMN `anote`;

-- 索引名若仍为 idx_huihua_*，可按需重命名（非必须）：
-- ALTER TABLE `fs_shop_qa_notice` RENAME INDEX `idx_huihua_uid_msgtype` TO `idx_shop_qa_notice_uid_msgtype`;
