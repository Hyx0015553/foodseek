package com.ideabobo.util;

import com.ideabobo.constant.CouponState;
import com.ideabobo.model.Youhuiquan;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 优惠券表规范化：state/deleted 数值化，extimestr 由 extime 派生。
 */
public final class YouhuiquanSupport {

    public static final int TYPE_TEMPLATE = 1;
    public static final int TYPE_USER = 2;

    private static final SimpleDateFormat EXPIRE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private YouhuiquanSupport() {
    }

    public static boolean isCouponTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        return tableName.toLowerCase().endsWith("youhuiquan");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object st = row.get("state");
        if (st != null) {
            Integer code = BillSupport.toInteger(st);
            if (code != null && code > 0) {
                row.put("statecn", CouponState.labelOf(code));
            }
        }
        Object ext = row.get("extime");
        if (ext != null) {
            try {
                long sec = Long.parseLong(ext.toString().trim());
                if (sec > 0) {
                    row.put("extimestr", EXPIRE_FMT.format(new Date(sec * 1000L)));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        Object del = row.get("deleted");
        if (del != null) {
            int d = BillSupport.toInteger(del) != null ? BillSupport.toInteger(del) : 0;
            row.put("deletestate", d == 1 ? "已删除" : "");
        }
    }

    public static void enrichRows(List<Map<String, Object>> rows) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            enrichRow(row);
        }
    }

    public static String resolveListFieldName(String tableName, String fieldName) {
        if (!isCouponTable(tableName) || fieldName == null) {
            return null;
        }
        if ("statecn".equals(fieldName)) {
            return "state";
        }
        if ("deletestate".equals(fieldName)) {
            return "deleted";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isCouponTable(tableName) || value == null) {
            return value;
        }
        if ("statecn".equals(fieldName)) {
            if (value instanceof Integer) {
                return value;
            }
            Integer code = CouponState.codeOfLabel(value.toString());
            return code != null ? code : value;
        }
        if ("deletestate".equals(fieldName)) {
            String t = value.toString().trim();
            if (t.isEmpty()) {
                return 0;
            }
            if ("已删除".equals(t)) {
                return 1;
            }
            return value;
        }
        return value;
    }

    public static void applyListRequest(Youhuiquan coupon, HttpServletRequest request) {
        if (coupon == null || request == null) {
            return;
        }
        mapFromRequest(coupon, request, false);
    }

    public static void applySaveRequest(Youhuiquan coupon, HttpServletRequest request) {
        if (coupon == null || request == null) {
            return;
        }
        mapFromRequest(coupon, request, true);
    }

    private static void mapFromRequest(Youhuiquan coupon, HttpServletRequest request, boolean defaultNormal) {
        String statecn = request.getParameter("statecn");
        if (statecn != null && !statecn.trim().isEmpty()) {
            Integer code = CouponState.codeOfLabel(statecn);
            if (code != null) {
                coupon.setState(code);
            }
        }
        String state = request.getParameter("state");
        if (state != null && !state.trim().isEmpty()) {
            try {
                coupon.setState(Integer.parseInt(state.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        String deletestate = request.getParameter("deletestate");
        if (deletestate != null) {
            coupon.setDeleted("已删除".equals(deletestate.trim()) ? 1 : 0);
        }
        String deleted = request.getParameter("deleted");
        if (deleted != null && !deleted.trim().isEmpty()) {
            try {
                coupon.setDeleted(Integer.parseInt(deleted.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (coupon.getExtime() == null) {
            String extimestr = request.getParameter("extimestr");
            Integer parsed = parseExtimeFromStr(extimestr);
            if (parsed != null) {
                coupon.setExtime(parsed);
            }
        }
        if (defaultNormal && coupon.getTypeid() != null && coupon.getTypeid() == TYPE_USER
                && coupon.getState() == null) {
            coupon.setState(CouponState.NORMAL);
        }
    }

    public static Integer parseExtimeFromStr(String extimestr) {
        if (extimestr == null || extimestr.trim().isEmpty()) {
            return null;
        }
        try {
            Date d = EXPIRE_FMT.parse(extimestr.trim());
            return (int) (d.getTime() / 1000L);
        } catch (ParseException e) {
            try {
                Date d2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(extimestr.trim());
                return (int) (d2.getTime() / 1000L);
            } catch (ParseException ignored) {
                return null;
            }
        }
    }
}
