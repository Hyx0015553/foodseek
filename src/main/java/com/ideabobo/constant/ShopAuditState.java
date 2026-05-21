package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 店铺审核状态（与 fs_dict.dict_group=shop_audit 一致）
 */
public final class ShopAuditState {

    public static final int PENDING = 1;
    public static final int APPROVED = 2;
    public static final int REJECTED = 3;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(PENDING, "待审核");
        c2l.put(APPROVED, "审核通过");
        c2l.put(REJECTED, "审核不通过");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);

        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private ShopAuditState() {
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
        if (t.contains("待审核")) {
            return PENDING;
        }
        if (t.contains("不通过")) {
            return REJECTED;
        }
        if (t.contains("通过")) {
            return APPROVED;
        }
        return null;
    }

    public static boolean isApproved(Integer state) {
        return state != null && state == APPROVED;
    }

    public static boolean isApprovedLabel(String statecn) {
        return isApproved(codeOfLabel(statecn));
    }
}
