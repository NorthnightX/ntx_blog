package com.ntx.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.blog.domain.TBlog;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_blog】的数据库操作Service
* @createDate 2023-07-24 15:40:56
*/
public interface TBlogService extends IService<TBlog> {

    int updateBlodById(TBlog blog);

    List<TBlog> getPage(Integer pageNum, Integer pageSize, TBlog tBlog);
}
