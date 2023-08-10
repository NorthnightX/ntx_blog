package com.ntx.blog.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.blog.domain.TLikeBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.mapper.TLikeBlogMapper;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TLikeBlogService;
import com.ntx.common.domain.Result;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


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
        String jsonString = JSON.toJSONString(likeBlog);
        //如果用户之前点过赞
        if (BooleanUtil.isTrue(member)) {
            //取消点赞
            kafkaTemplate.send("blogLike", "cancel", jsonString);
            return Result.success("已取消点赞");
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
                kafkaTemplate.send("blogLike", "like", jsonString);
                    return Result.success("点赞成功");
            }
            //两种数据不一致的情况：
            //该用户对该文章是点赞状态，但未写入redis
            //用户之前反对过该文章，但未添加进redis，更新数据回写便可
        }
        //用户之前反对该文章
        //更新数据库信息，更新redis
        kafkaTemplate.send("blogLike", "disLikeToLike", jsonString);
        return Result.success("点赞成功");
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




