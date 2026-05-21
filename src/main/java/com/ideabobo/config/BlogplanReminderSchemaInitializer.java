package com.ideabobo.config;

import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 探店计划：到店前第 3 天系统消息标记列 {@code fs_blogplan.remind_3d_sent}。
 */
@Component
public class BlogplanReminderSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void ensureBlogplanReminderColumn() {
        addBlogplanColumnIfMissing(
                "remind_3d_sent",
                "int DEFAULT NULL COMMENT '1=已发送计划到店前第3天系统通知(fs_system_notify)'");
    }

    private void addBlogplanColumnIfMissing(String columnName, String columnDefTail) {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        String base = pre.trim().replace("`", "");
        String full = base.endsWith("_") ? base + "blogplan" : base + "_blogplan";
        String table = "`" + full + "`";
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
