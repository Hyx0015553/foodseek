-- 系统消息表：note 原多为 varchar(500)，无法保存 AI 运营决策/整改等长文。
-- 请在业务库执行一次（表名带项目前缀 fs_，若不同请改表名）。
-- MySQL 5.7+ / 8.0

ALTER TABLE fs_sysmsg MODIFY COLUMN note TEXT NULL COMMENT '详情/正文（公告、系统通知、AI建议等）';
