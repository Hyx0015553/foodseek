package com.ideabobo.config;

import com.ideabobo.constant.CouponState;
import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import com.ideabobo.util.YouhuiquanSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 优惠券表：状态/删除标志数值化，去掉 extimestr 冗余，库存改 int，创建时间改 datetime。
 */
@Component
public class YouhuiquanSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void migrateCouponSchema() {
        String table = couponTable();
        try {
            addColumnIfMissing(table, "state",
                    "tinyint NULL DEFAULT NULL COMMENT '券状态码，见 fs_dict.coupon_state'");
            addColumnIfMissing(table, "deleted",
                    "tinyint NOT NULL DEFAULT 0 COMMENT '0上架1已删除(模板软删)'");
            migrateStatecnToState(table);
            migrateDeletestate(table);
            syncExtimeFromStr(table);
            modifyColumn(table, "kucun", "int NULL DEFAULT NULL COMMENT '库存(模板)'");
            modifyColumn(table, "ndate", "datetime NULL DEFAULT NULL COMMENT '创建/领取时间'");
            dropColumnIfExists(table, "statecn");
            dropColumnIfExists(table, "extimestr");
            dropColumnIfExists(table, "deletestate");
            runIgnoreError("ALTER TABLE " + table + " ADD INDEX `idx_coupon_state` (`state`)");
            runIgnoreError("ALTER TABLE " + table + " ADD INDEX `idx_coupon_typeid` (`typeid`)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void migrateStatecnToState(String table) {
        if (!columnExists(table, "statecn")) {
            return;
        }
        runIgnoreError("UPDATE " + table + " SET state=" + CouponState.NORMAL + " WHERE statecn='正常'");
        runIgnoreError("UPDATE " + table + " SET state=" + CouponState.USED + " WHERE statecn='已使用'");
    }

    private void migrateDeletestate(String table) {
        if (!columnExists(table, "deletestate")) {
            return;
        }
        runIgnoreError("UPDATE " + table + " SET deleted=1 WHERE TRIM(deletestate)='已删除'");
        runIgnoreError("UPDATE " + table + " SET deleted=0 WHERE deletestate IS NULL OR TRIM(deletestate)=''");
    }

    private void syncExtimeFromStr(String table) {
        if (!columnExists(table, "extimestr")) {
            return;
        }
        List<Map<String, Object>> rows = databaseService.find(
                "SELECT id, extime, extimestr FROM " + table + " WHERE extimestr IS NOT NULL AND TRIM(extimestr)<>''");
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            Object ext = row.get("extime");
            if (id == null) {
                continue;
            }
            if (ext != null) {
                try {
                    if (Long.parseLong(ext.toString().trim()) > 0) {
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            Integer parsed = YouhuiquanSupport.parseExtimeFromStr(
                    row.get("extimestr") == null ? null : row.get("extimestr").toString());
            if (parsed != null) {
                runIgnoreError("UPDATE " + table + " SET extime=" + parsed + " WHERE id=" + id);
            }
        }
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
                System.err.println("YouhuiquanSchemaInitializer: " + sql + " -> " + e.getMessage());
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

    private static String couponTable() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        return "`" + pre.trim().replace("`", "") + "youhuiquan`";
    }
}
