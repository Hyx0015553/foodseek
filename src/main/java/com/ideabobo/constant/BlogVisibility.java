package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 动态可见性 fs_dict.blog_visibility */
public final class BlogVisibility {

    public static final int PUBLIC = 1;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(PUBLIC, "公开");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);
        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private BlogVisibility() {
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
        if (t.contains("公开")) {
            return PUBLIC;
        }
        return null;
    }
}
