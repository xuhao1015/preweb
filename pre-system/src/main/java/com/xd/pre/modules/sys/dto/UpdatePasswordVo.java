package com.xd.pre.modules.sys.dto;

import lombok.Data;

@Data
public class UpdatePasswordVo {
    private String username;
    private String password;
    private String token;
    private String newPassword;
}
