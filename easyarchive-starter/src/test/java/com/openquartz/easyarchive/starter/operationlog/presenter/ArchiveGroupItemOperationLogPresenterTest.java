package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchiveGroupItemOperationLogPresenterTest {

    @Test
    void shouldRenderIdItemStatusChangeInChinese() {
        ArchiveGroupItemById before = new ArchiveGroupItemById();
        before.setId(20L);
        before.setGroupId(10L);
        before.setSourceTable("t_order");
        before.setTargetTable("t_order_archive");
        before.setEnableStatus(1);

        ArchiveGroupItemById after = new ArchiveGroupItemById();
        after.setId(20L);
        after.setGroupId(10L);
        after.setSourceTable("t_order");
        after.setTargetTable("t_order_archive");
        after.setEnableStatus(0);

        ArchiveGroupItemOperationLogPresenter presenter = new ArchiveGroupItemOperationLogPresenter();

        assertEquals("执行“启用分组项”操作，将 \"启用状态\" 从 \"停用\" 修改为：\"启用\"",
                presenter.buildIdStatusUpdate(before, after).getContent());
    }
}
