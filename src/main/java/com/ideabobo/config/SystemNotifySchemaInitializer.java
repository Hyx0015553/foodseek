package com.ideabobo.config;

import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 用户系统通知表 {@code fs_system_notify}：与 {@link com.ideabobo.model.SystemNotify}、
 * {@link BlogplanVisitReminderScheduler}、小程序 {@code listj/savej/deletej(table:'system_notify')} 一致。
 * <p>启动时：若无表则建表；若旧表缺 {@code msgtype} 则补列。</p>
 */
@Component
public class SystemNotifySchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void ensureSystemNotifySchema() {
        createTableIfNotExists();
        addColumnIfMissing(
                "msgtype",
                "int DEFAULT NULL COMMENT '子类型：20=探店计划到店日前第3天提醒'");
    }

    private String fullTableName() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        String base = pre.trim().replace("`", "");
        return base.endsWith("_") ? base + "system_notify" : base + "_system_notify";
    }

    private void createTableIfNotExists() {
        String t = "`" + fullTableName() + "`";
        String sql = "CREATE TABLE IF NOT EXISTS " + t + " ("
                + "`id` int NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "`uid` varchar(32) NULL DEFAULT NULL COMMENT '用户ID',"
                + "`title` varchar(32) NULL DEFAULT NULL COMMENT '标题',"
                + "`note` text NULL COMMENT '正文',"
                + "`ndate` varchar(50) NULL DEFAULT NULL COMMENT '创建时间',"
                + "`type` int NULL DEFAULT NULL COMMENT '0=已读 1=未读',"
                + "`msgtype` int NULL DEFAULT NULL COMMENT '子类型：20=探店计划到店日前第3天提醒',"
                + "PRIMARY KEY (`id`),"
                + "KEY `idx_system_notify_uid` (`uid`),"
                + "KEY `idx_system_notify_uid_type` (`uid`,`type`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci "
                + "COMMENT='用户系统通知（消息-系统）' ROW_FORMAT=DYNAMIC";
        try {
            databaseService.executeAction(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addColumnIfMissing(String columnName, String columnDefTail) {
        String table = "`" + fullTableName() + "`";
        String sql = "ALTER TABLE " + table + " ADD COLUMN `" + columnName + "` " + columnDefTail;
        try {
            databaseService.executeAction(sql);
        } catch (Exception e) {
            if (!isDuplicateColumnError(e)) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isDuplicateColumnError(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String m = cur.getMessage();
            if (m != null) {
                String u = m.toUpperCase();
                if (u.contains("DUPLICATE COLUMN") || u.contains("1060")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}
