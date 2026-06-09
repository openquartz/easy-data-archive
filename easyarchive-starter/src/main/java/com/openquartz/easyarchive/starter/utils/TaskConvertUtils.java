package com.openquartz.easyarchive.starter.utils;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.model.dto.TaskLogVO;
import com.openquartz.easyarchive.starter.model.dto.TaskVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Task Entity → VO 转换工具
 */
public class TaskConvertUtils {

    private TaskConvertUtils() {
    }

    public static TaskVO fromEntity(ArchiveGroupExecuteTask entity) {
        if (entity == null) {
            return null;
        }
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public static List<TaskVO> fromEntityTaskList(List<ArchiveGroupExecuteTask> list) {
        if (list == null) {
            return null;
        }
        return list.stream().map(TaskConvertUtils::fromEntity).collect(Collectors.toList());
    }

    public static TaskLogVO fromEntity(ArchiveTaskLog entity) {
        if (entity == null) {
            return null;
        }
        TaskLogVO vo = new TaskLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public static List<TaskLogVO> fromEntityLogList(List<ArchiveTaskLog> list) {
        if (list == null) {
            return null;
        }
        return list.stream().map(TaskConvertUtils::fromEntity).collect(Collectors.toList());
    }
}
