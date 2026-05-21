package com.ideabobo.config;

import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 数据字典表 fs_dict（状态、类型等枚举统一维护）
 */
@Component
public class DictSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void ensureDictTable() {
        String pre = tablePrefix();
        String table = "`" + pre + "dict`";
        String ddl = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "id int NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "dict_group varchar(32) NOT NULL COMMENT '分组：bill_state/bill_way/shop_audit等',"
                + "dict_code int NOT NULL COMMENT '枚举码',"
                + "dict_label varchar(32) NOT NULL COMMENT '显示名',"
                + "sort_no int NULL DEFAULT 0 COMMENT '排序',"
                + "enabled tinyint NOT NULL DEFAULT 1 COMMENT '1启用0禁用',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_dict_group_code (dict_group, dict_code)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典'";
        try {
            databaseService.executeAction(ddl);
            seedBillState(pre);
            seedShopAudit(pre);
            seedGoodState(pre);
            seedUserRole(pre);
            seedCouponState(pre);
            seedAuxStates(pre);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seedShopAudit(String pre) {
        String t = "`" + pre + "dict`";
        Object[][] rows = {
                {"shop_audit", 1, "待审核", 1},
                {"shop_audit", 2, "审核通过", 2},
                {"shop_audit", 3, "审核不通过", 3}
        };
        for (Object[] r : rows) {
            String sql = "INSERT IGNORE INTO " + t
                    + "(dict_group,dict_code,dict_label,sort_no,enabled) VALUES('"
                    + r[0] + "'," + r[1] + ",'" + r[2] + "'," + r[3] + ",1)";
            try {
                databaseService.executeAction(sql);
            } catch (Exception ignored) {
            }
        }
    }

    private void seedGoodState(String pre) {
        String t = "`" + pre + "dict`";
        Object[][] rows = {
                {"good_state", 1, "上架中", 1},
                {"good_state", 2, "已下架", 2}
        };
        for (Object[] r : rows) {
            String sql = "INSERT IGNORE INTO " + t
                    + "(dict_group,dict_code,dict_label,sort_no,enabled) VALUES('"
                    + r[0] + "'," + r[1] + ",'" + r[2] + "'," + r[3] + ",1)";
            try {
                databaseService.executeAction(sql);
            } catch (Exception ignored) {
            }
        }
    }

    private void seedCouponState(String pre) {
        String t = "`" + pre + "dict`";
        Object[][] rows = {
                {"coupon_state", 1, "正常", 1},
                {"coupon_state", 2, "已使用", 2},
                {"coupon_kind", 1, "券模板", 1},
                {"coupon_kind", 2, "用户持有券", 2}
        };
        for (Object[] r : rows) {
            String sql = "INSERT IGNORE INTO " + t
                    + "(dict_group,dict_code,dict_label,sort_no,enabled) VALUES('"
                    + r[0] + "'," + r[1] + ",'" + r[2] + "'," + r[3] + ",1)";
            try {
                databaseService.executeAction(sql);
            } catch (Exception ignored) {
            }
        }
    }

    private void seedAuxStates(String pre) {
        String t = "`" + pre + "dict`";
        Object[][] rows = {
                {"appeal_state", 1, "待处理", 1},
                {"appeal_state", 2, "申诉成功", 2},
                {"appeal_state", 3, "申诉失败", 3},
                {"sensitive_candidate_state", 1, "待处理", 1},
                {"sensitive_candidate_state", 2, "已采纳", 2},
                {"sensitive_candidate_state", 3, "已忽略", 3},
                {"qa_content_state", 1, "正常", 1},
                {"qa_content_state", 2, "隐藏", 2},
                {"qa_content_state", 3, "删除", 3},
                {"blogplan_state", 1, "待探店", 1},
                {"blogplan_state", 2, "已完成", 2},
                {"blogplan_state", 3, "已取消", 3},
                {"blog_visibility", 1, "公开", 1}
        };
        for (Object[] r : rows) {
            String sql = "INSERT IGNORE INTO " + t
                    + "(dict_group,dict_code,dict_label,sort_no,enabled) VALUES('"
                    + r[0] + "'," + r[1] + ",'" + r[2] + "'," + r[3] + ",1)";
            try {
                databaseService.executeAction(sql);
            } catch (Exception ignored) {
            }
        }
    }

    private void seedUserRole(String pre) {
        String t = "`" + pre + "dict`";
        Object[][] rows = {
                {"user_role", 1, "超级管理员", 1},
                {"user_role", 2, "用户", 2},
                {"user_role", 3, "商家", 3},
                {"user_role", 5, "运营管理员", 5},
                {"user_role", 6, "审核管理员", 6}
        };
        for (Object[] r : rows) {
            String sql = "INSERT IGNORE INTO " + t
                    + "(dict_group,dict_code,dict_label,sort_no,enabled) VALUES('"
                    + r[0] + "'," + r[1] + ",'" + r[2] + "'," + r[3] + ",1)";
            try {
                databaseService.executeAction(sql);
            } catch (Exception ignored) {
            }
        }
    }

    private void seedBillState(String pre) {
        String t = "`" + pre + "dict`";
        Object[][] rows = {
                {"bill_state", 1, "待付款", 1},
                {"bill_state", 2, "已付款", 2},
                {"bill_state", 3, "已发货", 3},
                {"bill_state", 4, "已完成", 4},
                {"bill_state", 5, "已评价", 5},
                {"bill_state", 6, "已取消", 6},
                {"bill_way", 1, "堂食", 1},
                {"bill_way", 2, "配送", 2},
                {"bill_way", 3, "外带", 3},
                {"bill_way", 4, "到店", 4}
        };
        for (Object[] r : rows) {
            String sql = "INSERT IGNORE INTO " + t
                    + "(dict_group,dict_code,dict_label,sort_no,enabled) VALUES('"
                    + r[0] + "'," + r[1] + ",'" + r[2] + "'," + r[3] + ",1)";
            try {
                databaseService.executeAction(sql);
            } catch (Exception ignored) {
            }
        }
    }

    private static String tablePrefix() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            return "fs_";
        }
        return pre.trim().replace("`", "");
    }
}
