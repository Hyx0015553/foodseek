package com.ideabobo.config;

import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 消息与订单列表：{@code fs_bill.merchantmsghide}、{@code fs_bill.user_delete}（我的订单列表移除）；旧库启动时自动补齐。
 */
@Component
public class BillMessageSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void ensureBillMessageColumns() {
        addBillColumnIfMissing(
                "merchantmsghide",
                "varchar(10) NULL DEFAULT NULL COMMENT '1=商家消息通知页不再展示该单订单提醒'");
        addBillColumnIfMissing(
                "user_delete",
                "int DEFAULT NULL COMMENT '0/null=我的订单展示；1=用户从订单列表移除（非删单）'");
    }

    private void addBillColumnIfMissing(String columnName, String columnDefTail) {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        String base = pre.trim().replace("`", "");
        String full = base.endsWith("_") ? base + "bill" : base + "_bill";
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
