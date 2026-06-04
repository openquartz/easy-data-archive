package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationValueFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.function.Function;

@Component
public class ArchiveGroupOperationLogPresenter {

    private final Function<Long, String> datasourceNameResolver;
    private final OperationValueFormatter formatter = new OperationValueFormatter();

    @Autowired
    public ArchiveGroupOperationLogPresenter(ArchiveConnectionMapper archiveConnectionMapper) {
        this(id -> {
            if (id == null) {
                return "";
            }
            com.openquartz.easyarchive.core.connection.entity.ArchiveConnection datasource =
                    archiveConnectionMapper.selectById(id);
            return datasource == null ? String.valueOf(id) : datasource.getDatasourceCode();
        });
    }

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

    public OperationLogCommand buildCreate(ArchiveGroup group) {
        String content = "新增分组：\"分组编码\" 为 \"" + group.getGroupCode()
                + "\"; \"分组名称\" 为 \"" + group.getGroupName() + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP", "CREATE", "新增分组", "ARCHIVE_GROUP",
                group.getId(), group.getGroupCode(), content, Collections.emptyList());
    }

    public OperationLogCommand buildStatusUpdate(ArchiveGroup before, ArchiveGroup after) {
        String content = "\"启用状态\" 从 \"" + status(before.getEnableStatus()) + "\" 修改为：\""
                + status(after.getEnableStatus()) + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP", "STATUS", "修改分组状态", "ARCHIVE_GROUP",
                after.getId(), after.getGroupCode(), content, Collections.emptyList());
    }

    public OperationLogCommand buildDelete(ArchiveGroup before) {
        String content = "删除分组：\"分组编码\" 为 \"" + before.getGroupCode()
                + "\"; \"分组名称\" 为 \"" + before.getGroupName() + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP", "DELETE", "删除分组", "ARCHIVE_GROUP",
                before.getId(), before.getGroupCode(), content, Collections.emptyList());
    }

    private String status(Integer enableStatus) {
        return formatter.formatEnableStatus(enableStatus);
    }
}
