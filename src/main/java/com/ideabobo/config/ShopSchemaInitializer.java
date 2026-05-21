package com.ideabobo.config;

import com.ideabobo.constant.ShopAuditState;
import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 店铺表规范化：审核状态数值化、删除误导字段 sid、类型收紧。
 */
@Component
public class ShopSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void migrateShopSchema() {
        String table = shopTable();
        try {
            addColumnIfMissing(table, "state",
                    "tinyint NOT NULL DEFAULT 1 COMMENT '审核状态码，见 fs_dict.shop_audit'");
            migrateStatecnToState(table);
            modifyColumn(table, "ownid", "int NULL DEFAULT NULL COMMENT '店主用户ID(fs_user.id)'");
            modifyColumn(table, "pf", "decimal(3,1) NULL DEFAULT NULL COMMENT '评分'");
            dropColumnIfExists(table, "sid");
            dropColumnIfExists(table, "statecn");
            runIgnoreError("ALTER TABLE " + table + " ADD INDEX `idx_shop_state` (`state`)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void migrateStatecnToState(String table) {
        if (!columnExists(table, "statecn")) {
            return;
        }
        runIgnoreError("UPDATE " + table + " SET state=" + ShopAuditState.PENDING + " WHERE statecn='待审核'");
        runIgnoreError("UPDATE " + table + " SET state=" + ShopAuditState.APPROVED + " WHERE statecn='审核通过'");
        runIgnoreError("UPDATE " + table + " SET state=" + ShopAuditState.REJECTED + " WHERE statecn='审核不通过'");
        runIgnoreError("UPDATE " + table + " SET state=" + ShopAuditState.PENDING
                + " WHERE (statecn IS NULL OR TRIM(statecn)='') AND (state IS NULL OR state=0)");
    }

    private void addColumnIfMissing(String table, String column, String def) {
        if (columnExists(table, column)) {
            return;
        }
        runIgnoreError("ALTER TABLE " + table + " ADD COLUMN `" + column + "` " + def);
    }

    private void modifyColumn(String table, String column, String def) {
        if (!columnExists(table, column)) {
            return;
        }
        runIgnoreError("ALTER TABLE " + table + " MODIFY COLUMN `" + column + "` " + def);
    }

    private void dropColumnIfExists(String table, String column) {
        if (!columnExists(table, column)) {
            return;
        }
        runIgnoreError("ALTER TABLE " + table + " DROP COLUMN `" + column + "`");
    }

    private boolean columnExists(String table, String column) {
        try {
            String bare = table.replace("`", "");
            List<Map<String, Object>> rows = databaseService.find(
                    "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"
                            + " AND TABLE_NAME='" + bare + "' AND COLUMN_NAME='" + column + "' LIMIT 1");
            return rows != null && !rows.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void runIgnoreError(String sql) {
        try {
            databaseService.executeAction(sql);
        } catch (Exception e) {
            if (!isBenignAlterError(e)) {
                System.err.println("ShopSchemaInitializer: " + sql + " -> " + e.getMessage());
            }
        }
    }

    private static boolean isBenignAlterError(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String m = cur.getMessage();
            if (m != null) {
                String u = m.toUpperCase();
                if (u.contains("DUPLICATE COLUMN") || u.contains("1060")
                        || u.contains("UNKNOWN COLUMN") || u.contains("1054")
                        || u.contains("CAN'T DROP") || u.contains("1091")
                        || u.contains("DUPLICATE KEY") || u.contains("1061")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String shopTable() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        String base = pre.trim().replace("`", "");
        return "`" + (base.endsWith("_") ? base + "shop" : base + "_shop") + "`";
    }
}
