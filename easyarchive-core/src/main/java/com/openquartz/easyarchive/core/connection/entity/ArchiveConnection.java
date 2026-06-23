package com.openquartz.easyarchive.core.connection.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.openquartz.easyarchive.common.entity.BaseEntity;
import com.openquartz.easyarchive.common.enums.DatasourceStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * 归档链接
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArchiveConnection extends BaseEntity {

    public static final int STATUS_UNTESTED = DatasourceStatusEnum.UNTESTED.getCode();
    public static final int STATUS_ENABLED = DatasourceStatusEnum.ENABLED.getCode();
    public static final int STATUS_DISABLED = DatasourceStatusEnum.DISABLED.getCode();

    private Long id;

    /**
     * 连接编码。唯一
     */
    @NotBlank(message = "数据源编码不能为空")
    private String connectCode;

    /**
     * 连接类型。MYSQL、ES等
     */
    @NotBlank(message = "数据源类型不能为空")
    private String connectType;

    /**
     * URL
     */
    @NotBlank(message = "连接地址不能为空")
    private String url;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 0-未测试，1-已启用，2-已停用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 展示名
     */
    private String datasourceName;

    /**
     * 最近校验时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date lastCheckTime;

    /**
     * 所属用户 ID
     */
    private Long ownerUserId;

    public String getDatasourceCode() {
        return connectCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.connectCode = datasourceCode;
    }

    public String getDatasourceType() {
        return connectType;
    }

    public void setDatasourceType(String datasourceType) {
        this.connectType = datasourceType;
    }

    public String getJdbcUrl() {
        return url;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.url = jdbcUrl;
    }

    public String getPasswordCipher() {
        return password;
    }

    public void setPasswordCipher(String passwordCipher) {
        this.password = passwordCipher;
    }

}
