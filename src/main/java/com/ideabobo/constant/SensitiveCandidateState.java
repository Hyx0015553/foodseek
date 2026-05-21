package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** AI 敏感候选词状态 fs_dict.sensitive_candidate_state */
public final class SensitiveCandidateState {

    public static final int PENDING = 1;
    public static final int ADOPTED = 2;
    public static final int IGNORED = 3;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(PENDING, "待处理");
        c2l.put(ADOPTED, "已采纳");
        c2l.put(IGNORED, "已忽略");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);
        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private SensitiveCandidateState() {
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
        if (t.contains("已采纳") || t.contains("采纳")) {
            return ADOPTED;
        }
        if (t.contains("已忽略") || t.contains("忽略")) {
            return IGNORED;
        }
        return null;
    }
}
