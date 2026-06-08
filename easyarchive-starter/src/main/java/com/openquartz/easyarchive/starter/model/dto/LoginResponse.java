package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse implements Serializable {

    private String token;
    private String username;
    private String realName;
    private String roleCode;
    private List<String> permissions;
    private List<String> capabilities;
    private Long expiresIn;
}
