package com.ideabobo.util;

import com.ideabobo.constant.AppealState;
import com.ideabobo.model.ReplayAppeal;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public final class ReplayAppealSupport {

    private ReplayAppealSupport() {
    }

    public static boolean isAppealTable(String tableName) {
        return tableName != null && tableName.toLowerCase().endsWith("replay_appeal");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object st = row.get("state");
        if (st != null) {
            Integer code = BillSupport.toInteger(st);
            if (code != null && code > 0) {
                String label = AppealState.labelOf(code);
                row.put("statecn", label);
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
        if (!isAppealTable(tableName) || fieldName == null) {
            return null;
        }
        if ("statecn".equals(fieldName)) {
            return "state";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isAppealTable(tableName) || !"statecn".equals(fieldName) || value == null) {
            return value;
        }
        if (value instanceof Integer) {
            return value;
        }
        Integer code = AppealState.codeOfLabel(value.toString());
        return code != null ? code : value;
    }

    public static void applyListRequest(ReplayAppeal model, HttpServletRequest request) {
        applyFromRequest(model, request);
    }

    public static void applySaveRequest(ReplayAppeal model, HttpServletRequest request) {
        applyFromRequest(model, request);
    }

    private static void applyFromRequest(ReplayAppeal model, HttpServletRequest request) {
        if (model == null || request == null) {
            return;
        }
        String statecn = request.getParameter("statecn");
        if (statecn != null && !statecn.trim().isEmpty()) {
            Integer code = AppealState.codeOfLabel(statecn);
            if (code != null) {
                model.setState(code);
            }
        }
        String state = request.getParameter("state");
        if (state != null && !state.trim().isEmpty()) {
            try {
                model.setState(Integer.parseInt(state.trim()));
            } catch (NumberFormatException ignored) {
                Integer code = AppealState.codeOfLabel(state);
                if (code != null) {
                    model.setState(code);
                }
            }
        }
    }
}
