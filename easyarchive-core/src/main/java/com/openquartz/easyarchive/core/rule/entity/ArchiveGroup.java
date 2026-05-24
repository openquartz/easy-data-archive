package com.openquartz.easyarchive.core.rule.entity;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveGroup extends BaseEntity {

    /**
     * ID
     */
    private Integer id;

    /**
     * 分组编码。唯一
     */
    private String groupCode;

    /**
     * 分组名
     */
    private String groupName;

    /**
     * 源库ID
     * @see ArchiveConnection#getId()
     */
    private Integer sourceConnectionId;

    /**
     * 目标库ID. 可选
     * @see ArchiveConnection#getId()
     */
    private Integer targetConnectionId;

    /**
     * 所属人ID
     */
    private Long ownerUserId;

    /**
     * 启用/禁用状态
     * 0-启用，1-禁用
     */
    private Integer enableStatus;
}
