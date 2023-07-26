package com.ntx.blogType.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.blogType.domain.TBlogType;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_blog_type】的数据库操作Service
* @createDate 2023-07-24 15:46:54
*/
public interface TBlogTypeService extends IService<TBlogType> {

    TBlogType getTypeById(int id);

    List<TBlogType> getByIds(List<Long> ids);
}
