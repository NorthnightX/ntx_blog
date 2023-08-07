package com.ntx.blog.controller;

import com.ntx.blog.domain.TLikeBlog;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TLikeBlogService;
import com.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/isLike")
public class LikeBlogController {
    @Autowired
    private TLikeBlogService likeBlogService;


    /**
     * 点赞
     * @param likeBlog
     * @return
     */
    @PostMapping("/likeBlog")
    public Result likeBlog(@RequestBody TLikeBlog likeBlog){
        return likeBlogService.likeBlog(likeBlog);
    }
}
