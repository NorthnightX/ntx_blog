package com.ntx.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntx.common.domain.TUser;
import org.apache.ibatis.annotations.Mapper;


/**
* @author NorthnightX
* @description 针对表【t_user】的数据库操作Mapper
* @createDate 2023-07-24 15:02:51
* @Entity generator.domain.TUser*/
@Mapper
public interface TUserMapper extends BaseMapper<TUser> {

    TUser queryUserById(int id);

    TUser getLoginUser(int id);
}




