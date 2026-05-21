package com.ideabobo.config;

import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 商家「评价管理」列表移除：{@code fs_replay.merchant_delete}，1 表示不在商家列表展示（非物理删除）。
 */
@Component
public class ReplayMerchantDeleteSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void ensureReplayMerchantDeleteColumn() {
        addReplayColumnIfMissing(
                "merchant_delete",
                "int DEFAULT NULL COMMENT '0/null=展示；1=商家评价管理列表移除（非物理删除）'");
    }

    private void addReplayColumnIfMissing(String columnName, String columnDefTail) {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        String base = pre.trim().replace("`", "");
        String full = base.endsWith("_") ? base + "replay" : base + "_replay";
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
