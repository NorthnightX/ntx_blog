package com.ntx.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntx.blog.domain.TComment;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TCollectBlogService;
import com.ntx.blog.service.TCommentService;
import com.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private TCommentService commentService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private TBlogService blogService;

    /**
     * 新增评论
     * @param comment
     * @return
     */
    @PostMapping("/addComment")
    public Result addComment(@RequestBody TComment comment){
        comment.setDeleted(1);
        comment.setCreateTime(LocalDateTime.now());
        comment.setModifyTime(LocalDateTime.now());
//        return commentService.save(comment) ? Result.success("评论成功") : Result.error("网络异常");
        return commentService.saveComment(comment);
    }

    /**
     * 查找BLog对应的评论
     * @param id
     * @return
     */
    @GetMapping("/getCommentByBlog/{id}")
    public Result queryCommentByBlogId(@PathVariable int id){
        return commentService.getCommentByBlog(id);
    }



}
