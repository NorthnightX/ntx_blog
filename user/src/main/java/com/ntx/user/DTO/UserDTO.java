package com.ntx.user.DTO;


import lombok.Data;


@Data
public class UserDTO {
    private Integer id;
    /**
     * 用户名
     */
    private String name;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 用户头像
     */
    private String image;

    /**
     * 邮件
     */
    private String email;

    /**
     * 联系电话
     */
    private String phone;

}
