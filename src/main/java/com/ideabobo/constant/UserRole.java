package com.ideabobo.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户角色（与 fs_dict.user_role 一致）
 */
public final class UserRole {

    public static final int SUPER_ADMIN = 1;
    public static final int USER = 2;
    public static final int MERCHANT = 3;
    public static final int OPERATOR = 5;
    public static final int AUDITOR = 6;

    private static final Map<Integer, String> CODE_TO_LABEL;
    private static final Map<String, Integer> LABEL_TO_CODE;

    static {
        Map<Integer, String> c2l = new HashMap<>();
        c2l.put(SUPER_ADMIN, "超级管理员");
        c2l.put(USER, "用户");
        c2l.put(MERCHANT, "商家");
        c2l.put(OPERATOR, "运营管理员");
        c2l.put(AUDITOR, "审核管理员");
        CODE_TO_LABEL = Collections.unmodifiableMap(c2l);

        Map<String, Integer> l2c = new HashMap<>();
        for (Map.Entry<Integer, String> e : c2l.entrySet()) {
            l2c.put(e.getValue(), e.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(l2c);
    }

    private UserRole() {
    }

    public static String codeAsString(Integer role) {
        return role == null ? "" : String.valueOf(role);
    }

    public static String labelOf(Integer role) {
        if (role == null) {
            return "";
        }
        String label = CODE_TO_LABEL.get(role);
        return label != null ? label : "";
    }

    public static Integer codeOfString(String roletype) {
        if (roletype == null) {
            return null;
        }
        String t = roletype.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            int code = Integer.parseInt(t);
            if (CODE_TO_LABEL.containsKey(code)) {
                return code;
            }
        } catch (NumberFormatException ignored) {
        }
        Integer byLabel = LABEL_TO_CODE.get(t);
        if (byLabel != null) {
            return byLabel;
        }
        if (t.contains("商家")) {
            return MERCHANT;
        }
        if (t.contains("运营")) {
            return OPERATOR;
        }
        if (t.contains("审核")) {
            return AUDITOR;
        }
        if (t.contains("管理员")) {
            return SUPER_ADMIN;
        }
        return USER;
    }
}
