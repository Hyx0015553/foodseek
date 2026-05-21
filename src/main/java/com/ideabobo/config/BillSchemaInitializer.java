package com.ideabobo.config;

import com.ideabobo.constant.BillState;
import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 订单表结构规范化：state 数值状态、uid/sid/total/ndate 类型、merchantmsghide 标志位；迁移后删除 statecn。
 */
@Component
public class BillSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void migrateBillSchema() {
        String table = billTable();
        try {
            addColumnIfMissing(table, "state",
                    "tinyint NOT NULL DEFAULT 1 COMMENT '订单状态码，见 fs_dict.bill_state'");
            migrateStatecnToState(table);
            modifyColumn(table, "uid", "int NULL DEFAULT NULL COMMENT '用户ID'");
            modifyColumn(table, "sid", "int NULL DEFAULT NULL COMMENT '店铺ID'");
            modifyColumn(table, "total", "decimal(10,2) NULL DEFAULT NULL COMMENT '订单总额(元)'");
            modifyColumn(table, "ndate", "datetime NULL DEFAULT NULL COMMENT '下单时间'");
            modifyColumn(table, "pf", "decimal(3,1) NULL DEFAULT NULL COMMENT '评分'");
            modifyColumn(table, "merchantmsghide",
                    "tinyint NOT NULL DEFAULT 0 COMMENT '1=商家消息页隐藏该单提醒'");
            dropColumnIfExists(table, "statecn");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void migrateStatecnToState(String table) {
        if (!columnExists(table, "statecn")) {
            return;
        }
        runIgnoreError("UPDATE " + table + " SET state=" + BillState.PENDING_PAY + " WHERE statecn='待付款'");
        runIgnoreError("UPDATE " + table + " SET state=" + BillState.PAID + " WHERE statecn='已付款'");
        runIgnoreError("UPDATE " + table + " SET state=" + BillState.SHIPPED + " WHERE statecn='已发货'");
        runIgnoreError("UPDATE " + table + " SET state=" + BillState.COMPLETED + " WHERE statecn='已完成'");
        runIgnoreError("UPDATE " + table + " SET state=" + BillState.REVIEWED + " WHERE statecn='已评价'");
        runIgnoreError("UPDATE " + table + " SET state=" + BillState.CANCELLED + " WHERE statecn='已取消'");
        runIgnoreError("UPDATE " + table + " SET state=" + BillState.COMPLETED
                + " WHERE (statecn IS NULL OR TRIM(statecn)='') AND state IS NULL");
        runIgnoreError("UPDATE " + table + " SET merchantmsghide=1 WHERE merchantmsghide='1'");
        runIgnoreError("UPDATE " + table + " SET merchantmsghide=0 WHERE merchantmsghide IS NULL OR merchantmsghide=''");
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
                System.err.println("BillSchemaInitializer: " + sql + " -> " + e.getMessage());
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
                        || u.contains("CAN'T DROP") || u.contains("1091")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String billTable() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        String base = pre.trim().replace("`", "");
        return "`" + (base.endsWith("_") ? base + "bill" : base + "_bill") + "`";
    }
}
