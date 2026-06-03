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
}
