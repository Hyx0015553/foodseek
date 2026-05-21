package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 店铺问答内容状态 fs_dict.qa_content_state */
public final class QaContentState {

    public static final int NORMAL = 1;
    public static final int HIDDEN = 2;
    public static final int DELETED = 3;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(NORMAL, "正常");
        c2l.put(HIDDEN, "隐藏");
        c2l.put(DELETED, "删除");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);
        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private QaContentState() {
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
            return NORMAL;
        }
        Integer exact = LABEL_TO_CODE.get(t);
        if (exact != null) {
            return exact;
        }
        if (t.contains("隐藏")) {
            return HIDDEN;
        }
        if (t.contains("删除")) {
            return DELETED;
        }
        if (t.contains("正常")) {
            return NORMAL;
        }
        return null;
    }
}
