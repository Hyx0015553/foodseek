package com.ideabobo.config;

import com.ideabobo.constant.GoodState;
import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 商品表规范化：上架状态数值化、外键与金额类型收紧、btype 明确为种类(1单品2组合)。
 */
@Component
public class GoodSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void migrateGoodSchema() {
        String table = goodTable();
        try {
            addColumnIfMissing(table, "state",
                    "tinyint NOT NULL DEFAULT 1 COMMENT '上架状态码，见 fs_dict.good_state'");
            migrateStatecnToState(table);
            modifyColumn(table, "btype", "tinyint NOT NULL DEFAULT 1 COMMENT '商品种类：1单品 2组合'");
            modifyColumn(table, "price", "decimal(10,2) NULL DEFAULT NULL COMMENT '价格(元)'");
            modifyColumn(table, "sid", "int NULL DEFAULT NULL COMMENT '店铺ID(fs_shop.id)'");
            modifyColumn(table, "typeid", "int NULL DEFAULT NULL COMMENT '大类ID(fs_type.id)'");
            modifyColumn(table, "pf", "decimal(3,1) NULL DEFAULT NULL COMMENT '评分'");
            dropColumnIfExists(table, "statecn");
            runIgnoreError("ALTER TABLE " + table + " ADD INDEX `idx_good_state` (`state`)");
            runIgnoreError("ALTER TABLE " + table + " ADD INDEX `idx_good_btype` (`btype`)");
        } catch (Exception e) {
            e.printStackTrace();
        }
        migrateTypeTables();
    }

    private void migrateTypeTables() {
        String pre = tablePrefix();
        String typeT = "`" + pre + "type`";
        String type2T = "`" + pre + "type2`";
        modifyColumn(typeT, "ownid", "int NULL DEFAULT NULL COMMENT '所属店铺ID(可选)'");
        modifyColumn(type2T, "pid", "int NULL DEFAULT NULL COMMENT '父类型ID(fs_type.id)'");
    }

    private void migrateStatecnToState(String table) {
        if (!columnExists(table, "statecn")) {
            return;
        }
        runIgnoreError("UPDATE " + table + " SET state=" + GoodState.ON_SHELF + " WHERE statecn='上架中'");
        runIgnoreError("UPDATE " + table + " SET state=" + GoodState.OFF_SHELF + " WHERE statecn='已下架'");
        runIgnoreError("UPDATE " + table + " SET state=" + GoodState.ON_SHELF
                + " WHERE (statecn IS NULL OR TRIM(statecn)='') AND (state IS NULL OR state=0)");
        runIgnoreError("UPDATE " + table + " SET btype=1 WHERE btype IS NULL OR TRIM(CAST(btype AS CHAR))=''");
        runIgnoreError("UPDATE " + table + " SET btype=1 WHERE CAST(btype AS CHAR)='1' OR btype=1");
        runIgnoreError("UPDATE " + table + " SET btype=2 WHERE CAST(btype AS CHAR)='2' OR btype=2");
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
                System.err.println("GoodSchemaInitializer: " + sql + " -> " + e.getMessage());
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

    private static String goodTable() {
        return "`" + tablePrefix() + "good`";
    }

    private static String tablePrefix() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        return pre.trim().replace("`", "");
    }
}
