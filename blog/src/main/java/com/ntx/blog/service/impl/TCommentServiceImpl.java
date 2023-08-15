package com.ntx.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.blog.VO.DeleteCommentVO;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.domain.TComment;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.dto.CommentDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TCommentService;
import com.ntx.blog.mapper.TCommentMapper;

import com.ntx.common.client.UserClient;
import com.ntx.common.domain.Result;
import com.ntx.common.domain.TUser;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
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
    private UserClient userClient;
    @Autowired
    private TBlogService blogService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private RestHighLevelClient client;
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
        //填充信息回MongoDB
        mongoTemplate.insertAll(dtoList);
        return Result.success(dtoList);
    }

    @Override
    @Transactional
    public Result deleteComment(DeleteCommentVO commentVO) throws IOException {
        //获取博客信息
        TBlog blog = blogService.getById(commentVO.getBlogId());
        //获取博客博主Id
        Integer blogger = blog.getBlogger();
        //获取评论信息
        TComment comment = this.getById(commentVO.getCommentId());
        //获取评论人Id
        Integer commentUserId = comment.getUserId();
        //获取删除者Id
        Integer deleteUserId = commentVO.getUserId();
        //如果删除者是博客博主或者评论发表人
        if(Objects.equals(deleteUserId, blogger) || Objects.equals(deleteUserId, commentUserId)){
            boolean delete = updateDeleteForComment(blog, comment.getId());
            if(delete){
                return Result.success("删除成功");
            }
        }
        List<TUser> userList = userClient.getByIds(Collections.singletonList(deleteUserId));
        for (TUser user : userList) {
            //如果用户是管理员
            if(user.getRole() == 1){
                boolean delete = updateDeleteForComment(blog, comment.getId());
                if(delete){
                    return Result.success("删除成功");
                }
            }
        }
        return Result.error("您没有权限删除该条评论");
    }


    private boolean updateDeleteForComment(TBlog blog, Integer commentId) throws IOException {
        //修改博客评论数
        Integer blogId = blog.getId();
        blogService.update().eq("id", blogId).setSql("comment = comment - 1").update();
        //修改评论的deleted字段
        boolean update = this.update().eq("id", commentId).setSql("deleted = 2").update();
        if(update){
            //移除MongoDB的评论
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(commentId));
            mongoTemplate.remove(query, CommentDTO.class);
            //修改MongoDB中的Blog的评论数量
            Query blogQuery = new Query(Criteria.where("_id").is(blogId));
            Update updateBlog = new Update();
            updateBlog.inc("comment", -1);
            mongoTemplate.updateFirst(blogQuery, updateBlog, BlogDTO.class);
            //修改ES的评论
            UpdateRequest request = new UpdateRequest("blog", String.valueOf(blogId));
            request.doc("comment", blog.getComment() - 1);
            client.update(request, RequestOptions.DEFAULT);
            return true;
        }
        return false;
    }
}




