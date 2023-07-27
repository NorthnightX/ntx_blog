package com.ntx.blog.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.mapper.TBlogMapper;
import com.ntx.blog.service.TBlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_blog】的数据库操作Service实现
* @createDate 2023-07-24 15:40:56
*/
@Service
public class TBlogServiceImpl extends ServiceImpl<TBlogMapper, TBlog>
    implements TBlogService {

    @Autowired
    private TBlogMapper blogMapper;
    @Override
    public int updateBlodById(TBlog blog) {
        return blogMapper.updateBlogById(blog);
    }

    /**
     * 分页
     * @param pageNum
     * @param pageSize
     * @param tBlog
     * @return
     */
    @Override
    public List<TBlog> getPage(Integer pageNum, Integer pageSize, TBlog tBlog) {
        int start = (pageNum - 1) * pageSize;
        int end = pageNum * pageSize;
        return blogMapper.getPage(start, end, tBlog);
    }
}




