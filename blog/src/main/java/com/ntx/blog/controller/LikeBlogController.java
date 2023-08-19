package com.ntx.blog.controller;

import com.alibaba.fastjson.JSON;
import com.ntx.blog.domain.TLikeBlog;
import com.ntx.blog.service.TLikeBlogService;
import com.ntx.blog.utils.UserHolder;
import com.ntx.common.domain.Result;
import com.ntx.common.domain.TUser;
import com.ntx.common.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/isLike")
public class LikeBlogController {
    @Autowired
    private TLikeBlogService likeBlogService;


    /**
     * 点赞
     *
     * @param likeBlog
     * @return
     */
    @PostMapping("/likeBlog")
    public Result likeBlog(@RequestBody TLikeBlog likeBlog) {
        try {
            TUser user = UserHolder.getUser();
            likeBlog.setUserId(user.getId());
            return likeBlogService.likeBlog(likeBlog);
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 查找用户喜欢的文章
     *
     * @return
     */
    @GetMapping("/queryLikeByUser")
    public Result queryLikeByUser() {
        try {
            Integer id = UserHolder.getUser().getId();
            return likeBlogService.queryLikeByUser(id);
        } finally {
            UserHolder.removeUser();
        }
    }
}
