package com.ideabobo.util;

import com.ideabobo.constant.GoodState;
import com.ideabobo.model.Good;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 商品表规范化兼容层：库内 state/btype 数值，接口仍可带 statecn。
 */
public final class GoodSupport {

    private GoodSupport() {
    }

    public static boolean isGoodTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String t = tableName.toLowerCase();
        return t.endsWith("good") && !t.contains("wordcloud");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object st = row.get("state");
        if (st != null) {
            Integer code = BillSupport.toInteger(st);
            if (code != null) {
                row.put("statecn", GoodState.labelOf(code));
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
        if (!isGoodTable(tableName) || fieldName == null) {
            return null;
        }
        if ("statecn".equals(fieldName)) {
            return "state";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isGoodTable(tableName) || !"statecn".equals(fieldName) || value == null) {
            return value;
        }
        if (value instanceof Integer) {
            return value;
        }
        Integer code = GoodState.codeOfLabel(value.toString());
        return code != null ? code : value;
    }

    public static void applyListRequest(Good good, HttpServletRequest request) {
        if (good == null || request == null) {
            return;
        }
        mapFromRequest(good, request, false);
    }

    public static void applySaveRequest(Good good, HttpServletRequest request) {
        if (good == null || request == null) {
            return;
        }
        mapFromRequest(good, request, true);
    }

    private static void mapFromRequest(Good good, HttpServletRequest request, boolean defaultOnShelf) {
        String statecn = request.getParameter("statecn");
        if (statecn != null && !statecn.trim().isEmpty()) {
            Integer code = GoodState.codeOfLabel(statecn);
            if (code != null) {
                good.setState(code);
            }
        }
        String state = request.getParameter("state");
        if (state != null && !state.trim().isEmpty()) {
            try {
                good.setState(Integer.parseInt(state.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        String btype = request.getParameter("btype");
        if (btype != null && !btype.trim().isEmpty() && good.getBtype() == null) {
            try {
                good.setBtype(Integer.parseInt(btype.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (defaultOnShelf && good.getState() == null) {
            good.setState(GoodState.ON_SHELF);
        }
        if (good.getBtype() == null) {
            good.setBtype(1);
        }
    }
}
