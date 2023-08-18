package com.ntx.user.DTO;

import lombok.Data;

@Data
public class LoginResultDTO {
    private UserDTO userDTO;
    private String token;
}
