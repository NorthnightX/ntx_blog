package com.ntx.blog.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.blog.domain.TLikeBlog;
import com.ntx.blog.mapper.TLikeBlogMapper;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TLikeBlogService;
import com.ntx.common.domain.Result;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Autowired
    private RestHighLevelClient client;

    /**
     * 点赞
     * @param likeBlog
     * @return
     */
    @Override
    @Transactional
    public Result likeBlog(TLikeBlog likeBlog) {
        //数据在redis用set实现
        //目前有时间bug
        //更新ES(传给ES值，如果是1，则该篇blog的喜欢量+1，如果是0则减一)
        //判断用户是否点赞，如果点过赞，则取消点赞（删除），没点过赞，则新增点赞，如果用户之前的点赞状态是2，即反对，则取消反对，重新点赞
        //从redis中获取点赞数据，获取不到查数据库,如果没有数据，则代表之前用户没有给这篇博客点过赞
        String blogId = String.valueOf(likeBlog.getBlogId());
        Boolean member = stringRedisTemplate.opsForSet().isMember(BLOG_LIKE_KEY + likeBlog.getUserId(), blogId);
        //如果用户之前点过赞
        if (BooleanUtil.isTrue(member)) {
            //取消点赞
            LambdaQueryWrapper<TLikeBlog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TLikeBlog::getBlogId, likeBlog.getBlogId());
            queryWrapper.eq(TLikeBlog::getUserId, likeBlog.getUserId());
            boolean remove = this.remove(queryWrapper);
            if (remove) {
                //更新blog的点赞数量
                boolean update = blogService.update().setSql("like_count = like_count - 1").eq("id", likeBlog.getBlogId()).update();
                if (update) {
                    Long remove1 = stringRedisTemplate.opsForSet().remove(BLOG_LIKE_KEY + likeBlog.getUserId(), blogId);
                    return remove1 != null && remove1 > 0 ? Result.success("已取消点赞") : Result.error("网络异常");
                }
            }
            return Result.error("网络异常");
        }
        //用户之前没点过赞，查看用户是否反对过该文章
        Boolean oppose = stringRedisTemplate.opsForSet().isMember(BLOG_OPPOSE_KEY + likeBlog.getUserId(), blogId);
        //如果用户没反对过
        if (BooleanUtil.isFalse(oppose)) {
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
                        Long add = stringRedisTemplate.opsForSet().add(BLOG_LIKE_KEY + likeBlog.getUserId(), String.valueOf(likeBlog.getBlogId()));

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
        boolean update1 = this.update().setSql("is_like = 1").
                eq("blog_id", likeBlog.getBlogId()).eq("user_id", likeBlog.getUserId()).update();
        //更新成功，写入redis
        if (update1) {
            //删除用户反对set的数据
            stringRedisTemplate.opsForSet().remove(BLOG_OPPOSE_KEY + likeBlog.getUserId(), blogId);
            //向用户点赞set添加数据
            stringRedisTemplate.opsForSet().add(BLOG_LIKE_KEY + likeBlog.getUserId(), String.valueOf(likeBlog.getBlogId()));
            return Result.success("点赞成功");
        }
        return Result.error("网络异常");
    }

    private Boolean ESUpdate(Integer blogId, Integer ops) {
        try {
            // 准备request
            UpdateRequest request = new UpdateRequest("blog", String.valueOf(blogId));

            // 使用 Script 方式更新 like_count 字段
            Script inlineScript = new Script(ScriptType.INLINE, "painless",
                    "ctx._source.like_count += params.value", Collections.singletonMap("value", ops));
            request.script(inlineScript);

            // 发送请求
            UpdateResponse response = client.update(request, RequestOptions.DEFAULT);

            // 根据更新结果判断操作是否成功
            if (response.getResult() == DocWriteResponse.Result.UPDATED) {
                // 更新成功
                return true;
            } else {
                // 更新失败
                return false;
            }
        } catch (ElasticsearchException | IOException ex) {
            // 更新操作发生异常
            ex.printStackTrace();
            return false;
        }
    }


    /**
     * 查找用户喜欢的文章
     *
     * @param userId
     * @return
     */
    @Override
    public Result queryLikeByUser(Integer userId) {
        String redisKey = BLOG_LIKE_KEY + userId;
        Boolean hasKey = stringRedisTemplate.hasKey(redisKey);
        //如果redis中有
        if (BooleanUtil.isTrue(hasKey)) {
            Set<String> members = stringRedisTemplate.opsForSet().members(redisKey);
            List<String> collect = members != null ? new ArrayList<>(members) : new ArrayList<>();
            return Result.success(collect);
        }
        //redis中没有
        //查找数据
        LambdaQueryWrapper<TLikeBlog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TLikeBlog::getId, userId).eq(TLikeBlog::getIsLike, 1);
        List<TLikeBlog> list = this.list(queryWrapper);
        //如果数据库有
        if (!list.isEmpty()) {
            //保存数据到redis，并返回数据
            List<String> blogIdList = new ArrayList<>(list.size());
            for (TLikeBlog tLikeBlog : list) {
                String blogId = String.valueOf(tLikeBlog.getBlogId());
                stringRedisTemplate.opsForSet().add(redisKey, blogId);
                blogIdList.add(blogId);
            }
            return Result.success(blogIdList);
        }
        return Result.success(new ArrayList<>());
    }

}




