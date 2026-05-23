package com.openquartz.easyarchive.core.rule;

import lombok.Data;

/**
 * 连接配置
 * @author svnee
 */
@Data
public class ConnectionConfig {

    /**
     * id
     */
    private Integer id;

    /**
     * 数据源连接名
     */
    private String name;

    /**
     * 连接url
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

}
