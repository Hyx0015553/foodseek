package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单状态码（与 fs_dict.dict_group=bill_state 一致）
 */
public final class BillState {

    public static final int PENDING_PAY = 1;
    public static final int PAID = 2;
    public static final int SHIPPED = 3;
    public static final int COMPLETED = 4;
    public static final int REVIEWED = 5;
    public static final int CANCELLED = 6;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(PENDING_PAY, "待付款");
        c2l.put(PAID, "已付款");
        c2l.put(SHIPPED, "已发货");
        c2l.put(COMPLETED, "已完成");
        c2l.put(REVIEWED, "已评价");
        c2l.put(CANCELLED, "已取消");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);

        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private BillState() {
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
        for (Map.Entry<String, Integer> e : LABEL_TO_CODE.entrySet()) {
            if (t.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /** 已付款及之后（用于 GMV、商家待办等统计） */
    public static boolean isPaidOrLater(Integer state) {
        if (state == null) {
            return false;
        }
        return state >= PAID && state != CANCELLED;
    }

    public static boolean isPaidOrLaterLabel(String statecn) {
        return isPaidOrLater(codeOfLabel(statecn));
    }
}
