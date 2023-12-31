package com.ntx.blogType.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.blogType.mapper.TBlogTypeMapper;
import com.ntx.blogType.service.TBlogTypeService;

import com.ntx.common.domain.Result;
import com.ntx.common.domain.TBlogType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_blog_type】的数据库操作Service实现
* @createDate 2023-07-24 15:46:54
*/
@Service
public class TBlogTypeServiceImpl extends ServiceImpl<TBlogTypeMapper, TBlogType> implements TBlogTypeService{

    @Autowired
    private TBlogTypeMapper blogTypeMapper;
    @Override
    public TBlogType getTypeById(int id) {
        return blogTypeMapper.getTypeById(id);
    }

    /**
     * blogController请求类型数据
     * @param ids
     * @return
     */
    @Override
    public List<TBlogType> getByIds(List<Long> ids) {
        return blogTypeMapper.getByIds(ids);
    }

    /**
     * 分页查询
     * @param pageNum
     * @param pageSize
     * @param name
     * @return
     */
    @Override
    public List<TBlogType> getPage(Integer pageNum, Integer pageSize, String name) {
        int start = (pageNum - 1) * pageSize;
        return blogTypeMapper.queryBlogPage(start, pageSize, name);
    }

    /**
     * 根据用户id查找type
     * @param id
     * @return
     */
    @Override
    public Result getTypeByUser(int id) {
        List<TBlogType> tBlogTypes = blogTypeMapper.queryByBlogger(id);
        return Result.success(tBlogTypes);
    }
}




