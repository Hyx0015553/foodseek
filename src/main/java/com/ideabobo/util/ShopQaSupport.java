package com.ideabobo.util;

import com.ideabobo.constant.QaContentState;
import com.ideabobo.constant.UserRole;
import com.ideabobo.model.ShopQa;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public final class ShopQaSupport {

    private ShopQaSupport() {
    }

    public static boolean isShopQaTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String t = tableName.toLowerCase();
        return t.endsWith("shop_qa") && !t.contains("notice");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object st = row.get("state");
        if (st != null) {
            Integer code = BillSupport.toInteger(st);
            if (code != null && code > 0) {
                row.put("statecn", QaContentState.labelOf(code));
            }
        }
        Object role = row.get("role");
        if (role != null) {
            Integer code = BillSupport.toInteger(role);
            if (code != null && code > 0) {
                row.put("roletype", UserRole.codeAsString(code));
            }
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
        if (!isShopQaTable(tableName) || fieldName == null) {
            return null;
        }
        if ("statecn".equals(fieldName)) {
            return "state";
        }
        if ("roletype".equals(fieldName)) {
            return "role";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isShopQaTable(tableName) || value == null) {
            return value;
        }
        if ("statecn".equals(fieldName)) {
            if (value instanceof Integer) {
                return value;
            }
            Integer code = QaContentState.codeOfLabel(value.toString());
            return code != null ? code : value;
        }
        if ("roletype".equals(fieldName)) {
            if (value instanceof Integer) {
                return value;
            }
            Integer code = UserRole.codeOfString(value.toString());
            return code != null ? code : value;
        }
        return value;
    }

    public static void applyListRequest(ShopQa model, HttpServletRequest request) {
        applyFromRequest(model, request);
    }

    public static void applySaveRequest(ShopQa model, HttpServletRequest request) {
        applyFromRequest(model, request);
        if (model.getState() == null) {
            model.setState(QaContentState.NORMAL);
        }
    }

    private static void applyFromRequest(ShopQa model, HttpServletRequest request) {
        if (model == null || request == null) {
            return;
        }
        String statecn = request.getParameter("statecn");
        if (statecn != null) {
            Integer code = QaContentState.codeOfLabel(statecn);
            if (code != null) {
                model.setState(code);
            }
        }
        String state = request.getParameter("state");
        if (state != null && !state.trim().isEmpty()) {
            try {
                model.setState(Integer.parseInt(state.trim()));
            } catch (NumberFormatException e) {
                Integer code = QaContentState.codeOfLabel(state);
                if (code != null) {
                    model.setState(code);
                }
            }
        }
        String roletype = request.getParameter("roletype");
        if (roletype != null && !roletype.trim().isEmpty()) {
            Integer code = UserRole.codeOfString(roletype);
            if (code != null) {
                model.setRole(code);
            }
        }
        String role = request.getParameter("role");
        if (role != null && !role.trim().isEmpty()) {
            try {
                model.setRole(Integer.parseInt(role.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
