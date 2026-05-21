package com.ideabobo.util;

import com.ideabobo.constant.BlogVisibility;
import com.ideabobo.model.Blog;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public final class BlogSupport {

    private BlogSupport() {
    }

    public static boolean isBlogTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String t = tableName.toLowerCase();
        return t.endsWith("blog") && !t.contains("blogplan");
    }

    public static void enrichRow(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        Object vis = row.get("visibility");
        if (vis != null) {
            Integer code = BillSupport.toInteger(vis);
            if (code != null && code > 0) {
                row.put("vtype", BlogVisibility.labelOf(code));
            }
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
        if (!isBlogTable(tableName) || fieldName == null) {
            return null;
        }
        if ("vtype".equals(fieldName)) {
            return "visibility";
        }
        return null;
    }

    public static Object resolveListFieldValue(String tableName, String fieldName, Object value) {
        if (!isBlogTable(tableName) || !"vtype".equals(fieldName) || value == null) {
            return value;
        }
        if (value instanceof Integer) {
            return value;
        }
        Integer code = BlogVisibility.codeOfLabel(value.toString());
        return code != null ? code : value;
    }

    public static void applyListRequest(Blog blog, HttpServletRequest request) {
        applyFromRequest(blog, request);
    }

    public static void applySaveRequest(Blog blog, HttpServletRequest request) {
        applyFromRequest(blog, request);
        if (blog.getVisibility() == null) {
            blog.setVisibility(BlogVisibility.PUBLIC);
        }
    }

    private static void applyFromRequest(Blog blog, HttpServletRequest request) {
        if (blog == null || request == null) {
            return;
        }
        String vtype = request.getParameter("vtype");
        if (vtype != null && !vtype.trim().isEmpty()) {
            Integer code = BlogVisibility.codeOfLabel(vtype);
            if (code != null) {
                blog.setVisibility(code);
            }
        }
        String visibility = request.getParameter("visibility");
        if (visibility != null && !visibility.trim().isEmpty()) {
            try {
                blog.setVisibility(Integer.parseInt(visibility.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
