package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改密码请求DTO
 */
@Data
public class ChangePasswordRequest implements Serializable {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码长度不能少于8位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
             message = "密码必须包含字母和数字")
    private String newPassword;
}
