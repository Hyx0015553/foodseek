package com.ideabobo.util;

import com.ideabobo.model.Replay;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/** 评价表：pid/uid 数值化后接口仍返回字符串形态以兼容旧前端 */
public final class ReplaySupport {

    private ReplaySupport() {
    }

    public static boolean isReplayTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String t = tableName.toLowerCase();
        return t.endsWith("replay") && !t.contains("appeal");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object pid = row.get("pid");
        if (pid != null) {
            row.put("pid", String.valueOf(pid));
        }
        Object uid = row.get("uid");
        if (uid != null) {
            row.put("uid", String.valueOf(uid));
        }
        Object pf = row.get("pf");
        if (pf != null) {
            row.put("pf", String.valueOf(pf));
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

    public static void applyListRequest(Replay replay, HttpServletRequest request) {
        applyFromRequest(replay, request);
    }

    public static void applySaveRequest(Replay replay, HttpServletRequest request) {
        applyFromRequest(replay, request);
    }

    private static void applyFromRequest(Replay replay, HttpServletRequest request) {
        if (replay == null || request == null) {
            return;
        }
        String pid = request.getParameter("pid");
        if (pid != null && !pid.trim().isEmpty()) {
            try {
                replay.setPid(Integer.parseInt(pid.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        String uid = request.getParameter("uid");
        if (uid != null && !uid.trim().isEmpty()) {
            try {
                replay.setUid(Integer.parseInt(uid.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        String pf = request.getParameter("pf");
        if (pf != null && !pf.trim().isEmpty()) {
            replay.setPf(pf.trim());
        }
    }
}
