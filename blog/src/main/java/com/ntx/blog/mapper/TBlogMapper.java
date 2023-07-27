package com.ntx.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntx.blog.domain.TBlog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_blog】的数据库操作Mapper
* @createDate 2023-07-24 15:40:56
* @Entity generator.domain.TBlog
*/
@Mapper
public interface TBlogMapper extends BaseMapper<TBlog> {

    int updateBlogById(TBlog blog);

    List<TBlog> getPage(Integer start, Integer end, TBlog tBlog);
}




