package com.openquartz.easyarchive.starter.operationlog;

public class OperationValueFormatter {

    public String formatDatasourceStatus(Integer value) {
        if (value == null) {
            return "";
        }
        switch (value) {
            case 0:
                return "未测试";
            case 1:
                return "正常";
            case 2:
                return "异常";
            case 3:
                return "禁用";
            default:
                return String.valueOf(value);
        }
    }

    public String formatEnableStatus(Integer value) {
        if (value == null) {
            return "";
        }
        return value == 0 ? "启用" : "停用";
    }

    public String formatBooleanSwitch(Integer value) {
        if (value == null) {
            return "";
        }
        return value == 0 ? "是" : "否";
    }

    public String changedPasswordText() {
        return "\"密码\" 已更新";
    }

    public String formatUserStatus(Integer value) {
        if (value == null) {
            return "";
        }
        return value == 0 ? "启用" : "停用";
    }

    public String formatUserRole(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return "";
        }
        if ("ADMIN".equalsIgnoreCase(roleCode)) {
            return "管理员";
        }
        if ("USER".equalsIgnoreCase(roleCode)) {
            return "普通用户";
        }
        return roleCode;
    }

    public String joinNames(Iterable<String> values) {
        if (values == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("、");
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
