package com.ideabobo.util;

import com.ideabobo.constant.SensitiveCandidateState;

import java.util.List;
import java.util.Map;

public final class SensitiveCandidateSupport {

    private SensitiveCandidateSupport() {
    }

    public static boolean isCandidateTable(String tableName) {
        return tableName != null && tableName.toLowerCase().contains("sensitive_ai_candidate");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object st = row.get("state");
        if (st != null) {
            Integer code = BillSupport.toInteger(st);
            if (code != null && code > 0) {
                row.put("statecn", SensitiveCandidateState.labelOf(code));
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

    public static int stateCodeForAdopt() {
        return SensitiveCandidateState.ADOPTED;
    }

    public static int stateCodeForIgnore() {
        return SensitiveCandidateState.IGNORED;
    }

    public static int stateCodeForPending() {
        return SensitiveCandidateState.PENDING;
    }
}
