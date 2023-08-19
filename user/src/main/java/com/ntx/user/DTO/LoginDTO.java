package com.ntx.user.DTO;

import lombok.Data;

@Data
public class LoginDTO {
    private UserDTO userDTO;
    private String token;
}
