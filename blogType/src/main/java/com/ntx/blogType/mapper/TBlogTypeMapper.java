package com.ntx.blogType.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntx.common.domain.TBlogType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_blog_type】的数据库操作Mapper
* @createDate 2023-07-24 15:46:54
* @Entity generator.domain.TBlogType
*/
@Mapper
public interface TBlogTypeMapper extends BaseMapper<TBlogType> {

    TBlogType getTypeById(int id);

    List<TBlogType> getByIds(List<Long> ids);

    List<TBlogType> queryBlogPage(int start, int pageSize, String name);

    List<TBlogType> queryByBlogger(int blogger);


}




