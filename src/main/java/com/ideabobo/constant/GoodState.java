package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品上架状态（与 fs_dict.good_state 一致）
 */
public final class GoodState {

    public static final int ON_SHELF = 1;
    public static final int OFF_SHELF = 2;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(ON_SHELF, "上架中");
        c2l.put(OFF_SHELF, "已下架");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);

        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private GoodState() {
    }

    public static String labelOf(Integer code) {
        if (code == null) {
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
        if (t.contains("下架")) {
            return OFF_SHELF;
        }
        if (t.contains("上架")) {
            return ON_SHELF;
        }
        return null;
    }

    public static boolean isOnShelf(Integer state) {
        return state != null && state == ON_SHELF;
    }
}
