package com.openquartz.easyarchive.starter.operationlog;

import com.openquartz.easyarchive.common.enums.BinarySwitchEnum;
import com.openquartz.easyarchive.common.enums.DatasourceStatusEnum;
import com.openquartz.easyarchive.common.enums.EnableStatusEnum;

public class OperationValueFormatter {

    public String formatDatasourceStatus(Integer value) {
        DatasourceStatusEnum status = DatasourceStatusEnum.fromCode(value);
        return status == null ? valueToText(value) : status.getDesc();
    }

    public String formatEnableStatus(Integer value) {
        EnableStatusEnum status = EnableStatusEnum.fromCode(value);
        return status == null ? valueToText(value) : status.getDesc();
    }

    public String formatBooleanSwitch(Integer value) {
        BinarySwitchEnum status = BinarySwitchEnum.fromCode(value);
        return status == null ? valueToText(value) : status.getDesc();
    }

    public String changedPasswordText() {
        return "\"密码\" 已更新";
    }

    public String formatUserStatus(Integer value) {
        return formatEnableStatus(value);
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

    private String valueToText(Integer value) {
        return value == null ? "" : String.valueOf(value);
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
