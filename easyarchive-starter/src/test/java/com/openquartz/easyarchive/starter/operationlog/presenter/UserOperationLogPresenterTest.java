package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.common.SysUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserOperationLogPresenterTest {

    @Test
    void shouldRenderChineseUserUpdateWithoutPlainPassword() {
        SysUser before = new SysUser();
        before.setId(1L);
        before.setUsername("alice");
        before.setRealName("张三");
        before.setRoleCode("normal_user");
        before.setStatus(0);

        SysUser after = new SysUser();
        after.setId(1L);
        after.setUsername("alice");
        after.setRealName("李四");
        after.setRoleCode("platform_admin");
        after.setStatus(1);

        UserOperationLogPresenter presenter = new UserOperationLogPresenter();
        String content = presenter.buildUpdate(before, after, true).getContent();

        assertTrue(content.contains("\"姓名\" 从 \"张三\" 修改为：\"李四\""));
        assertTrue(content.contains("\"角色\" 从 \"普通用户\" 修改为：\"管理员\""));
        assertTrue(content.contains("\"状态\" 从 \"启用\" 修改为：\"停用\""));
        assertTrue(content.contains("\"密码\" 已更新"));
    }

    @Test
    void shouldRenderCreateContentWithChineseLabels() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("bob");
        user.setRealName("王五");
        user.setRoleCode("archive_admin");
        user.setStatus(0);

        UserOperationLogPresenter presenter = new UserOperationLogPresenter();

        assertEquals("新增用户：\"用户名\" 为 \"bob\"; \"姓名\" 为 \"王五\"; \"角色\" 为 \"归档管理员\"; \"状态\" 为 \"启用\"",
                presenter.buildCreate(user).getContent());
    }
}
