package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户持有券状态（fs_dict.coupon_state；模板券 typeid=1 通常无状态）
 */
public final class CouponState {

    public static final int NORMAL = 1;
    public static final int USED = 2;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(NORMAL, "正常");
        c2l.put(USED, "已使用");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);

        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private CouponState() {
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
        if (t.contains("已使用")) {
            return USED;
        }
        if (t.contains("正常")) {
            return NORMAL;
        }
        return null;
    }
}
