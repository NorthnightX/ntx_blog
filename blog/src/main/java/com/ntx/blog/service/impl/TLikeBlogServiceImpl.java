package com.ntx.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.blog.domain.TLikeBlog;
import com.ntx.blog.mapper.TLikeBlogMapper;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TLikeBlogService;
import com.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.ntx.blog.common.SystemContent.*;

/**
 * @author NorthnightX
 * @description 针对表【t_like_blog】的数据库操作Service实现
 * @createDate 2023-08-07 10:37:34
 */
@Service
public class TLikeBlogServiceImpl extends ServiceImpl<TLikeBlogMapper, TLikeBlog>
        implements TLikeBlogService {

    @Autowired
    private TBlogService blogService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Result likeBlog(TLikeBlog likeBlog) {
        //数据在redis用set实现
        //目前有时间bug
        //判断用户是否点赞，如果点过赞，则取消点赞（删除），没点过赞，则新增点赞，如果用户之前的点赞状态是2，即反对，则取消反对，重新点赞
        //从redis中获取点赞数据，获取不到查数据库,如果没有数据，则代表之前用户没有给这篇博客点过赞
        Boolean member = stringRedisTemplate.opsForSet().isMember(BLOG_LIKE_KEY + likeBlog.getUserId(), likeBlog.getBlogId());
        //如果用户之前点过赞
        if (Boolean.TRUE.equals(member)) {
            //取消点赞
            LambdaQueryWrapper<TLikeBlog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TLikeBlog::getBlogId, likeBlog.getBlogId());
            queryWrapper.eq(TLikeBlog::getUserId, likeBlog.getUserId());
            boolean remove = this.remove(queryWrapper);
            if (remove) {
                //更新blog的点赞数量
                boolean update = blogService.update().setSql("like_count = like_count - 1").eq("id", likeBlog.getBlogId()).update();
                if (update) {
                    Long remove1 = stringRedisTemplate.opsForSet().remove(BLOG_COMMENT_KEY + likeBlog.getUserId(), likeBlog.getBlogId());
                    return remove1 != null && remove1 > 0 ? Result.success("已取消点赞") : Result.error("网络异常");
                }
            }
            return Result.error("网络异常");
        }
        //用户之前没点过赞，查看用户是否反对过该文章
        Boolean oppose = stringRedisTemplate.opsForSet().isMember(BLOG_OPPOSE_KEY + likeBlog.getUserId(), likeBlog.getBlogId());
        //如果用户没反对过
        if (!Boolean.TRUE.equals(oppose)) {
            //如果用户也没反对过该文章，查询数据库，排除redis和数据库数据不一致的情况
            LambdaQueryWrapper<TLikeBlog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TLikeBlog::getBlogId, likeBlog.getBlogId());
            queryWrapper.eq(TLikeBlog::getUserId, likeBlog.getUserId());
            TLikeBlog isLikeYet = this.getOne(queryWrapper);
            //用户之前并未对该文章进行点赞或反对
            if (isLikeYet == null) {
                likeBlog.setCreateTime(LocalDateTime.now());
                //点赞
                boolean save = this.save(likeBlog);
                //更新blog的点赞数量
                if (save) {
                    boolean update = blogService.update().setSql("like_count = like_count + 1").eq("id", likeBlog.getBlogId()).update();
                    if (update) {
                        //向redis中添加用户的点赞信息
                        Long add = stringRedisTemplate.opsForSet().add(BLOG_OPPOSE_KEY + likeBlog.getUserId(), String.valueOf(likeBlog.getBlogId()));
                        return add != null && add > 0 ? Result.success("点赞成功") : Result.error("网络异常");
                    }
                    return Result.error("网络异常");
                }
            }
            //两种数据不一致的情况：
                //该用户对该文章是点赞状态，但未写入redis
                //用户之前反对过该文章，但未添加进redis，更新数据回写便可
            return Result.error("网络异常");
        }
        //用户之前反对该文章
        //更新数据库信息，更新redis
        boolean update1 = this.update().setSql("isLike = 1").
                eq("blog_id", likeBlog.getBlogId()).eq("user_id", likeBlog.getUserId()).update();
        //更新成功，写入redis
        if(update1){
            //删除用户反对set的数据
            stringRedisTemplate.opsForSet().remove(BLOG_OPPOSE_KEY + likeBlog.getUserId(), likeBlog.getBlogId());
            //向用户点赞set添加数据
            stringRedisTemplate.opsForSet().add(BLOG_OPPOSE_KEY + likeBlog.getUserId(), String.valueOf(likeBlog.getBlogId()));
            return Result.success("点赞成功");
        }
        return Result.error("网络异常");
    }
}




