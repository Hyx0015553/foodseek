package com.ideabobo.util;

import com.ideabobo.constant.ShopAuditState;
import com.ideabobo.model.Shop;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 店铺表规范化兼容层：库内 audit_state，接口仍可带 statecn。
 */
public final class ShopSupport {

    private ShopSupport() {
    }

    public static boolean isShopTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String t = tableName.toLowerCase();
        return t.endsWith("shop") && !t.contains("shop_qa") && !t.contains("wordcloud");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object st = row.get("state");
        if (st == null) {
            return;
        }
        Integer code = BillSupport.toInteger(st);
        if (code != null) {
            row.put("statecn", ShopAuditState.labelOf(code));
        }
    }

    public static void enrichRows(List<Map<String, Object>> rows) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            enrichRow(row);
        }
    }

    public static String resolveListFieldName(String tableName, String fieldName) {
        if (!isShopTable(tableName) || fieldName == null) {
            return null;
        }
        if ("statecn".equals(fieldName)) {
            return "state";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isShopTable(tableName) || !"statecn".equals(fieldName) || value == null) {
            return value;
        }
        if (value instanceof Integer) {
            return value;
        }
        Integer code = ShopAuditState.codeOfLabel(value.toString());
        return code != null ? code : value;
    }

    /** list/find 查询：仅把 statecn / state 写入 model，不设默认值 */
    public static void applyListRequest(Shop shop, HttpServletRequest request) {
        if (shop == null || request == null) {
            return;
        }
        mapStateFromRequest(shop, request, false);
    }

    public static void applySaveRequest(Shop shop, HttpServletRequest request) {
        if (shop == null || request == null) {
            return;
        }
        mapStateFromRequest(shop, request, true);
    }

    private static void mapStateFromRequest(Shop shop, HttpServletRequest request, boolean defaultPending) {
        String statecn = request.getParameter("statecn");
        if (statecn != null && !statecn.trim().isEmpty()) {
            Integer code = ShopAuditState.codeOfLabel(statecn);
            if (code != null) {
                shop.setState(code);
            }
        }
        String state = request.getParameter("state");
        if (state != null && !state.trim().isEmpty()) {
            try {
                shop.setState(Integer.parseInt(state.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (defaultPending && shop.getState() == null) {
            shop.setState(ShopAuditState.PENDING);
        }
    }
}
