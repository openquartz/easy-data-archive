package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.common.SysUser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDatasourcePermissionOperationLogPresenterTest {

    @Test
    void shouldRenderReplaceContentWithBeforeAfterDatasourceNames() {
        SysUser user = new SysUser();
        user.setId(9L);
        user.setUsername("operator");
        user.setRealName("运维同学");

        UserDatasourcePermissionOperationLogPresenter presenter =
                new UserDatasourcePermissionOperationLogPresenter();

        String content = presenter.buildReplace(user, Collections.singletonList("源A"), Arrays.asList("源B", "源C"))
                .getContent();

        assertEquals("替换用户数据源权限：\"用户名称\" 为 \"operator（运维同学）\"; \"变更前数据源权限\" 为 \"源A\"; \"变更后数据源权限\" 为 \"源B、源C\"",
                content);
    }
}
