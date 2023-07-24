package com.ntx.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.user.domain.TUser;
import com.ntx.user.mapper.TUserMapper;
import com.ntx.user.service.TUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author NorthnightX
* @description 针对表【t_user】的数据库操作Service实现
* @createDate 2023-07-24 15:02:51
*/
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements TUserService {

    @Autowired
    private TUserMapper userMapper;

    @Override
    public TUser getUserById(int id) {
        return userMapper.queryUserById(id);
    }
}




