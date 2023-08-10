package com.ntx.blog.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.blog.domain.TLikeBlog;
import com.ntx.common.domain.Result;

/**
* @author NorthnightX
* @description 针对表【t_like_blog】的数据库操作Service
* @createDate 2023-08-07 10:37:34
*/
public interface TLikeBlogService extends IService<TLikeBlog> {

    Result likeBlog(TLikeBlog likeBlog);

    Result queryLikeByUser(Integer userId);


}
