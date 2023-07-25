package com.ntx.user.domain;

import lombok.Data;

@Data
public class LoginForm {
    private String username;
    private String password;
    private String phone;
    private String phoneCode;
    private String code;
    private String codeKey;
}
