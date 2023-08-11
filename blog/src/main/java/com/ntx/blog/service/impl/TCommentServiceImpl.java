package com.ntx.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.blog.domain.TComment;
import com.ntx.blog.dto.CommentDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TCommentService;
import com.ntx.blog.mapper.TCommentMapper;

import com.ntx.common.client.UserClient;
import com.ntx.common.domain.Result;
import com.ntx.common.domain.TUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author NorthnightX
 * @description 针对表【t_comment】的数据库操作Service实现
 * @createDate 2023-08-06 19:43:15
 */
@Service
public class TCommentServiceImpl extends ServiceImpl<TCommentMapper, TComment>
        implements TCommentService {

    @Autowired
    private TCommentMapper commentMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserClient userClient;
    @Autowired
    private TBlogService blogService;
    @Autowired
    private MongoTemplate mongoTemplate;

//    /**
//     * 新增评论
//     * @param comment
//     * @return
//     */
//    @Override
//    public Result saveComment(TComment comment) {
//        comment.setDeleted(1);
//        comment.setCreateTime(LocalDateTime.now());
//        comment.setModifyTime(LocalDateTime.now());
//        Boolean save = commentMapper.saveComment(comment);
//        //评论完成后，将blog的评论数+1
//        if (save) {
//            boolean update = blogService.update().setSql("comment = comment + 1").eq("id", comment.getBlogId()).update();
//            return update ? Result.success("评论成功") : Result.error("网络异常");
//        }
//        return Result.error("网络异常");
//    }

    /**
     * 根据blog查找评论
     *
     * @param id
     * @return
     */
    @Override
    public Result getCommentByBlog(int id) {

        //从MongoDB直接查找，并返回
        Query query = new Query();
        query.addCriteria(Criteria.where("blogId").is(id));
        List<CommentDTO> commentDTOS = mongoTemplate.find(query, CommentDTO.class);
        if(!commentDTOS.isEmpty()){
            return Result.success(commentDTOS);
        }
        //mongoDB没有查找数据库
        LambdaQueryWrapper<TComment> queryWrapper = new LambdaQueryWrapper<>();
        //构造查询wrapper，过滤掉删除掉的评论
        queryWrapper.eq(TComment::getBlogId, id);
        queryWrapper.eq(TComment::getDeleted, 1);
        List<TComment> list = this.list(queryWrapper);
        List<Integer> userIdList = list.stream().map(TComment::getUserId).distinct().collect(Collectors.toList());
        //没评论，直接返回
        if(userIdList.isEmpty()){
            return Result.success(new ArrayList<>());
        }
        List<TUser> userList = userClient.getByIds(userIdList);
        Map<Integer, TUser> userMap = userList.stream().collect(Collectors.toMap(TUser::getId, tUser -> tUser));
        //填充DTO对象
        List<CommentDTO> dtoList = list.stream().map((comment) -> {
            CommentDTO commentDTO = new CommentDTO();
            BeanUtil.copyProperties(comment, commentDTO);
            TUser user = userMap.get(commentDTO.getUserId());
            commentDTO.setUserImage(user.getImage());
            commentDTO.setUserName(user.getNickName());
            return commentDTO;
        }).collect(Collectors.toList());
        mongoTemplate.insertAll(dtoList);
        return Result.success(dtoList);
    }
}




