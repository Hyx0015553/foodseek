package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 评价申诉状态 fs_dict.appeal_state */
public final class AppealState {

    public static final int PENDING = 1;
    public static final int SUCCESS = 2;
    public static final int REJECTED = 3;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(PENDING, "待处理");
        c2l.put(SUCCESS, "申诉成功");
        c2l.put(REJECTED, "申诉失败");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);
        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private AppealState() {
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
        if (t.contains("待处理")) {
            return PENDING;
        }
        if (t.contains("申诉成功") || t.contains("成功")) {
            return SUCCESS;
        }
        if (t.contains("申诉失败") || t.contains("失败") || t.contains("驳回")) {
            return REJECTED;
        }
        return null;
    }
}
