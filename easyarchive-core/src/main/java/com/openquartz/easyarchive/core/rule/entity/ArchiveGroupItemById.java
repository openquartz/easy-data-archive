package com.openquartz.easyarchive.core.rule.entity;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import com.openquartz.easyarchive.common.enums.BinarySwitchEnum;
import com.openquartz.easyarchive.common.enums.EnableStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 按照ID 归档归档配置
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArchiveGroupItemById extends BaseEntity implements ArchiveGroupItem  {

    /**
     * ID
     */
    private Long id;

    /**
     * 来源表名
     */
    private String sourceTable;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 分组ID
     */
    private Long groupId;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 执行sql
     */
    private String fetchSql;

    /**
     * 删除条件
     */
    private String deleteWhere;

    /**
     * start id
     * 支持自定义表达式
     */
    private String startId = "0";

    /**
     * end id
     * 支持自定义表达式
     */
    private String endId = String.valueOf(Long.MAX_VALUE);

    /**
     * 步长
     */
    private Integer stepCount = 1000;

    /**
     * 滚动步长
     */
    private Integer stepRounds = 5000;

    /**
     * 规则停顿时间，单位毫秒，为空时走全局配置
     */
    private Integer pauseMs;

    /**
     * 0-启用清理源数据。1-不启用清理源数据
     */
    private Integer enableClean;

    /**
     * 是否启用写入目标数据
     * 0-启用，1-不启用
     */
    private Integer enableWrite;

    /**
     * 启用状态
     * 0-启用 1-禁用
     */
    private Integer enableStatus;

    /**
     * ID 字段名。默认为"ID"
     */
    private String idColumn = "ID";

    @Override
    public boolean valid() {
        return EnableStatusEnum.isEnabled(enableStatus);
    }

    @Override
    public boolean isCleanEnabled() {
        return BinarySwitchEnum.isOn(enableClean);
    }

    @Override
    public boolean isWriteEnabled() {
        return BinarySwitchEnum.isOn(enableWrite);
    }
}
