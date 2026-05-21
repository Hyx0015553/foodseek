package com.ideabobo.config;

import com.ideabobo.constant.UserRole;
import com.ideabobo.service.DatabaseService;
import com.ideabobo.util.Common;
import com.ideabobo.util.UserSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户表规范化：role 数值化、sid 整型、收藏拆表、删除废弃 statecn 与逗号收藏列。
 */
@Component
public class UserSchemaInitializer {

    @Autowired
    private DatabaseService databaseService;

    @PostConstruct
    public void migrateUserSchema() {
        try {
            ensureFavoriteTable();
            migrateUserColumns();
            migrateCsvFavoritesToTable();
            dropLegacyUserColumns();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ensureFavoriteTable() {
        String t = "`" + UserSupport.favoriteTable() + "`";
        String ddl = "CREATE TABLE IF NOT EXISTS " + t + " ("
                + "id int NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "uid int NOT NULL COMMENT '用户ID',"
                + "fav_type tinyint NOT NULL COMMENT '1店铺2动态3菜品',"
                + "target_id int NOT NULL COMMENT '目标ID',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_user_fav (uid, fav_type, target_id),"
                + "KEY idx_user_fav_uid (uid)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏关系'";
        runIgnoreError(ddl);
    }

    private void migrateUserColumns() {
        String table = userTable();
        addColumnIfMissing(table, "role",
                "tinyint NOT NULL DEFAULT 2 COMMENT '角色码，见 fs_dict.user_role'");
        if (columnExists(table, "roletype")) {
            runIgnoreError("UPDATE " + table + " SET role=" + UserRole.SUPER_ADMIN
                    + " WHERE roletype='1' OR roletype=1");
            runIgnoreError("UPDATE " + table + " SET role=" + UserRole.USER
                    + " WHERE roletype='2' OR roletype=2");
            runIgnoreError("UPDATE " + table + " SET role=" + UserRole.MERCHANT
                    + " WHERE roletype='3' OR roletype=3");
            runIgnoreError("UPDATE " + table + " SET role=" + UserRole.OPERATOR
                    + " WHERE roletype='5' OR roletype=5");
            runIgnoreError("UPDATE " + table + " SET role=" + UserRole.AUDITOR
                    + " WHERE roletype='6' OR roletype=6");
            runIgnoreError("UPDATE " + table + " SET role=" + UserRole.USER
                    + " WHERE role IS NULL OR role=0");
        }
        modifyColumn(table, "sid", "int NULL DEFAULT NULL COMMENT '商家店铺ID(fs_shop.id)'");
        modifyColumn(table, "tel", "varchar(20) NULL DEFAULT NULL COMMENT '手机号'");
        runIgnoreError("ALTER TABLE " + table + " ADD INDEX `idx_user_role` (`role`)");
    }

    private void migrateCsvFavoritesToTable() {
        if (!columnExists(userTable(), "favs")) {
            return;
        }
        String ut = userTable();
        String ft = "`" + UserSupport.favoriteTable() + "`";
        List<Map<String, Object>> users = databaseService.find(
                "SELECT id,favs,favs2,favs3 FROM " + ut + " WHERE id IS NOT NULL");
        if (users == null) {
            return;
        }
        for (Map<String, Object> u : users) {
            int uid;
            try {
                uid = Integer.parseInt(u.get("id").toString().trim());
            } catch (Exception e) {
                continue;
            }
            importCsv(uid, UserSupport.FAV_SHOP, u.get("favs"), ft);
            importCsv(uid, UserSupport.FAV_POST, u.get("favs2"), ft);
            importCsv(uid, UserSupport.FAV_GOOD, u.get("favs3"), ft);
        }
    }

    private void importCsv(int uid, int favType, Object csvObj, String favTable) {
        if (csvObj == null) {
            return;
        }
        Set<String> ids = UserSupport.splitCsvIds(csvObj.toString());
        for (String idStr : ids) {
            try {
                int targetId = Integer.parseInt(idStr);
                runIgnoreError("INSERT IGNORE INTO " + favTable + "(uid,fav_type,target_id) VALUES("
                        + uid + "," + favType + "," + targetId + ")");
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void dropLegacyUserColumns() {
        String table = userTable();
        dropColumnIfExists(table, "statecn");
        dropColumnIfExists(table, "roletype");
        dropColumnIfExists(table, "favs");
        dropColumnIfExists(table, "favs2");
        dropColumnIfExists(table, "favs3");
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
                System.err.println("UserSchemaInitializer: " + sql + " -> " + e.getMessage());
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

    private static String userTable() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        return "`" + pre.trim().replace("`", "") + "user`";
    }
}
