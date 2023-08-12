package com.ntx.blog.service;

import com.ntx.blog.VO.DeleteCommentVO;
import com.ntx.blog.domain.TComment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.common.domain.Result;

import java.io.IOException;

/**
* @author NorthnightX
* @description 针对表【t_comment】的数据库操作Service
* @createDate 2023-08-06 19:43:15
*/
public interface TCommentService extends IService<TComment> {

//    Result saveComment(TComment comment);

    Result getCommentByBlog(int id);

    Result deleteComment(DeleteCommentVO commentVO) throws IOException;
}
