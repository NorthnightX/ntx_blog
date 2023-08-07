package com.ntx.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.blog.domain.TComment;
import com.ntx.blog.service.TCommentService;
import com.ntx.blog.mapper.TCommentMapper;
import com.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author NorthnightX
* @description 针对表【t_comment】的数据库操作Service实现
* @createDate 2023-08-06 19:43:15
*/
@Service
public class TCommentServiceImpl extends ServiceImpl<TCommentMapper, TComment>
    implements TCommentService{

    @Autowired
    private TCommentMapper commentMapper;

    @Override
    public Result saveComment(TComment comment) {
        Boolean save = commentMapper.saveComment(comment);
        return save ? Result.success("评论成功") : Result.error("网络异常");
    }
}




