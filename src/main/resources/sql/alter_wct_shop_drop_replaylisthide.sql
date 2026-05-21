-- 评价「列表移除」已迁至 fs_replay.merchant_delete，废弃店铺表 JSON 列。
-- 若列不存在会报错 1091，可忽略。
ALTER TABLE `fs_shop` DROP COLUMN `replaylisthide`;
