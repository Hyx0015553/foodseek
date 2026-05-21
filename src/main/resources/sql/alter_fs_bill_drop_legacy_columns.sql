-- 旧库升级：按实际存在的列逐条执行（已删除的列会报错，跳过即可）
ALTER TABLE `fs_bill` DROP COLUMN `msgevalhide`;
ALTER TABLE `fs_bill` DROP COLUMN `qid`;
ALTER TABLE `fs_bill` DROP COLUMN `qusername`;
ALTER TABLE `fs_bill` DROP COLUMN `qtel`;
ALTER TABLE `fs_bill` DROP COLUMN `bgcounts`;
ALTER TABLE `fs_bill` DROP COLUMN `address`;
ALTER TABLE `fs_bill` DROP COLUMN `pnote`;
ALTER TABLE `fs_bill` DROP COLUMN `shnote`;
ALTER TABLE `fs_bill` DROP COLUMN `shtype`;
ALTER TABLE `fs_bill` DROP COLUMN `shstatecn`;
ALTER TABLE `fs_bill` DROP COLUMN `fhnote`;
ALTER TABLE `fs_bill` DROP COLUMN `kid`;
ALTER TABLE `fs_bill` DROP COLUMN `ktitle`;
ALTER TABLE `fs_bill` DROP COLUMN `kcode`;
