package com.ntx.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.user.domain.LoginForm;
import com.ntx.common.domain.Result;
import com.ntx.user.domain.TUser;

import java.io.IOException;

/**
* @author NorthnightX
* @description 针对表【t_user】的数据库操作Service
* @createDate 2023-07-24 15:02:51
*/
public interface TUserService extends IService<TUser> {

    TUser getUserById(int id);

    Result login(LoginForm loginForm);

    Result getVerificationCode() throws IOException;

    Result phoneCode(String phone);
}
