package com.ntx.blog.controller;

import com.alibaba.fastjson.JSON;
import com.ntx.blog.VO.DeleteCommentVO;
import com.ntx.blog.domain.TComment;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TCommentService;
import com.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private TCommentService commentService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private TBlogService blogService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 新增评论
     * @param comment
     * @return
     */
    @PostMapping("/addComment")
    public Result addComment(@RequestBody TComment comment){
        kafkaTemplate.send("blogComment","", JSON.toJSONString(comment));
        return Result.success("评论成功");
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

    /**
     * 删除评论
     * @param commentVO
     * @return
     */
    @PutMapping("/deleteComment")
    public Result deleteComment(@RequestBody DeleteCommentVO commentVO) throws IOException {
        return commentService.deleteComment(commentVO);
    }

}
