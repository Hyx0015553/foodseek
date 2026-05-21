package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 探店计划状态 fs_dict.blogplan_state */
public final class BlogplanState {

    public static final int PENDING = 1;
    public static final int COMPLETED = 2;
    public static final int CANCELLED = 3;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(PENDING, "待探店");
        c2l.put(COMPLETED, "已完成");
        c2l.put(CANCELLED, "已取消");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);
        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private BlogplanState() {
    }

    public static String labelOf(Integer code) {
        if (code == null || code <= 0) {
            return "";
        }
        String label = CODE_TO_LABEL.get(code);
        return label != null ? label : "";
    }

    public static Integer codeOfLabel(String label) {
        if (label == null) {
            return null;
        }
        String t = label.trim();
        if (t.isEmpty()) {
            return null;
        }
        Integer exact = LABEL_TO_CODE.get(t);
        if (exact != null) {
            return exact;
        }
        if (t.contains("待探店")) {
            return PENDING;
        }
        if (t.contains("已完成") || t.contains("完成")) {
            return COMPLETED;
        }
        if (t.contains("已取消") || t.contains("取消")) {
            return CANCELLED;
        }
        return null;
    }
}
