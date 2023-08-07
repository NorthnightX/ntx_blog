package com.ntx.blog.mapper;

import com.ntx.blog.domain.TComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author NorthnightX
* @description 针对表【t_comment】的数据库操作Mapper
* @createDate 2023-08-06 19:43:15
* @Entity com.ntx.blog.domain.TComment
*/
@Mapper
public interface TCommentMapper extends BaseMapper<TComment> {

    Boolean saveComment(TComment comment);
}




