package com.ntx.user.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


@Data
public class UserDTO {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户名
     */
    private String name;

    /**
     * 密码
     */
    private String password;
    /**
     * 昵称
     */
    private String nickName;

    /**
     * 邮件
     */
    private String email;
    /**
     * 联系电话
     */
    private String phone;

    /**
     * 用户头像
     */
    private String image;


}
