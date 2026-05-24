package com.openquartz.easyarchive.core.connection.entity;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 归档链接
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArchiveConnection extends BaseEntity {

    private Long id;

    /**
     * 连接编码。唯一
     */
    private String connectCode;

    /**
     * 连接类型。MYSQL、ES等
     */
    private String connectType;

    /**
     * URL
     */
    private String url;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 0-未测试，1-正常，2-异常
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
