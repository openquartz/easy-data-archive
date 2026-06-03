package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveGroupOperationLogPresenterTest {

    @Test
    void shouldTranslateDatasourceIdsAndOnlyRenderChangedFields() {
        ArchiveGroup before = new ArchiveGroup();
        before.setId(10L);
        before.setGroupCode("ORDER_ARCHIVE");
        before.setGroupName("订单归档");
        before.setTargetDatasourceId(2L);

        ArchiveGroup after = new ArchiveGroup();
        after.setId(10L);
        after.setGroupCode("ORDER_ARCHIVE");
        after.setGroupName("订单归档华东");
        after.setTargetDatasourceId(3L);

        ArchiveGroupOperationLogPresenter presenter =
                new ArchiveGroupOperationLogPresenter(id -> id == 2L ? "archive_old" : "archive_new");

        String content = presenter.buildUpdate(before, after).getContent();

        assertTrue(content.contains("\"分组名称\" 从 \"订单归档\" 修改为：\"订单归档华东\""));
        assertTrue(content.contains("\"目标数据源\" 从 \"archive_old\" 修改为：\"archive_new\""));
    }
}
