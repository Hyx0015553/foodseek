package com.ideabobo.util;

import com.ideabobo.constant.BillState;
import com.ideabobo.model.Bill;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单表规范化后的兼容层：库内用 state/数值外键，接口仍可带 statecn 供旧前端使用。
 */
public final class BillSupport {

    private BillSupport() {
    }

    public static boolean isBillTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String t = tableName.toLowerCase();
        return t.endsWith("bill") || "bill".equals(t);
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object st = row.get("state");
        if (st == null) {
            return;
        }
        Integer code = toInteger(st);
        if (code != null) {
            row.put("statecn", BillState.labelOf(code));
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

    /**
     * list 查询条件：前端传 statecn 时转为 state 等值查询。
     *
     * @return 替换后的字段名，若无需替换返回 null
     */
    public static String resolveListFieldName(String tableName, String fieldName) {
        if (!isBillTable(tableName) || fieldName == null) {
            return null;
        }
        if ("statecn".equals(fieldName)) {
            return "state";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isBillTable(tableName) || !"statecn".equals(fieldName) || value == null) {
            return value;
        }
        if (value instanceof Integer) {
            return value;
        }
        Integer code = BillState.codeOfLabel(value.toString());
        return code != null ? code : value;
    }

    /** save 前：请求里的 statecn / state 写入 Bill.state */
    public static void applySaveRequest(Bill bill, HttpServletRequest request) {
        if (bill == null || request == null) {
            return;
        }
        String statecn = request.getParameter("statecn");
        if (statecn != null && !statecn.trim().isEmpty()) {
            Integer code = BillState.codeOfLabel(statecn);
            if (code != null) {
                bill.setState(code);
            }
        }
        String state = request.getParameter("state");
        if (state != null && !state.trim().isEmpty()) {
            try {
                bill.setState(Integer.parseInt(state.trim()));
            } catch (NumberFormatException ignored) {
                // keep statecn mapping
            }
        }
    }

    public static Integer toInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static BigDecimal toDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        try {
            String t = raw.toString().trim();
            if (t.isEmpty()) {
                return null;
            }
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }
}
