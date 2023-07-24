package com.ntx.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.mapper.TBlogMapper;
import com.ntx.blog.service.TBlogService;
import org.springframework.stereotype.Service;

/**
* @author NorthnightX
* @description 针对表【t_blog】的数据库操作Service实现
* @createDate 2023-07-24 15:40:56
*/
@Service
public class TBlogServiceImpl extends ServiceImpl<TBlogMapper, TBlog>
    implements TBlogService {

}




