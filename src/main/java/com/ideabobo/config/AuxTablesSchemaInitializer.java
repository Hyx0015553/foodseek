package com.ideabobo.config;

import com.ideabobo.constant.AppealState;
import com.ideabobo.constant.BlogplanState;
import com.ideabobo.constant.BlogVisibility;
import com.ideabobo.constant.QaContentState;
import com.ideabobo.constant.SensitiveCandidateState;
import com.ideabobo.constant.UserRole;
import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 模块6：其余业务表规范化（申诉/问答/探店计划/评价/动态/消息/敏感词等）
 */
@Component
public class AuxTablesSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void migrateAuxTables() {
        String pre = tablePrefix();
        try {
            migrateReplayAppeal(pre);
            migrateSensitiveCandidate(pre);
            migrateShopQa(pre);
            migrateBlogplan(pre);
            migrateReplay(pre);
            migrateBlog(pre);
            migrateSystemNotify(pre);
            migrateDatetimeColumns(pre);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void migrateReplayAppeal(String pre) {
        String t = tbl(pre, "replay_appeal");
        addColumnIfMissing(t, "state", "tinyint NULL DEFAULT NULL COMMENT '申诉状态码 appeal_state'");
        if (columnExists(t, "statecn")) {
            runIgnoreError("UPDATE " + t + " SET state=" + AppealState.PENDING + " WHERE statecn LIKE '%待处理%'");
            runIgnoreError("UPDATE " + t + " SET state=" + AppealState.SUCCESS + " WHERE statecn LIKE '%申诉成功%'");
            runIgnoreError("UPDATE " + t + " SET state=" + AppealState.REJECTED + " WHERE statecn LIKE '%申诉失败%'");
            dropColumnIfExists(t, "statecn");
        }
        modifyColumn(t, "ndate", "datetime NULL DEFAULT NULL COMMENT '申诉时间'");
        runIgnoreError("ALTER TABLE " + t + " ADD INDEX `idx_replay_appeal_state` (`state`)");
    }

    private void migrateSensitiveCandidate(String pre) {
        String t = tbl(pre, "sensitive_ai_candidate");
        addColumnIfMissing(t, "state", "tinyint NOT NULL DEFAULT 1 COMMENT '候选状态 sensitive_candidate_state'");
        if (columnExists(t, "statecn")) {
            runIgnoreError("UPDATE " + t + " SET state=" + SensitiveCandidateState.PENDING + " WHERE statecn LIKE '%待处理%'");
            runIgnoreError("UPDATE " + t + " SET state=" + SensitiveCandidateState.ADOPTED + " WHERE statecn LIKE '%已采纳%'");
            runIgnoreError("UPDATE " + t + " SET state=" + SensitiveCandidateState.IGNORED + " WHERE statecn LIKE '%已忽略%'");
            dropColumnIfExists(t, "statecn");
        }
        modifyColumn(t, "ndate", "datetime NULL DEFAULT NULL COMMENT '记录时间'");
        runIgnoreError("ALTER TABLE " + t + " ADD INDEX `idx_sensitive_candidate_state` (`state`)");
    }

    private void migrateShopQa(String pre) {
        String t = tbl(pre, "shop_qa");
        addColumnIfMissing(t, "state", "tinyint NOT NULL DEFAULT 1 COMMENT '内容状态 qa_content_state'");
        addColumnIfMissing(t, "role", "tinyint NULL DEFAULT NULL COMMENT '发布者角色 user_role'");
        if (columnExists(t, "statecn")) {
            runIgnoreError("UPDATE " + t + " SET state=" + QaContentState.NORMAL + " WHERE TRIM(IFNULL(statecn,'')) IN ('','正常')");
            runIgnoreError("UPDATE " + t + " SET state=" + QaContentState.HIDDEN + " WHERE statecn LIKE '%隐藏%'");
            runIgnoreError("UPDATE " + t + " SET state=" + QaContentState.DELETED + " WHERE statecn LIKE '%删除%'");
            dropColumnIfExists(t, "statecn");
        }
        if (columnExists(t, "roletype")) {
            runIgnoreError("UPDATE " + t + " SET role=1 WHERE roletype='1' OR roletype=1");
            runIgnoreError("UPDATE " + t + " SET role=2 WHERE roletype='2' OR roletype=2");
            runIgnoreError("UPDATE " + t + " SET role=3 WHERE roletype='3' OR roletype=3");
            runIgnoreError("UPDATE " + t + " SET role=5 WHERE roletype='5' OR roletype=5");
            runIgnoreError("UPDATE " + t + " SET role=6 WHERE roletype='6' OR roletype=6");
            dropColumnIfExists(t, "roletype");
        }
        modifyColumn(t, "ndate", "datetime NULL DEFAULT NULL COMMENT '发布时间'");
    }

    private void migrateBlogplan(String pre) {
        String t = tbl(pre, "blogplan");
        if (!columnExists(t, "state")) {
            return;
        }
        String colType = columnSqlType(t, "state");
        if (colType != null && colType.toLowerCase().contains("varchar")) {
            addColumnIfMissing(t, "state_code", "tinyint NULL DEFAULT 1 COMMENT '计划状态 blogplan_state'");
            runIgnoreError("UPDATE " + t + " SET state_code=" + BlogplanState.PENDING + " WHERE state='待探店'");
            runIgnoreError("UPDATE " + t + " SET state_code=" + BlogplanState.COMPLETED + " WHERE state='已完成'");
            runIgnoreError("UPDATE " + t + " SET state_code=" + BlogplanState.CANCELLED + " WHERE state='已取消'");
            dropColumnIfExists(t, "state");
            runIgnoreError("ALTER TABLE " + t + " CHANGE COLUMN `state_code` `state` tinyint NULL DEFAULT 1 COMMENT '计划状态 blogplan_state'");
        }
        modifyColumn(t, "ndate", "datetime NULL DEFAULT NULL COMMENT '创建/更新时间'");
        modifyColumn(t, "plantime", "varchar(20) NULL DEFAULT NULL COMMENT '计划到店时间'");
    }

    private void migrateReplay(String pre) {
        String t = tbl(pre, "replay");
        modifyColumn(t, "pid", "int NULL DEFAULT NULL COMMENT '商品ID'");
        modifyColumn(t, "uid", "int NULL DEFAULT NULL COMMENT '用户ID'");
        modifyColumn(t, "pf", "decimal(3,1) NULL DEFAULT NULL COMMENT '评分'");
        modifyColumn(t, "ndate", "datetime NULL DEFAULT NULL COMMENT '评价时间'");
    }

    private void migrateBlog(String pre) {
        String t = tbl(pre, "blog");
        addColumnIfMissing(t, "visibility", "tinyint NOT NULL DEFAULT 1 COMMENT '可见性 blog_visibility'");
        if (columnExists(t, "vtype")) {
            runIgnoreError("UPDATE " + t + " SET visibility=" + BlogVisibility.PUBLIC + " WHERE TRIM(IFNULL(vtype,''))='公开' OR vtype IS NULL OR TRIM(vtype)=''");
            dropColumnIfExists(t, "vtype");
        }
        modifyColumn(t, "ndate", "datetime NULL DEFAULT NULL COMMENT '发布时间'");
    }

    private void migrateSystemNotify(String pre) {
        String t = tbl(pre, "system_notify");
        modifyColumn(t, "uid", "int NULL DEFAULT NULL COMMENT '用户ID'");
        modifyColumn(t, "ndate", "datetime NULL DEFAULT NULL COMMENT '创建时间'");
    }

    private void migrateDatetimeColumns(String pre) {
        modifyColumn(tbl(pre, "sysmsg"), "ndate", "datetime NULL DEFAULT NULL COMMENT '发送时间'");
        modifyColumn(tbl(pre, "shop_qa_notice"), "ndate", "datetime NULL DEFAULT NULL COMMENT '更新时间'");
        modifyColumn(tbl(pre, "sensitive_hit_log"), "ndate", "datetime NULL DEFAULT NULL COMMENT '记录时间'");
    }

    private String columnSqlType(String table, String column) {
        try {
            String bare = table.replace("`", "");
            List<Map<String, Object>> rows = databaseService.find(
                    "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"
                            + " AND TABLE_NAME='" + bare + "' AND COLUMN_NAME='" + column + "' LIMIT 1");
            if (rows == null || rows.isEmpty() || rows.get(0).get("COLUMN_TYPE") == null) {
                return null;
            }
            return rows.get(0).get("COLUMN_TYPE").toString();
        } catch (Exception e) {
            return null;
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
                System.err.println("AuxTablesSchemaInitializer: " + sql + " -> " + e.getMessage());
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

    private static String tablePrefix() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            return "fs_";
        }
        return pre.trim().replace("`", "");
    }

    private static String tbl(String pre, String name) {
        return "`" + pre + name + "`";
    }
}
