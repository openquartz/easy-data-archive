package com.openquartz.easyarchive.core.rule;

/**
 * 归档任务项
 */
public interface ArchiveGroupItem {

    /**
     * 归档分组ID
     * @return ID
     */
    Long getGroupId();

    /**
     * ID 列名
     * @return ID
     */
    String getIdColumn();

    /**
     * 来源表，需要支持自定义表达式
     * @return 来源表
     */
    String getSourceTable();

    /**
     * 目标表,需要支持自定义表达式
     * @return 目标表
     */
    String getTargetTable();

    /**
     * 优先级
     * @return 优先级
     */
    Integer getPriority();

    /**
     * 是否有效
     * @return 是否有效
     */
    boolean valid();

    /**
     * 获取sql
     * @return sql
     */
    String getFetchSql();

    /**
     * 1-启用，0-禁用
     * @return 是否启用
     */
    boolean enableClean();

    /**
     * 删除数据条件。主要用于乐观锁。删除数据时使用
     * @return 删除条件
     */
    String getDeleteWhere();

    /**
     * 1-启用，0-禁用
     * @return 是否启用
     */
    boolean enableWrite();
}
