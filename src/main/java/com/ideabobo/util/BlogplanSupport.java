package com.ideabobo.util;

import com.ideabobo.constant.BlogplanState;
import com.ideabobo.model.Blogplan;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public final class BlogplanSupport {

    private BlogplanSupport() {
    }

    public static boolean isBlogplanTable(String tableName) {
        return tableName != null && tableName.toLowerCase().endsWith("blogplan");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object st = row.get("state");
        if (st != null) {
            Integer code = BillSupport.toInteger(st);
            if (code != null && code > 0) {
                String label = BlogplanState.labelOf(code);
                row.put("statecn", label);
                row.put("state", label);
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
        if (!isBlogplanTable(tableName) || fieldName == null) {
            return null;
        }
        if ("state".equals(fieldName) || "statecn".equals(fieldName)) {
            return "state";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isBlogplanTable(tableName) || value == null) {
            return value;
        }
        if (!"state".equals(fieldName) && !"statecn".equals(fieldName)) {
            return value;
        }
        if (value instanceof Integer) {
            return value;
        }
        Integer code = BlogplanState.codeOfLabel(value.toString());
        return code != null ? code : value;
    }

    public static void applyListRequest(Blogplan model, HttpServletRequest request) {
        applyFromRequest(model, request);
    }

    public static void applySaveRequest(Blogplan model, HttpServletRequest request) {
        applyFromRequest(model, request);
        if (model.getState() == null) {
            model.setState(BlogplanState.PENDING);
        }
    }

    private static void applyFromRequest(Blogplan model, HttpServletRequest request) {
        if (model == null || request == null) {
            return;
        }
        String state = request.getParameter("state");
        if (state != null && !state.trim().isEmpty()) {
            try {
                model.setState(Integer.parseInt(state.trim()));
            } catch (NumberFormatException e) {
                Integer code = BlogplanState.codeOfLabel(state);
                if (code != null) {
                    model.setState(code);
                }
            }
        }
        String statecn = request.getParameter("statecn");
        if (statecn != null && !statecn.trim().isEmpty()) {
            Integer code = BlogplanState.codeOfLabel(statecn);
            if (code != null) {
                model.setState(code);
            }
        }
    }
}
