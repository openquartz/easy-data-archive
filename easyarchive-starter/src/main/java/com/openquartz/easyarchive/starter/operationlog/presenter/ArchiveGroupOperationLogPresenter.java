package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;

import java.util.Collections;
import java.util.function.Function;

public class ArchiveGroupOperationLogPresenter {

    private final Function<Long, String> datasourceNameResolver;

    public ArchiveGroupOperationLogPresenter(Function<Long, String> datasourceNameResolver) {
        this.datasourceNameResolver = datasourceNameResolver;
    }

    public OperationLogCommand buildUpdate(ArchiveGroup before, ArchiveGroup after) {
        String content = "\"分组名称\" 从 \"" + before.getGroupName() + "\" 修改为：\"" + after.getGroupName() + "\""
                + "; \"目标数据源\" 从 \"" + datasourceNameResolver.apply(before.getTargetDatasourceId()) + "\" 修改为：\""
                + datasourceNameResolver.apply(after.getTargetDatasourceId()) + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP", "UPDATE", "保存分组", "ARCHIVE_GROUP",
                after.getId(), after.getGroupCode(), content, Collections.emptyList());
    }
}
