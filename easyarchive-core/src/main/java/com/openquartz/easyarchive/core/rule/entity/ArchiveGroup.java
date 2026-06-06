package com.openquartz.easyarchive.core.rule.entity;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 归档分组
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveGroup extends BaseEntity {

    /**
     * ID
     */
    private Long id;

    /**
     * 父分组ID
     */
    private Long parentId;

    /**
     * 分组编码。唯一
     */
    private String groupCode;

    /**
     * 分组名
     */
    private String groupName;

    /**
     * 分组路径
     */
    private String groupPath;

    /**
     * 分组层级
     */
    private Integer groupLevel;

    /**
     * 源数据源ID
     */
    private Long sourceDatasourceId;

    /**
     * 目标数据源ID
     */
    private Long targetDatasourceId;

    /**
     * 所属人ID
     */
    private Long ownerUserId;

    /**
     * 启用/禁用状态
     * 0-启用，1-禁用
     */
    private Integer enableStatus;

    /**
     * 是否启用完成通知
     * 0-关闭，1-开启
     */
    private Integer notifyEnabled;

    /**
     * 通知渠道
     */
    private String notifyChannel;

    /**
     * 通知 webhook 地址
     */
    private String notifyWebhookUrl;

    /**
     * 是否启用站内通知
     * 0-关闭，1-开启
     */
    private Integer inAppNotifyEnabled;

    /**
     * 备注
     */
    private String remark;
}
