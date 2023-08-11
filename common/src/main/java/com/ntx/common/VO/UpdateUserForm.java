package com.ntx.common.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UpdateUserForm {

    private Integer id;

    private String name;

    private String nickName;

    private String email;

    private String phone;

    private String image;
    private String password;
    private String field;

}
