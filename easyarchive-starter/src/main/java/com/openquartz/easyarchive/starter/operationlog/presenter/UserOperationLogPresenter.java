package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationValueFormatter;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class UserOperationLogPresenter {

    private final OperationValueFormatter formatter = new OperationValueFormatter();

    public OperationLogCommand buildCreate(SysUser user) {
        List<String> segments = new ArrayList<>();
        segments.add("新增用户");
        segments.add(fieldValue("用户名", user.getUsername()));
        if (StringUtils.hasText(user.getRealName())) {
            segments.add(fieldValue("姓名", user.getRealName()));
        }
        if (StringUtils.hasText(user.getMobile())) {
            segments.add(fieldValue("手机号", user.getMobile()));
        }
        if (StringUtils.hasText(user.getEmail())) {
            segments.add(fieldValue("邮箱", user.getEmail()));
        }
        segments.add(fieldValue("角色", roleName(user.getRoleCode())));
        segments.add(fieldValue("状态", statusName(user.getStatus())));
        return new OperationLogCommand("USER", "CREATE", "新增用户", "USER",
                user.getId(), user.getUsername(), joinCreateSegments(segments), Collections.emptyList());
    }

    public OperationLogCommand buildUpdate(SysUser before, SysUser after, boolean passwordUpdated) {
        List<String> changes = new ArrayList<>();
        appendIfChanged(changes, "用户名", before.getUsername(), after.getUsername());
        appendIfChanged(changes, "姓名", before.getRealName(), after.getRealName());
        appendIfChanged(changes, "手机号", before.getMobile(), after.getMobile());
        appendIfChanged(changes, "邮箱", before.getEmail(), after.getEmail());
        appendIfChanged(changes, "角色", roleName(before.getRoleCode()), roleName(after.getRoleCode()));
        appendIfChanged(changes, "状态", statusName(before.getStatus()), statusName(after.getStatus()));
        appendIfChanged(changes, "备注", before.getRemark(), after.getRemark());
        if (passwordUpdated) {
            changes.add("\"密码\" 已更新");
        }
        return new OperationLogCommand("USER", "UPDATE", "编辑用户", "USER",
                after.getId(), after.getUsername(), String.join("; ", changes), Collections.emptyList());
    }

    public OperationLogCommand buildStatusUpdate(SysUser before, SysUser after) {
        String content = fieldChange("状态", statusName(before.getStatus()), statusName(after.getStatus()));
        return new OperationLogCommand("USER", "STATUS", "修改用户状态", "USER",
                after.getId(), after.getUsername(), content, Collections.emptyList());
    }

    private String joinCreateSegments(List<String> segments) {
        if (segments.isEmpty()) {
            return "新增用户";
        }
        StringBuilder builder = new StringBuilder(segments.get(0)).append("：");
        for (int i = 1; i < segments.size(); i++) {
            if (i > 1) {
                builder.append("; ");
            }
            builder.append(segments.get(i));
        }
        return builder.toString();
    }

    private void appendIfChanged(List<String> changes, String label, String before, String after) {
        String left = normalize(before);
        String right = normalize(after);
        if (!left.equals(right)) {
            changes.add(fieldChange(label, left, right));
        }
    }

    private String fieldValue(String label, String value) {
        return "\"" + label + "\" 为 \"" + normalize(value) + "\"";
    }

    private String fieldChange(String label, String before, String after) {
        return "\"" + label + "\" 从 \"" + normalize(before) + "\" 修改为：\"" + normalize(after) + "\"";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private String roleName(String roleCode) {
        if (RoleConstants.isAdmin(roleCode)) {
            return "管理员";
        }
        if (RoleConstants.isUser(roleCode)) {
            return "普通用户";
        }
        return StringUtils.hasText(roleCode) ? roleCode : "";
    }

    private String statusName(Integer status) {
        return formatter.formatUserStatus(status);
    }
}
