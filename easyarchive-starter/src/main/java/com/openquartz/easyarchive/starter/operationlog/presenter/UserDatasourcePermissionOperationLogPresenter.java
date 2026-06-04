package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Component
public class UserDatasourcePermissionOperationLogPresenter {

    public OperationLogCommand buildGrant(SysUser user, List<String> beforeNames, List<String> afterNames) {
        return build("GRANT", "授予用户数据源权限", user, beforeNames, afterNames);
    }

    public OperationLogCommand buildRevoke(SysUser user, List<String> beforeNames, List<String> afterNames) {
        return build("REVOKE", "撤销用户数据源权限", user, beforeNames, afterNames);
    }

    public OperationLogCommand buildReplace(SysUser user, List<String> beforeNames, List<String> afterNames) {
        return build("REPLACE", "替换用户数据源权限", user, beforeNames, afterNames);
    }

    private OperationLogCommand build(String action, String buttonName, SysUser user,
                                      List<String> beforeNames, List<String> afterNames) {
        String content = buttonName + "：\"用户名称\" 为 \"" + userDisplayName(user)
                + "\"; \"变更前数据源权限\" 为 \"" + joinNames(beforeNames)
                + "\"; \"变更后数据源权限\" 为 \"" + joinNames(afterNames) + "\"";
        return new OperationLogCommand("USER_DATASOURCE_PERMISSION", action, buttonName, "USER",
                user.getId(), user.getUsername(), content, Collections.emptyList());
    }

    private String userDisplayName(SysUser user) {
        if (StringUtils.hasText(user.getRealName())) {
            return user.getUsername() + "（" + user.getRealName() + "）";
        }
        return user.getUsername();
    }

    private String joinNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "无";
        }
        return String.join("、", names);
    }
}
