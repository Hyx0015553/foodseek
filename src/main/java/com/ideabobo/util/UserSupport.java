package com.ideabobo.util;

import com.ideabobo.constant.UserRole;
import com.ideabobo.model.User;
import com.ideabobo.service.DatabaseService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户表规范化：库内 role 数值、收藏关系表；接口仍带 roletype / favs* 逗号串。
 */
public final class UserSupport {

    public static final int FAV_SHOP = 1;
    public static final int FAV_POST = 2;
    public static final int FAV_GOOD = 3;

    private UserSupport() {
    }

    public static boolean isUserTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        return tableName.toLowerCase().endsWith("user");
    }

    public static void enrichRow(DatabaseService db, Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object role = row.get("role");
        if (role != null) {
            Integer code = BillSupport.toInteger(role);
            if (code != null) {
                row.put("roletype", UserRole.codeAsString(code));
            }
        } else {
            Object legacy = row.get("roletype");
            if (legacy != null) {
                row.put("roletype", legacy.toString().trim());
            }
        }
        Object uidObj = row.get("id");
        if (uidObj == null || db == null) {
            return;
        }
        try {
            int uid = Integer.parseInt(uidObj.toString().trim());
            row.put("favs", joinCsv(loadFavoriteIds(db, uid, FAV_SHOP)));
            row.put("favs2", joinCsv(loadFavoriteIds(db, uid, FAV_POST)));
            row.put("favs3", joinCsv(loadFavoriteIds(db, uid, FAV_GOOD)));
        } catch (NumberFormatException ignored) {
        }
    }

    public static void enrichRows(DatabaseService db, List<Map<String, Object>> rows) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            enrichRow(db, row);
        }
    }

    public static String resolveListFieldName(String tableName, String fieldName) {
        if (!isUserTable(tableName) || fieldName == null) {
            return null;
        }
        if ("roletype".equals(fieldName)) {
            return "role";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isUserTable(tableName) || !"roletype".equals(fieldName) || value == null) {
            return value;
        }
        if (value instanceof Integer) {
            return value;
        }
        Integer code = UserRole.codeOfString(value.toString());
        return code != null ? code : value;
    }

    public static void applyListRequest(User user, HttpServletRequest request) {
        if (user == null || request == null) {
            return;
        }
        mapRoleFromRequest(user, request, false);
    }

    public static void applySaveRequest(User user, HttpServletRequest request) {
        if (user == null || request == null) {
            return;
        }
        mapRoleFromRequest(user, request, true);
    }

    public static void syncFavoritesAfterSave(DatabaseService db, int uid, HttpServletRequest request) {
        if (db == null || request == null || uid <= 0) {
            return;
        }
        if (request.getParameter("favs") != null) {
            replaceFavorites(db, uid, FAV_SHOP, request.getParameter("favs"));
        }
        if (request.getParameter("favs2") != null) {
            replaceFavorites(db, uid, FAV_POST, request.getParameter("favs2"));
        }
        if (request.getParameter("favs3") != null) {
            replaceFavorites(db, uid, FAV_GOOD, request.getParameter("favs3"));
        }
    }

    private static void mapRoleFromRequest(User user, HttpServletRequest request, boolean defaultUser) {
        String roletype = request.getParameter("roletype");
        if (roletype != null && !roletype.trim().isEmpty()) {
            Integer code = UserRole.codeOfString(roletype);
            if (code != null) {
                user.setRole(code);
            }
        }
        String role = request.getParameter("role");
        if (role != null && !role.trim().isEmpty()) {
            try {
                user.setRole(Integer.parseInt(role.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (defaultUser && user.getRole() == null) {
            user.setRole(UserRole.USER);
        }
    }

    public static Set<String> splitCsvIds(String csv) {
        Set<String> out = new LinkedHashSet<>();
        if (csv == null) {
            return out;
        }
        String t = csv.trim();
        if (t.isEmpty() || "0".equals(t)) {
            return out;
        }
        for (String p : t.split(",")) {
            if (p == null) {
                continue;
            }
            String id = p.trim();
            if (!id.isEmpty() && !"0".equals(id)) {
                out.add(id);
            }
        }
        return out;
    }

    public static String joinCsv(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            if (id == null || id.trim().isEmpty() || "0".equals(id.trim())) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id.trim());
        }
        return sb.toString();
    }

    /** 某用户某类收藏的目标 ID 集合（店铺/动态/菜品） */
    public static Set<String> loadFavoriteIdSet(DatabaseService db, int uid, int favType) {
        return new LinkedHashSet<>(loadFavoriteIds(db, uid, favType));
    }

    /**
     * 全部用户的店铺收藏（fav_type=1），用于协同推荐；key=uid，value=店铺 id 集合。
     */
    public static Map<Integer, Set<String>> loadAllUserShopFavoriteMap(DatabaseService db, int excludeUid) {
        Map<Integer, Set<String>> out = new HashMap<>();
        if (db == null) {
            return out;
        }
        String table = favoriteTable();
        List<Map<String, Object>> rows = db.find(
                "SELECT uid, target_id FROM " + table + " WHERE fav_type=" + FAV_SHOP + " ORDER BY uid, id");
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Object uo = row.get("uid");
            Object to = row.get("target_id");
            if (uo == null || to == null) {
                continue;
            }
            try {
                int uid = Integer.parseInt(uo.toString().trim());
                if (excludeUid > 0 && uid == excludeUid) {
                    continue;
                }
                out.computeIfAbsent(uid, k -> new LinkedHashSet<>()).add(to.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private static List<String> loadFavoriteIds(DatabaseService db, int uid, int favType) {
        List<String> ids = new ArrayList<>();
        String table = favoriteTable();
        List<Map<String, Object>> rows = db.find(
                "SELECT target_id FROM " + table + " WHERE uid=" + uid + " AND fav_type=" + favType + " ORDER BY id");
        if (rows == null) {
            return ids;
        }
        for (Map<String, Object> row : rows) {
            Object tid = row.get("target_id");
            if (tid != null) {
                ids.add(tid.toString());
            }
        }
        return ids;
    }

    private static void replaceFavorites(DatabaseService db, int uid, int favType, String csv) {
        String table = favoriteTable();
        db.executeAction("DELETE FROM " + table + " WHERE uid=" + uid + " AND fav_type=" + favType);
        Set<String> ids = splitCsvIds(csv);
        for (String idStr : ids) {
            try {
                int targetId = Integer.parseInt(idStr);
                db.executeAction("INSERT IGNORE INTO " + table + "(uid,fav_type,target_id) VALUES("
                        + uid + "," + favType + "," + targetId + ")");
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static String favoriteTable() {
        String pre = Common.getProperty("tableprefix");
        if (pre == null || pre.trim().isEmpty()) {
            pre = "fs_";
        }
        return pre.trim().replace("`", "") + "user_favorite";
    }

    private static String joinCsv(List<String> ids) {
        Set<String> set = new LinkedHashSet<>();
        if (ids != null) {
            set.addAll(ids);
        }
        return joinCsv(set);
    }
}
